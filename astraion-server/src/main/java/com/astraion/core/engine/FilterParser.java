package com.astraion.core.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 筛选参数解析器
 * 格式：name:eq:张三,status:in:active,disabled
 */
public class FilterParser {

    public static List<Filter> parse(String raw) {
        List<Filter> filters = new ArrayList<>();
        if (raw == null || raw.isBlank()) return filters;

        for (String part : raw.split(",")) {
            String[] segs = part.trim().split(":", 3);
            if (segs.length >= 3) {
                filters.add(new Filter(segs[0], segs[1], segs[2]));
            } else if (segs.length == 2) {
                filters.add(new Filter(segs[0], "eq", segs[1]));
            }
        }
        return filters;
    }
}
