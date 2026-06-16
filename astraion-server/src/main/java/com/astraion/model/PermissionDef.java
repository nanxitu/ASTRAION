package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 权限规则定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PermissionDef {

    /** 操作级权限：create / read / update / delete → 表达式 */
    @Builder.Default
    private Map<String, String> rules = Map.of();

    /** 字段级权限 */
    @Builder.Default
    private List<FieldPermission> fieldPermissions = List.of();

    /** 行级权限 */
    @Builder.Default
    private List<RowPermission> rowPermissions = List.of();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldPermission {
        private String field;
        private String read;
        private String write;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowPermission {
        private String rule;
        private List<String> actions;
    }

    public String getRule(String action) {
        return rules != null ? rules.get(action) : null;
    }
}
