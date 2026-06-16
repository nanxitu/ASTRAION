package com.astraion.core.ai.tools;

import com.astraion.core.engine.WorkflowEngine;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class QueryTasksTool implements AstraionTool {

    private final WorkflowEngine workflowEngine;

    public QueryTasksTool(WorkflowEngine workflowEngine) { this.workflowEngine = workflowEngine; }

    @Override
    public String getName() { return "queryTasks"; }

    @Override
    public String getDescription() { return "查询当前用户的待办/已办任务，或查询指定业务记录的审批进度。"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("status", "string", "modelName", "string", "recordId", "integer"); }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        try {
            String status = (String) params.getOrDefault("status", "pending");
            String modelName = (String) params.get("modelName");
            Object recordIdObj = params.get("recordId");
            Long userId = Long.parseLong(ctx.userId);
            Map<String, Object> result;
            if (modelName != null && recordIdObj != null) {
                Long recordId = toLong(recordIdObj);
                List<Map<String, Object>> instances = workflowEngine.listInstances(modelName, recordId);
                result = Map.of("instances", (Object) instances, "message", "查询到 " + instances.size() + " 条流程实例");
            } else if ("completed".equals(status)) {
                result = workflowEngine.listCompletedTasks(userId, 1, 50);
            } else {
                result = workflowEngine.listPendingTasks(userId, 1, 50);
            }
            return ToolResult.ok("查询成功", result);
        } catch (Exception e) {
            return ToolResult.fail("查询任务失败: " + e.getMessage());
        }
    }

    private Long toLong(Object o) { return o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()); }
}
