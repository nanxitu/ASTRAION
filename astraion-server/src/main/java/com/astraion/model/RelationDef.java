package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型关联关系定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationDef {

    /** 关系名称 */
    private String name;

    /** 关系类型：belongsTo, hasMany, hasManyThrough, belongsToMany */
    private String type;

    /** 目标模型名 */
    private String targetModel;

    /** 外键字段名 */
    private String foreignKey;

    /** 显示标签 */
    private String label;
}
