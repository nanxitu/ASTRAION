package com.astraion.core.ai.tools;

import java.util.Map;

/**
 * AI Tool 接口 — 所有供 LLM 调用的工具
 */
public interface AstraionTool {

    /** 工具名称，如 queryData */
    String getName();

    /** 工具描述（LLM 理解调用场景） */
    String getDescription();

    /** 参数 JSON Schema */
    Map<String, Object> getParameterSchema();

    /** 执行工具 */
    ToolResult execute(Map<String, Object> params, ToolContext ctx);

    class ToolResult {
        public final boolean success;
        public final String message;
        public final Object data;

        private ToolResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static ToolResult ok(String message, Object data) {
            return new ToolResult(true, message, data);
        }

        public static ToolResult fail(String message) {
            return new ToolResult(false, message, null);
        }
    }

    class ToolContext {
        public final String userId;
        public final String username;
        public final String role;

        public ToolContext(String userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }
    }
}
