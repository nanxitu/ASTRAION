package com.astraion.core.engine;

import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.ModelMeta;
import com.astraion.model.UserContext;
import com.astraion.model.WorkflowDef;
import com.astraion.model.WorkflowDef.StepDef;
import com.astraion.model.WorkflowDef.ActionDef;
import com.astraion.model.WorkflowDef.ConditionDef;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流引擎 — 驱动审批流程
 *
 * 提供流程启动、任务审批/驳回、任务查询等功能。
 * 支持条件分支（Aviator 表达式）、自动超时处理。
 */
@Component
public class WorkflowEngine {

    private final JdbcTemplate jdbcTemplate;
    private final MetadataEngine metadataEngine;
    private final DynamicCRUD dynamicCRUD;
    private final ObjectMapper objectMapper;

    public WorkflowEngine(JdbcTemplate jdbcTemplate, MetadataEngine metadataEngine,
                          DynamicCRUD dynamicCRUD, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataEngine = metadataEngine;
        this.dynamicCRUD = dynamicCRUD;
        this.objectMapper = objectMapper;
    }

    // ==================== 工作流启动 ====================

    /**
     * 启动一个工作流实例
     *
     * @param workflowName 流程名称（定义于 ModelMeta.workflows 中）
     * @param modelName    业务模型名
     * @param recordId     业务记录 ID
     * @param data         当前数据（用于条件分支判断）
     * @param ctx          当前用户上下文
     * @return 第一个待办任务（若无则返回 null）
     */
    public Map<String, Object> startWorkflow(String workflowName, String modelName,
                                              Long recordId, Map<String, Object> data,
                                              UserContext ctx) {
        WorkflowDef wf = getWorkflowDef(modelName, workflowName);
        if (wf == null) {
            throw new WorkflowException("工作流未定义: " + workflowName + " (模型: " + modelName + ")");
        }

        StepDef firstStep = resolveFirstStep(wf, data);
        if (firstStep == null) {
            throw new WorkflowException("工作流没有可用的起始步骤");
        }

        // 创建流程实例
        jdbcTemplate.update("""
            INSERT INTO astraion_workflow_instance
                (workflow_id, model_name, record_id, current_step, status, initiator_id, started_at)
            VALUES (?,?,?,?,?,?,?)
            """,
            getWorkflowId(modelName, workflowName),
            modelName, recordId, firstStep.getId(), "running",
            ctx.getUserId(), LocalDateTime.now()
        );

        Long instanceId = jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);

        // 创建首个待办任务
        Map<String, Object> task = createTask(instanceId, modelName, recordId, firstStep, ctx);

