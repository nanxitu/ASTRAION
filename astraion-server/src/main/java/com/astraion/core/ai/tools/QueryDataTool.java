package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.ModelMeta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 查询数据工具 — queryData
 */
@Component
public class QueryDataTool implements AstraionTool {

    private static final Logger log = LoggerFactory.getLogger(QueryDataTool.class);

    private final MetadataEngine metadataEngine;
    private final JdbcTemplate jdbcTemplate;

    public QueryDataTool(MetadataEngine metadataEngine, JdbcTemplate jdbcTemplate) {
        this.metadataEngine = metadataEngine;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() { return "queryData"; }

    @Override
    public String getDescription() {
        return "查询数据。支持业务模型（如 department、employee）和系统表（user、role）。支持筛选、排序、分页。";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("modelName", "string");
        schema.put("filter", "string");
        schema.put("limit", "string");
        schema.put("orderBy", "string");
        return schema;
    }

    /** 允许通过 queryData 直接查询的系统表名 */
    private static final Set<String> SYSTEM_TABLES = Set.of("user", "role");

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        String modelName = (String) params.get("modelName");
        if (modelName == null || modelName.isEmpty()) {
            return ToolResult.fail("Missing modelName");
        }

        // 先查业务模型
        ModelMeta model = metadataEngine.getModel(modelName);
        String tableName;

        if (model != null) {
            tableName = "astraion_data_" + modelName;
        } else if (SYSTEM_TABLES.contains(modelName)) {
            tableName = "astraion_" + modelName;
        } else {
            return ToolResult.fail("模型不存在: " + modelName);
        }

        String filter = (String) params.getOrDefault("filter", "");
        String limitStr = (String) params.getOrDefault("limit", "50");
        String orderBy = (String) params.getOrDefault("orderBy", "id DESC");

        int limit;
        try {
            limit = Math.min(Integer.parseInt(limitStr), 200);
        } catch (NumberFormatException e) {
            limit = 50;
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);
        sql.append(" WHERE 1=1");

        if (!filter.isEmpty()) {
            sql.append(" AND ").append(filter);
        }

        if (orderBy != null && !orderBy.isEmpty() && !orderBy.isBlank()) {
            sql.append(" ORDER BY ").append(orderBy);
        } else {
            sql.append(" ORDER BY id");
        }
        sql.append(" LIMIT ").append(limit);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());
            return ToolResult.ok("查询到 " + rows.size() + " 条记录", rows);
        } catch (Exception e) {
            log.warn("[QueryData] Failed for {} params={}: {}",
                modelName, params.getOrDefault("filter",""), e.getMessage());
            return ToolResult.fail("查询失败");
        }
    }
}
