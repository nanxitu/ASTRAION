package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 展示配置 — 控制列表/表单/详情的显示方式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisplayDef {

    private ListView list;
    private FormView form;
    private DetailView detail;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListView {
        private List<String> columns;
        private List<String> sortable;
        private List<String> filterable;
        @Builder.Default
        private int pageSize = 20;
        private String defaultSort;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormView {
        private List<FormGroup> groups;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FormGroup {
        private String label;
        private List<List<String>> fields; // [["name","phone"],["email"]]
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailView {
        private List<String> fields;
    }
}
