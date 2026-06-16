package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * ASTRAION 元数据模型定义 — 一切业务的起点
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelMeta {

    /** 模型标识：小写+下划线，如 customer、leave_request */
    private String modelName;

    /** 显示名称：客户、请假申请 */
    private String displayName;

    /** 描述 */
    private String description;

    /** 是否系统内置（内置模型不可删除） */
    @Builder.Default
    private boolean builtin = false;

    /** 版本号，每次变更自增 */
    @Builder.Default
    private int version = 1;

    /** 字段定义 */
    @Builder.Default
    private List<FieldDef> fields = new ArrayList<>();

    /** 模型间关联关系 */
    @Builder.Default
    private List<RelationDef> relations = new ArrayList<>();

    /** 索引定义 */
    @Builder.Default
    private List<IndexDef> indexes = new ArrayList<>();

    /** 生命周期钩子绑定 */
    private LifecycleDef lifecycle;

    /** 权限规则 */
    private PermissionDef permissions;

    /** 审批流程（可多个） */
    @Builder.Default
    private List<WorkflowDef> workflows = new ArrayList<>();

    /** 展示配置 */
    private DisplayDef display;

    /** 状态：active / deprecated / disabled */
    @Builder.Default
    private String status = "active";
}
