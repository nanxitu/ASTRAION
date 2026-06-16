package com.astraion.core.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用查询参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryParams {

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 20;

    private String sort;
    private List<Filter> filters;

    public void addFilter(String field, String op, String value) {
        if (filters == null) filters = new ArrayList<>();
        filters.add(new Filter(field, op, value));
    }

    public void addFilter(Filter filter) {
        if (filters == null) filters = new ArrayList<>();
        filters.add(filter);
    }

    public void addFilter(String rawCondition) {
        if (filters == null) filters = new ArrayList<>();
        filters.add(new Filter("_row_permission", "raw", rawCondition));
    }
}
