package com.astraion.core.ai.tools;

import com.astraion.core.engine.WorkflowEngine;
import com.astraion.model.UserContext;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ApproveTaskTool implements AstraionTool {

    private final WorkflowEngine workflowEngine;

    public ApproveTaskTool(WorkflowEngine workflowEngine) { this.workflowEngine = workflowEngine; }

    @Override
    public String getName() { return "approveTask"; }

    @Override
    public String getDescription() { return "审批通过一个待办任务。需要 taskId。可选 comment 备注。"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("taskId", "integer", "comment", "string"); }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        try {
            Object taskIdObj = params.get("taskId");
            if (taskIdObj == null) return ToolResult.fail("缺少 taskId");
            Long taskId = toLong(taskIdObj);
            String comment = (String) params.getOrDefault("comment", "");
            UserContext userCtx = buildCtx(ctx);
            Map<String, Object> result = workflowEngine.approveTask(taskId, comment, userCtx);
            return ToolResult.ok("审批通过", result);
        } catch (Exception e) {
            return ToolResult.fail("审批失败: " + e.getMessage());
        }
    }

    private Long toLong(Object o) { return o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()); }

    private UserContext buildCtx(ToolContext ctx) {
        UserContext u = new UserContext();
        u.setUserId(Long.parseLong(ctx.userId)); u.setUsername(ctx.username); u.setRole(ctx.role);
        return u;
    }
}
