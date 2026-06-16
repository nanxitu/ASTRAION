package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 审批流程定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkflowDef {

    private String name;
    private String description;

    /** 触发时机：onCreate, onUpdate, manual */
    @Builder.Default
    private String trigger = "onCreate";

    /** 关联的业务模型（可选，不填则通用） */
    private String modelName;

    /** 流程步骤 */
    @Builder.Default
    private List<StepDef> steps = List.of();

    /** 条件分支：根据条件选择不同步骤路径 */
    @Builder.Default
    private List<ConditionDef> conditions = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepDef {
        private String id;
        private String name;
        private String type;          // user_task | auto | end
        private String assignee;      // 审批人：currentUser | currentUser.director | role:xxx | user:xxx
        private String assigneeExpr;  // 或表达式：${dept.manager}
        private Integer timeout;      // 超时秒数
        private String timeoutAction; // autoApprove | autoReject | escalate
        private List<ActionDef> actions;

        public StepDef getNextStep(String actionCode) {
            if (actions == null) return null;
            for (ActionDef a : actions) {
                if (a.getCode().equals(actionCode)) {
                    StepDef next = new StepDef();
                    next.setId(a.getNext());
                    return next;
                }
            }
            return null;
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionDef {
        private String code;     // approve | reject | submit
        private String label;    // 通过 | 驳回 | 提交
        private String next;     // 下一步骤 ID，或 "end" / "start"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConditionDef {
        private String step;
        private String condition;  // Aviator 表达式
        private String description;
    }

    public StepDef getFirstStep() {
        return steps != null && !steps.isEmpty() ? steps.get(0) : null;
    }

    public StepDef getStep(String stepId) {
        if (steps == null) return null;
        return steps.stream().filter(s -> s.getId().equals(stepId)).findFirst().orElse(null);
    }
}
