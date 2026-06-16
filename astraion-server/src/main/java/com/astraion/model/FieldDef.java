package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 字段定义
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldDef {

    /** 字段名：数据库列名，小写+下划线 */
    private String name;

    /** 数据类型：string, text, integer, decimal, boolean, date, datetime, enum, enumMulti, relation, relationMulti, file, image, json, encrypted, password, email, phone, url, autoNumber */
    private String type;

    /** 显示标签 */
    private String label;

    /** 是否必填 */
    private boolean required;

    /** 是否唯一 */
    private boolean unique;

    /** 默认值 */
    private Object defaultValue;

    /** 最大长度（文本类型） */
    private Integer maxLength;

    /** 最小长度 */
    private Integer minLength;

    /** 占位提示 */
    private String placeholder;

    /** 字段描述 */
    private String description;

    /** 是否只读 */
    private boolean readonly;

    /** 是否隐藏 */
    private boolean hidden;

    /** 字段排序 */
    @Builder.Default
    private int order = 0;

    /** enum/enuMulti 类型的选项列表 */
    private Object options; // List<String> or List<Map>

    /** 校验规则 */
    private Map<String, Object> validation;

    /** relation/relationMulti 类型的目标模型名 */
    private String targetModel;
}