        return task;
    }

    // ==================== 任务审批/驳回 ====================

    /**
     * 审批通过当前任务
     *
     * @param taskId   任务 ID
     * @param comment  审批意见
     * @param ctx      当前用户上下文
     * @return 下一步任务（可能存在多任务），或表示流程结束的 Map
     */
    public Map<String, Object> approveTask(Long taskId, String comment, UserContext ctx) {
        return processTask(taskId, comment, "approved", ctx);
    }

    /**
     * 驳回当前任务
     *
     * @param taskId   任务 ID
     * @param comment  驳回意见
     * @param ctx      当前用户上下文
     * @return 下个步骤的信息
     */
    public Map<String, Object> rejectTask(Long taskId, String comment, UserContext ctx) {
        return processTask(taskId, comment, "rejected", ctx);
    }

    /**
     * 处理任务（审批/驳回通用逻辑）
     */
    private Map<String, Object> processTask(Long taskId, String comment, String action, UserContext ctx) {
        // 获取任务
        Map<String, Object> task = getTask(taskId);
        if (task == null) {
            throw new WorkflowException("任务不存在: " + taskId);
        }

        String currentStatus = (String) task.get("status");
        if (!"pending".equals(currentStatus)) {
            throw new WorkflowException("任务不在待办状态，无法操作");
        }

        // 权限检查：只能操作属于自己的任务
        Long assigneeId = (Long) task.get("assignee_id");
        if (!ctx.getUserId().equals(assigneeId) && !ctx.isAdmin()) {
            throw new WorkflowException("无权操作此任务");
        }

        Long instId = (Long) task.get("workflow_inst_id");
        String modelName = (String) task.get("model_name");
        Long recordId = (Long) task.get("record_id");
        String stepId = (String) task.get("step_id");

        // 获取实例信息
        Map<String, Object> instance = getInstance(instId);
        String wfName = getWorkflowNameFromInstance(instance, modelName);

        WorkflowDef wf = getWorkflowDef(modelName, wfName);
        if (wf == null) {
            throw new WorkflowException("工作流已不存在");
        }

        StepDef currentStep = wf.getStep(stepId);
        if (currentStep == null) {
            throw new WorkflowException("流程步骤不存在: " + stepId);
        }

        // 标记当前任务已完成
        jdbcTemplate.update("""
            UPDATE astraion_task
            SET status=?, comment=?, completed_at=NOW()
            WHERE id=?
            """, "completed".equals(action) ? "approved" : "rejected", comment, taskId);

        // 查找下一步
        ActionDef actionDef = findAction(currentStep, action);
        if (actionDef == null) {
            throw new WorkflowException("步骤 " + stepId + " 不支持操作: " + action);
        }

        String nextStepId = actionDef.getNext();

        // 获取业务数据（用于条件分支）
        Map<String, Object> recordData = getRecordData(modelName, recordId);

        // 处理条件分支
        nextStepId = resolveConditionalNext(wf, nextStepId, recordData);

        if ("end".equals(nextStepId)) {
            // 流程结束
            jdbcTemplate.update("""
                UPDATE astraion_workflow_instance
                SET status='completed', current_step=?, finished_at=NOW()
                WHERE id=?
                """, nextStepId, instId);

            return Map.of(
                "action", "end",
                "message", "流程已结束",
                "workflowInstanceId", instId
            );
        }

        if ("start".equals(nextStepId) && "rejected".equals(action)) {
            // 驳回回到起点 — 重新开始
            StepDef firstStep = resolveFirstStep(wf, recordData);
            if (firstStep != null) {
                jdbcTemplate.update("""
                    UPDATE astraion_workflow_instance
                    SET current_step=?, status='running'
                    WHERE id=?
                    """, firstStep.getId(), instId);

                Map<String, Object> newTask = createTask(instId, modelName, recordId, firstStep, ctx);
                return newTask;
            }
        }

        // 正常进入下一步
        StepDef nextStep = wf.getStep(nextStepId);
        if (nextStep == null) {
            throw new WorkflowException("下一步骤不存在: " + nextStepId);
        }

        jdbcTemplate.update("""
            UPDATE astraion_workflow_instance
            SET current_step=?
            WHERE id=?
            """, nextStepId, instId);

        // 若下一步是 auto 类型，自动通过
        if ("auto".equals(nextStep.getType())) {
            Map<String, Object> autoTask = createTask(instId, modelName, recordId, nextStep, ctx);
            Long autoTaskId = (Long) autoTask.get("id");
            return processTask(autoTaskId, "系统自动通过", "approve", ctx);
        }

        Map<String, Object> newTask = createTask(instId, modelName, recordId, nextStep, ctx);
        return newTask;
    }

    // ==================== 查询方法 ====================

    /**
     * 获取单个任务
     */
    public Map<String, Object> getTask(Long taskId) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT * FROM astraion_task WHERE id=?", taskId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询待办任务列表（支持分页）
     */
    public Map<String, Object> listPendingTasks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_task WHERE assignee_id=? AND status='pending'",
            Long.class, userId
        );

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT t.*, wi.workflow_id, wi.started_at AS instance_started_at " +
            "FROM astraion_task t " +
            "LEFT JOIN astraion_workflow_instance wi ON t.workflow_inst_id = wi.id " +
            "WHERE t.assignee_id=? AND t.status='pending' " +
            "ORDER BY t.created_at DESC LIMIT ? OFFSET ?",
            userId, size, offset
        );

        return Map.of(
            "total", total != null ? total : 0L,
            "items", items,
            "page", page,
            "size", size
        );
    }

    /**
     * 查询已办任务列表
     */
    public Map<String, Object> listCompletedTasks(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_task WHERE assignee_id=? AND status IN ('approved','rejected')",
            Long.class, userId
        );

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT * FROM astraion_task WHERE assignee_id=? AND status IN ('approved','rejected') " +
            "ORDER BY completed_at DESC LIMIT ? OFFSET ?",
            userId, size, offset
        );

        return Map.of(
            "total", total != null ? total : 0L,
            "items", items,
            "page", page,
            "size", size
        );
    }

    /**
     * 查询流程实例
     */
    public Map<String, Object> getInstance(Long instanceId) {
        List<Map<String, Object>> results = jdbcTemplate.queryForList(
            "SELECT * FROM astraion_workflow_instance WHERE id=?", instanceId
        );
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 查询指定记录的流程实例（含历史）
     */
    public List<Map<String, Object>> listInstances(String modelName, Long recordId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM astraion_workflow_instance WHERE model_name=? AND record_id=? ORDER BY started_at DESC",
            modelName, recordId
        );
    }

    /**
     * 查询某个实例下的所有任务（含历史）
     */
    public List<Map<String, Object>> listTasksByInstance(Long instanceId) {
        return jdbcTemplate.queryForList(
            "SELECT * FROM astraion_task WHERE workflow_inst_id=? ORDER BY created_at ASC",
            instanceId
        );
    }

    /**
     * 获取流程定义（从模型元数据中提取）
     */
    private WorkflowDef getWorkflowDef(String modelName, String workflowName) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null || model.getWorkflows() == null) return null;
        return model.getWorkflows().stream()
            .filter(w -> w.getName().equals(workflowName))
            .findFirst()
            .orElse(null);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取或创建流程定义在数据库中的记录
     */
    private Long getWorkflowId(String modelName, String workflowName) {
        WorkflowDef wf = getWorkflowDef(modelName, workflowName);
        if (wf == null) return null;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT id FROM astraion_workflow WHERE name=? AND model_name=?",
            workflowName, modelName
        );

        if (!rows.isEmpty()) {
            return (Long) rows.get(0).get("id");
        }

        // 持久化到流程表
        try {
            String config = objectMapper.writeValueAsString(wf);
            jdbcTemplate.update(
                "INSERT INTO astraion_workflow (name, model_name, trigger, config, status) VALUES (?,?,?,?,?)",
                workflowName, modelName, wf.getTrigger(), config, "active"
            );
            return jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);
        } catch (JsonProcessingException e) {
            throw new WorkflowException("序列化流程定义失败", e);
        }
    }

    /**
     * 判断条件分支，决定下一步
     */
    private String resolveConditionalNext(WorkflowDef wf, String defaultNext, Map<String, Object> data) {
        if (wf.getConditions() == null || wf.getConditions().isEmpty()) return defaultNext;

        for (ConditionDef cd : wf.getConditions()) {
            if (cd.getStep() != null && cd.getStep().equals(defaultNext)) {
                continue; // 此条件不针对当前步骤
            }
            if (cd.getCondition() != null && evaluateCondition(cd.getCondition(), data)) {
                return cd.getStep();
            }
        }
        return defaultNext;
    }

    /**
     * 解析起始步骤（考虑条件分支）
     */
    private StepDef resolveFirstStep(WorkflowDef wf, Map<String, Object> data) {
        StepDef first = wf.getFirstStep();
        if (first == null) return null;

        // 检查是否有针对起始步骤的条件分支
        String resolvedStepId = resolveConditionalNext(wf, first.getId(), data);
        if (!resolvedStepId.equals(first.getId())) {
            StepDef resolved = wf.getStep(resolvedStepId);
            if (resolved != null) return resolved;
        }

        return first;
    }

    /**
     * 执行条件表达式（Aviator）
     */
    private boolean evaluateCondition(String expr, Map<String, Object> data) {
        try {
            Map<String, Object> env = new HashMap<>();
            if (data != null) env.putAll(data);
            env.put("_now", LocalDateTime.now());

            Object result = AviatorEvaluator.execute(expr, env);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 寻找匹配的 Action
     */
    private ActionDef findAction(StepDef step, String actionCode) {
        if (step.getActions() == null) return null;
        return step.getActions().stream()
            .filter(a -> a.getCode().equals(actionCode))
            .findFirst()
            .orElse(null);
    }

    /**
     * 创建待办任务
     */
    private Map<String, Object> createTask(Long instanceId, String modelName, Long recordId,
                                            StepDef step, UserContext ctx) {
        Long assigneeId = resolveAssignee(step, ctx);

        jdbcTemplate.update("""
            INSERT INTO astraion_task
                (workflow_inst_id, model_name, record_id, step_id, title, assignee_id, status, created_at)
            VALUES (?,?,?,?,?,?,?,?)
            """,
            instanceId, modelName, recordId, step.getId(),
            step.getName() != null ? step.getName() : ("步骤: " + step.getId()),
            assigneeId, "pending", LocalDateTime.now()
        );

        Long taskId = jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);

        return jdbcTemplate.queryForList(
            "SELECT * FROM astraion_task WHERE id=?", taskId
        ).get(0);
    }

    /**
     * 解析审批人
     */
    private Long resolveAssignee(StepDef step, UserContext ctx) {
        if (step == null) return ctx.getUserId();

        String assignee = step.getAssignee();
        if (assignee == null) return ctx.getUserId();

        // currentUser — 当前用户
        if ("currentUser".equals(assignee)) {
            return ctx.getUserId();
        }

        // role:xxx — 按角色查找第一个用户
        if (assignee.startsWith("role:")) {
            String role = assignee.substring(5);
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id FROM astraion_user WHERE role=? ORDER BY id LIMIT 1", role
            );
            if (!users.isEmpty()) {
                return (Long) users.get(0).get("id");
            }
            // 找不到就指定给当前用户
            return ctx.getUserId();
        }

        // user:xxx — 按用户名查找
        if (assignee.startsWith("user:")) {
            String username = assignee.substring(5);
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id FROM astraion_user WHERE username=?", username
            );
            if (!users.isEmpty()) {
                return (Long) users.get(0).get("id");
            }
            return ctx.getUserId();
        }

        // currentUser.director — 上级（取当前用户的直属上级，暂用 dept_id 关联）
        if ("currentUser.director".equals(assignee)) {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT id FROM astraion_user WHERE dept_id=? AND role IN ('admin','root') ORDER BY id LIMIT 1",
                ctx.getDeptId()
            );
            if (!users.isEmpty()) {
                return (Long) users.get(0).get("id");
            }
            // 找不到则指定给管理员
            List<Map<String, Object>> admins = jdbcTemplate.queryForList(
                "SELECT id FROM astraion_user WHERE role IN ('admin','root') ORDER BY id LIMIT 1"
            );
            if (!admins.isEmpty()) {
                return (Long) admins.get(0).get("id");
            }
            return ctx.getUserId();
        }

        // 默认给当前用户
        return ctx.getUserId();
    }

    /**
     * 从实例记录中获取工作流名称
     */
    private String getWorkflowNameFromInstance(Map<String, Object> instance, String modelName) {
        if (instance == null) return null;
        Long workflowId = (Long) instance.get("workflow_id");
        if (workflowId == null) return null;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT name FROM astraion_workflow WHERE id=?", workflowId
        );
        return rows.isEmpty() ? null : (String) rows.get(0).get("name");
    }

    /**
     * 获取业务记录数据
     */
    private Map<String, Object> getRecordData(String modelName, Long recordId) {
        try {
            String table = "astraion_data_" + modelName;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + " WHERE id=?", recordId
            );
            return rows.isEmpty() ? Map.of() : rows.get(0);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
