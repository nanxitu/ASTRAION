package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生命周期钩子绑定 — 每个钩子可绑多个插件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LifecycleDef {
    private List<String> beforeCreate;
    private List<String> afterCreate;
    private List<String> beforeUpdate;
    private List<String> afterUpdate;
    private List<String> beforeDelete;
    private List<String> afterDelete;
    private List<String> beforeQuery;
    private List<String> afterQuery;
}
