package com.astraion.core.ai;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.ai.tools.ToolRegistry;
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
    private final Map<String, List<ObjectNode>> conversationHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY = 20;

    public AIOrchestrator(LLMService llmService, ToolRegistry toolRegistry,
                          JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.toolRegistry = toolRegistry;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
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

        // 添加 AI 回复到消息列表（含 tool_calls）
        // 收集工具结果
        for (JsonNode tc : toolCalls) {
            String toolName = tc.path("function").path("name").asText();
            String argsStr = tc.path("function").path("arguments").asText();

            @SuppressWarnings("unchecked")
            Map<String, Object> args = objectMapper.readValue(argsStr, Map.class);

            // 执行工具
            ToolResult result = toolRegistry.execute(toolName, args, ctx);

            // 组装 tool call 消息
            ObjectNode aiMsg = messages.addObject();
            aiMsg.put("role", "assistant");
            aiMsg.putNull("content");
            aiMsg.set("tool_calls", objectMapper.createArrayNode().add(tc));

            ObjectNode toolMsg = messages.addObject();
            toolMsg.put("role", "tool");
            toolMsg.put("tool_call_id", tc.path("id").asText());
            toolMsg.put("content", objectMapper.writeValueAsString(result));
        }

        // 再次调用 LLM 生成最终回复
        String modelName = getConfig("ai.model");
        String responseBody = llmService.sendRequest(
            baseUrl.endsWith("/chat/completions") ? baseUrl : baseUrl + "/v1/chat/completions",
            apiKey, messages, temperature, maxTokens, modelName, null);

        JsonNode root = objectMapper.readTree(responseBody);
        String finalReply = root.path("choices").get(0).path("message").path("content").asText("");

        if (finalReply.isEmpty()) {
            String summary = buildToolResultSummary(toolCalls);
            sender.accept(makeJson("complete", summary));
            saveAssistantToHistory(sessionId, summary);
        } else {
            sender.accept(makeJson("complete", finalReply));
            saveAssistantToHistory(sessionId, finalReply);
        }
    }

    private String buildToolResultSummary(JsonNode toolCalls) {
        StringBuilder sb = new StringBuilder("已完成以下操作：\n");
        for (JsonNode tc : toolCalls) {
            String name = tc.path("function").path("name").asText();
            sb.append("- ").append(name).append("\n");
        }
        return sb.toString();
    }

    private String buildSystemPrompt(String username, String role) {
        String sysPrompt = getConfigFromDb("astraion.system_prompt_key");
        if (sysPrompt.isEmpty()) {
            sysPrompt = "你是一个 AI 业务助手，运行在 ASTRAION 平台上。\n"
                + "你的工作是帮助用户通过对话管理所有业务数据。\n\n"
                + "核心原则：\n"
                + "1. 用户的所有操作都通过你完成\n"
                + "2. 重要操作必须让用户确认后才能执行\n"
                + "3. 根据数据量选择合适的展示方式\n"
                + "4. 你只能使用提供的工具进行操作\n"
                + "5. 不要在数据量多时输出大量原始 JSON\n";
        }
        return sysPrompt + "\n\n当前用户: " + username + " (角色: " + role + ")";
    }

    private String getConfig(String key) {
        var rows = jdbcTemplate.queryForList(
            "SELECT config_value FROM astraion_system_config WHERE config_key=?", key);
        return rows.isEmpty() ? "" : (String) rows.get(0).get("config_value");
    }

    private String getConfigFromDb(String key) {
        var rows = jdbcTemplate.queryForList(
            "SELECT config_value FROM astraion_system_config WHERE config_key=?", "system.prompt");
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
