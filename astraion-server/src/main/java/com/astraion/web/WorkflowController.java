package com.astraion.web;

import com.astraion.core.engine.WorkflowEngine;
import com.astraion.core.engine.WorkflowException;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工作流控制器 — 流程审批 API
 */
@RestController
@RequestMapping("/api/v1/workflow")
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    public WorkflowController(WorkflowEngine workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    /**
     * 启动工作流
     */
    @PostMapping("/instances")
    public ApiResponse<Map<String, Object>> startWorkflow(@RequestBody StartRequest req,
                                                           HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        Map<String, Object> result = workflowEngine.startWorkflow(
            req.getWorkflowName(), req.getModelName(), req.getRecordId(),
            req.getData() != null ? req.getData() : Map.of(), ctx
        );
        return ApiResponse.ok("工作流已启动", result);
    }

    /**
     * 审批通过
     */
    @PostMapping("/tasks/{taskId}/approve")
    public ApiResponse<Map<String, Object>> approveTask(@PathVariable Long taskId,
                                                         @RequestBody(required = false) TaskActionRequest req,
                                                         HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        String comment = req != null ? req.getComment() : "";
        Map<String, Object> result = workflowEngine.approveTask(taskId, comment, ctx);
        return ApiResponse.ok("审批通过", result);
    }

    /**
     * 驳回
     */
    @PostMapping("/tasks/{taskId}/reject")
    public ApiResponse<Map<String, Object>> rejectTask(@PathVariable Long taskId,
                                                        @RequestBody(required = false) TaskActionRequest req,
                                                        HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        String comment = req != null ? req.getComment() : "";
        Map<String, Object> result = workflowEngine.rejectTask(taskId, comment, ctx);
        return ApiResponse.ok("已驳回", result);
    }

    /**
     * 获取单个任务详情
     */
    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Map<String, Object>> getTask(@PathVariable Long taskId) {
        Map<String, Object> task = workflowEngine.getTask(taskId);
        if (task == null) {
            return ApiResponse.fail(404, "任务不存在");
        }
        return ApiResponse.ok(task);
    }

    /**
     * 查询我的待办
     */
    @GetMapping("/tasks/pending")
    public ApiResponse<Map<String, Object>> listPendingTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        return ApiResponse.ok(workflowEngine.listPendingTasks(ctx.getUserId(), page, size));
    }

    /**
     * 查询我的已办
     */
    @GetMapping("/tasks/completed")
    public ApiResponse<Map<String, Object>> listCompletedTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        return ApiResponse.ok(workflowEngine.listCompletedTasks(ctx.getUserId(), page, size));
    }

    /**
     * 获取流程实例详情
     */
    @GetMapping("/instances/{instanceId}")
    public ApiResponse<Map<String, Object>> getInstance(@PathVariable Long instanceId) {
        Map<String, Object> instance = workflowEngine.getInstance(instanceId);
        if (instance == null) {
            return ApiResponse.fail(404, "流程实例不存在");
        }
        return ApiResponse.ok(instance);
    }

    /**
     * 查询指定业务记录的流程实例
     */
    @GetMapping("/instances")
    public ApiResponse<List<Map<String, Object>>> listInstances(
            @RequestParam String modelName,
            @RequestParam Long recordId) {
        return ApiResponse.ok(workflowEngine.listInstances(modelName, recordId));
    }

    /**
     * 查询某个流程实例下的所有任务（含历史）
     */
    @GetMapping("/instances/{instanceId}/tasks")
    public ApiResponse<List<Map<String, Object>>> listTasksByInstance(@PathVariable Long instanceId) {
        return ApiResponse.ok(workflowEngine.listTasksByInstance(instanceId));
    }

    // ---- DTOs ----

    public static class StartRequest {
        private String workflowName;
        private String modelName;
        private Long recordId;
        private Map<String, Object> data;

        public String getWorkflowName() { return workflowName; }
        public void setWorkflowName(String workflowName) { this.workflowName = workflowName; }
        public String getModelName() { return modelName; }
        public void setModelName(String modelName) { this.modelName = modelName; }
        public Long getRecordId() { return recordId; }
        public void setRecordId(Long recordId) { this.recordId = recordId; }
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }

    public static class TaskActionRequest {
        private String comment;
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    // ---- helpers ----

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
