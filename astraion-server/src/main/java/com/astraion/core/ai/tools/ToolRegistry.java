package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tool 注册中心 — 管理所有 AI 可调用的工具
 */
@Component
public class ToolRegistry {

    private final Map<String, AstraionTool> tools = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ToolRegistry(ObjectMapper objectMapper, List<AstraionTool> toolList) {
        this.objectMapper = objectMapper;
        for (AstraionTool tool : toolList) {
            tools.put(tool.getName(), tool);
        }
    }

    /** 注册一个工具 */
    public void register(AstraionTool tool) {
        tools.put(tool.getName(), tool);
    }

    /** 获取指定工具 */
    public AstraionTool getTool(String name) {
        return tools.get(name);
    }

    /** 获取所有工具名 */
    public Set<String> getToolNames() {
        return tools.keySet();
    }

    /** 执行工具 */
    public ToolResult execute(String toolName, Map<String, Object> params, ToolContext ctx) {
        AstraionTool tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.fail("未知工具: " + toolName);
        }
        return tool.execute(params, ctx);
    }

    /** 生成 OpenAI 格式的 tool 定义列表 */
    public ArrayNode getToolDefinitions() {
        ArrayNode arr = objectMapper.createArrayNode();
        for (AstraionTool tool : tools.values()) {
            ObjectNode def = objectMapper.createObjectNode();
            def.put("type", "function");
            ObjectNode func = def.putObject("function");
            func.put("name", tool.getName());
            func.put("description", tool.getDescription());
            func.set("parameters", buildParametersJson(tool.getParameterSchema()));
            arr.add(def);
        }
        return arr;
    }

    private ObjectNode buildParametersJson(Map<String, Object> schema) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "object");
        ObjectNode props = root.putObject("properties");
        ArrayNode required = root.putArray("required");

        if (schema != null) {
            for (Map.Entry<String, Object> entry : schema.entrySet()) {
                String key = entry.getKey();
                Object val = entry.getValue();
                ObjectNode prop = props.putObject(key);
                if (val instanceof String) {
                    prop.put("type", (String) val);
                } else {
                    prop.put("type", "string");
                }
                required.add(key);
            }
        }
        return root;
    }
}
