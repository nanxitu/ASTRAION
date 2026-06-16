package com.astraion.core.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询筛选条件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Filter {
    private String field;
    private String op;     // eq, neq, gt, lt, gte, lte, like, in, between, raw
    private String value;
}
