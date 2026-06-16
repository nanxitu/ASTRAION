package com.astraion.core.engine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 分页查询结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult {
    private long total;
    private List<Map<String, Object>> items;
    private int page;
    private int size;

    public int getTotalPages() {
        return (int) Math.ceil((double) total / size);
    }
}
