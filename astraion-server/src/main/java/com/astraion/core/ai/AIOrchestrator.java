package com.astraion.core.ai;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.ai.tools.ToolRegistry;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.DisplayDef;
import com.astraion.model.ModelMeta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * AI 编排器 — 接收用户消息，
 * 1. 调用 LLM 理解意图
 * 2. 如果需要工具调用则执行工具
 * 3. 将结果返回给前端
 */
@Component
public class AIOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AIOrchestrator.class);

    private final LLMService llmService;
    private final ToolRegistry toolRegistry;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MetadataEngine metadataEngine;
    private final Map<String, List<ObjectNode>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    public AIOrchestrator(LLMService llmService, ToolRegistry toolRegistry,
                          JdbcTemplate jdbcTemplate, ObjectMapper objectMapper,
                          MetadataEngine metadataEngine) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.metadataEngine = metadataEngine;
    }

    /**
     * 处理用户消息（异步）
     */
    public void handleMessage(String sessionId, String userId, String username, String role,
                               String userMessage, Consumer<String> sender) {

        String baseUrl = getConfig("ai.base_url");
        String apiKey = getConfig("ai.api_key");

        if (baseUrl.isEmpty() || apiKey.isEmpty()) {
            sender.accept(makeJson("error", "AI 模型未配置，请在设置中配置"));
            return;
        }

        try {
            // 1. 发送 "思考中..." 提示
            sender.accept(makeJson("delta", "正在思考..."));

            // 2. 构建系统提示（优先从 DB 加载）
            String systemPrompt = buildSystemPrompt(username, role);

            // 3. 构建对话上下文（含历史）
            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode sysMsg = messages.addObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);

            // 从历史恢复对话上下文
            List<ObjectNode> history = conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>());
            synchronized (history) {
                int start = Math.max(0, history.size() - 16);
                for (int i = start; i < history.size(); i++) {
                    messages.add(history.get(i));
                }
            }

            // 添加当前用户消息
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            // 工具定义
            ArrayNode tools = toolRegistry.getToolDefinitions();

            // 发送给 LLM
            String temperatureStr = getConfig("ai.temperature");
            String maxTokensStr = getConfig("ai.max_tokens");
            double temperature = parseDouble(temperatureStr, 0.7);
            int maxTokens = parseInt(maxTokensStr, 4096);
            String model = getConfig("ai.model");

            String responseBody = llmService.sendRequest(baseUrl, apiKey, messages, temperature, maxTokens, model, tools);

            // 4. 解析 LLM 回复
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choice = root.path("choices").get(0);
            JsonNode message = choice.path("message");

            JsonNode toolCalls = message.path("tool_calls");
            String reply = message.path("content").asText("");

            if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                // 4a. 有工具调用 → 执行工具 → 再次调用 LLM
                executeToolCallsAndRespond(toolCalls, userId, username, role,
                    baseUrl, apiKey, model, temperature, maxTokens, messages, sender, sessionId);
            } else {
                // 4b. 直接回复
                sender.accept(makeJson("complete", reply));
                // 保存到历史
                saveAssistantToHistory(sessionId, reply);
            }

        } catch (Exception e) {
            log.error("[AI] Error processing message", e);
            sender.accept(makeJson("error", "处理出错: " + e.getMessage()));
        }
    }

    private void executeToolCallsAndRespond(JsonNode toolCalls, String userId, String username, String role,
                                             String baseUrl, String apiKey, String model,
                                             double temperature, int maxTokens,
                                             ArrayNode messages, Consumer<String> sender, String sessionId) throws Exception {

        ToolContext ctx = new ToolContext(userId, username, role);

        log.info("[AI] Executing {} tool call(s), no second LLM round", toolCalls.size());

        // 一条 assistant 消息，包含所有 tool_calls
        ObjectNode assistantMsg = messages.addObject();
        assistantMsg.put("role", "assistant");
        assistantMsg.putNull("content");
        assistantMsg.set("tool_calls", toolCalls);

        List<String> parts = new ArrayList<>();
        Object renderData = null;
        String renderType = null;

        for (JsonNode tc : toolCalls) {
            String toolName = tc.path("function").path("name").asText();
            String argsStr = tc.path("function").path("arguments").asText();
            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsStr, Map.class);

            ToolResult result = toolRegistry.execute(toolName, args, ctx);

            // 记录到历史
            ObjectNode toolMsg = messages.addObject();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", tc.path("id").asText());
            toolMsg.put("content", objectMapper.writeValueAsString(result));

            // 检测显示配置 & 构建 renderData
            if (result.success && result.data instanceof List && !((List<?>) result.data).isEmpty()) {
                renderData = buildRenderData(toolName, args, result.data);
                if (renderData != null) renderType = "cards";
            }

            // 格式化结果
            parts.add(formatToolResult(toolName, result));
        }

        // 发文本摘要
        String summary = String.join("\n\n", parts);
        sender.accept(makeJson("complete", summary));
        saveAssistantToHistory(sessionId, summary);

        // 有卡片配置 → 额外发 render 消息
        if (renderData != null) {
            sender.accept(makeRenderJson(renderType, renderData));
        }
    }

    /**
     * 后端直接格式化工具结果，无需二次调 LLM。
     */
    @SuppressWarnings("unchecked")
    private String formatToolResult(String toolName, ToolResult result) {
        if (!result.success) {
            return "❌ " + result.message;
        }

        if (!(result.data instanceof List)) {
            return "✅ " + result.message;
        }

        List<Object> list = (List<Object>) result.data;
        if (list.isEmpty()) {
            return "📋 " + result.message + "（无数据）";
        }

        Object first = list.get(0);
        // 模型列表/结构 → 表格
        if (first instanceof Map && ((Map<String, Object>) first).containsKey("fields")) {
            return formatModelList(list);
        }
        // 普通数据查询 → 键值对列表
        return formatDataList(list);
    }

    /** 格式化模型结构（listModels / getModelSchema 的结果） */
    @SuppressWarnings("unchecked")
    private String formatModelList(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : list) {
            Map<String, Object> model = (Map<String, Object>) obj;
            String modelName = String.valueOf(model.getOrDefault("modelName", "?"));
            String displayName = String.valueOf(model.getOrDefault("displayName", modelName));
            String desc = model.containsKey("description") ? String.valueOf(model.get("description")) : "";
            sb.append("<h4>📦 ").append(modelName).append("（").append(displayName).append("）</h4>");
            if (!desc.isEmpty() && !"null".equals(desc)) {
                sb.append("<p style='color:var(--text-muted)'>").append(escapeHtml(desc)).append("</p>");
            }

            Object fieldsObj = model.get("fields");
            if (fieldsObj instanceof List) {
                List<Object> fields = (List<Object>) fieldsObj;
                if (fields.isEmpty()) continue;
                sb.append("<table><tr><th>必填</th><th>字段名</th><th>显示名</th><th>类型</th></tr>");
                for (Object f : fields) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fd = (f instanceof Map)
                        ? (Map<String, Object>) f
                        : objectMapper.convertValue(f, Map.class);
                    String fName = escapeHtml(String.valueOf(fd.getOrDefault("name", "?")));
                    String fType = escapeHtml(String.valueOf(fd.getOrDefault("type", "?")));
                    String fLabel = escapeHtml(String.valueOf(fd.getOrDefault("label", "")));
                    Object req = fd.getOrDefault("required", false);
                    String mark = Boolean.TRUE.equals(req) ? "🔴" : "";
                    sb.append("<tr><td>").append(mark).append("</td>")
                        .append("<td><code>").append(fName).append("</code></td>")
                        .append("<td>").append(fLabel).append("</td>")
                        .append("<td>").append(fType).append("</td></tr>");
                }
                sb.append("</table>");
            }
        }
        return sb.toString();
    }

    /** 格式化数据查询结果 */
    @SuppressWarnings("unchecked")
    private String formatDataList(List<Object> list) {
        if (list.isEmpty()) return "<em>📋 无数据</em>";

        Object first = list.get(0);
        if (!(first instanceof Map)) {
            StringBuilder sb = new StringBuilder("<ul>");
            for (Object obj : list) {
                sb.append("<li>").append(escapeHtml(String.valueOf(obj))).append("</li>");
                if (list.indexOf(obj) >= 50) break;
            }
            sb.append("</ul>");
            return sb.toString();
        }

        Map<String, Object> sample = (Map<String, Object>) list.get(0);
        List<String> cols = new ArrayList<>();
        for (String k : sample.keySet()) {
            if (k.startsWith("_") || "id".equals(k)) continue;
            if (cols.size() < 6) cols.add(k);
        }
        if (cols.isEmpty()) {
            return "<em>无可见字段</em>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p>📊 共 ").append(list.size()).append(" 条</p>\n");
        sb.append("<table><tr>");
        for (String col : cols) {
            sb.append("<th>").append(escapeHtml(col)).append("</th>");
        }
        sb.append("</tr>");

        int shown = 0;
        for (Object obj : list) {
            if (++shown > 50) {
                sb.append("</table><p><em>... 仅显示前 50 条</em></p>");
                return sb.toString();
            }
            Map<String, Object> row = (Map<String, Object>) obj;
            sb.append("<tr>");
            for (String col : cols) {
                Object val = row.get(col);
                String s = val == null ? "" : String.valueOf(val);
                sb.append("<td>").append(escapeHtml(s.length() > 40 ? s.substring(0, 40) + "..." : s)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String buildSystemPrompt(String username, String role) {
        // 工具规则说明（始终追加，无论 DB 是否配了自定义提示）
        String toolRules = "\n\n工具选择规则：\n"
            + "1. 当用户问『有哪些XX』『列出XX』『查询XX』『看看XX』时，调用 queryData(modelName=XX)\n"
            + "2. 当用户问『有什么模型』『有哪些业务』『现存什么』时，调用 listModels\n"
            + "3. 当用户问某个模型的结构字段时，调用 getModelSchema(modelName=XX)\n"
            + "例如『有哪些员工』调用 queryData(modelName=employee)\n"
            + "例如『有什么模型』调用 listModels\n"
            + "例如『部门有哪些字段』调用 getModelSchema(modelName=department)\n";

        String sysPrompt = getConfigFromDb("astraion.system_prompt_key");
        if (sysPrompt.isEmpty()) {
            sysPrompt = "你是一个 AI 业务助手，运行在 ASTRAION 平台上。\n"
                + "你的工作是帮助用户通过对话管理所有业务数据。\n\n"
                + "核心原则：\n"
                + "1. 用户的所有操作都通过你完成\n"
                + "2. 重要操作必须让用户确认后才能执行\n"
                + "3. 不要在数据量多时输出大量原始 JSON\n";
        }
        return sysPrompt + toolRules + "\n当前用户: " + username + " (角色: " + role + ")";
    }

    private String getConfig(String key) {
        var rows = jdbcTemplate.queryForList(
            "SELECT config_value FROM astraion_system_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : (String) rows.get(0).get("config_value");
    }

    private String getConfigFromDb(String key) {
        var rows = jdbcTemplate.queryForList(
            "SELECT config_value FROM astraion_system_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : (String) rows.get(0).get("config_value");
    }

    private void saveAssistantToHistory(String sessionId, String reply) {
        List<ObjectNode> history = conversationHistory.get(sessionId);
        if (history != null && reply != null && !reply.isEmpty()) {
            synchronized (history) {
                ObjectNode assistantMsg = objectMapper.createObjectNode();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", reply);
                history.add(assistantMsg);
                // 限制历史长度
                while (history.size() > MAX_HISTORY) {
                    history.remove(0);
                }
            }
        }
    }

    /**
     * 构建卡片渲染数据。如果模型有 display 配置，返回 card 格式。
     */
    @SuppressWarnings("unchecked")
    private Object buildRenderData(String toolName, Map<String, Object> args, Object data) {
        if (!"queryData".equals(toolName) || !(data instanceof List)) return null;

        String modelName = (String) args.get("modelName");
        if (modelName == null) return null;

        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null || model.getDisplay() == null) return null;

        var display = model.getDisplay();
        var cardsConfig = display.getCards();
        if (cardsConfig == null) return null;

        String titleField = cardsConfig.getTitleField();
        List<String> cardFields = cardsConfig.getFields();
        List<DisplayDef.CardAction> cardActions = cardsConfig.getActions();

        List<Object> rows = (List<Object>) data;
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object rowObj : rows) {
            if (!(rowObj instanceof Map)) continue;
            Map<String, Object> row = (Map<String, Object>) rowObj;
            Object id = row.getOrDefault("id", 0);
            Object title = titleField != null ? row.getOrDefault(titleField, "") : "";

            Map<String, Object> fields = new LinkedHashMap<>();
            if (cardFields != null) {
                for (String f : cardFields) {
                    Object v = row.get(f);
                    if (v != null) fields.put(f, v);
                }
            } else {
                for (String k : row.keySet()) {
                    if (!k.startsWith("_") && !"id".equals(k)) {
                        fields.put(k, row.get(k));
                    }
                }
            }

            List<Map<String, Object>> actions = new ArrayList<>();
            if (cardActions != null) {
                for (var ca : cardActions) {
                    Map<String, Object> act = new LinkedHashMap<>();
                    act.put("label", ca.getLabel());
                    act.put("tool", ca.getTool());
                    if (ca.getVariant() != null) act.put("variant", ca.getVariant());
                    actions.add(act);
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", id);
            item.put("title", title);
            item.put("fields", fields);
            item.put("actions", actions);
            items.add(item);
        }

        Map<String, Object> renderData = new LinkedHashMap<>();
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("titleField", titleField);
        config.put("cardFields", cardFields);
        renderData.put("config", config);
        renderData.put("items", items);
        return renderData;
    }

    /** 发送 renderData 消息给前端 */
    private String makeRenderJson(String renderType, Object renderData) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", "complete");
            node.put("renderType", renderType);
            node.set("renderData", objectMapper.valueToTree(renderData));
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("[AI] render JSON error: {}", e.getMessage());
            return "{\"type\":\"error\",\"content\":\"Render error\"}";
        }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s); } catch (Exception e) { return def; }
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private String makeJson(String type, String content) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", type);
            node.put("content", content);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"content\":\"JSON error\"}";
        }
    }
}
