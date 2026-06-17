package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 展示配置 — 控制列表/卡片/表单/详情的显示方式和交互动作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DisplayDef {

    private ListView list;
    private CardsView cards;
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
        private String defaultSort;
        private int pageSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardsView {
        /** 标题字段 */
        private String titleField;
        /** 展示在卡片上的子字段 */
        private List<String> fields;
        /** 卡片上的动作按钮 */
        private List<CardAction> actions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardAction {
        /** 按钮文字 */
        private String label;
        /** 调用的工具名，如 updateData、deleteData */
        private String tool;
        /** 按钮风格：primary / danger / default */
        private String variant;
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
        private List<List<String>> fields;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailView {
        private List<String> fields;
    }
}
