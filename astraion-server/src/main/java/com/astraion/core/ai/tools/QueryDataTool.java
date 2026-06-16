package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.ModelMeta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 查询数据工具 — queryData
 */
@Component
public class QueryDataTool implements AstraionTool {

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
        return "查询业务模型数据。支持筛选、排序、分页。适用于所有已创建的模型。";
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

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        String modelName = (String) params.get("modelName");
        if (modelName == null || modelName.isEmpty()) {
            return ToolResult.fail("Missing modelName");
        }

        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) {
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
        sql.append("SELECT * FROM astraion_data_").append(modelName);
        sql.append(" WHERE 1=1");

        if (!filter.isEmpty()) {
            sql.append(" AND ").append(filter);
        }

        sql.append(" ORDER BY ").append(orderBy);
        sql.append(" LIMIT ").append(limit);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString());
            return ToolResult.ok("查询到 " + rows.size() + " 条记录", rows);
        } catch (Exception e) {
            return ToolResult.fail("查询失败: " + e.getMessage());
        }
    }
}
