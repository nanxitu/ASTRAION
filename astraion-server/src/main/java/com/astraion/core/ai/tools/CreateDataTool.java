package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 创建数据工具 — createData
 */
@Component
public class CreateDataTool implements AstraionTool {

    private final MetadataEngine metadataEngine;
    private final JdbcTemplate jdbcTemplate;

    public CreateDataTool(MetadataEngine metadataEngine, JdbcTemplate jdbcTemplate) {
        this.metadataEngine = metadataEngine;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getName() { return "createData"; }

    @Override
    public String getDescription() { return "在指定模型中创建一条新记录。"; }

    @Override
    public Map<String, Object> getParameterSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("modelName", "string");
        schema.put("data", "string");
        return schema;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        String modelName = (String) params.get("modelName");
        String dataStr = (String) params.get("data");

        if (modelName == null || dataStr == null) {
            return ToolResult.fail("Missing modelName or data");
        }

        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) {
            return ToolResult.fail("模型不存在: " + modelName);
        }

        // Parse data JSON -> Map
        Map<String, Object> data;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            data = om.readValue(dataStr, Map.class);
        } catch (Exception e) {
            return ToolResult.fail("data 格式错误: " + e.getMessage());
        }

        // Build INSERT SQL
        StringBuilder cols = new StringBuilder();
        StringBuilder vals = new StringBuilder();
        List<Object> args = new ArrayList<>();

        cols.append("_created_by");
        vals.append("?");
        args.add(ctx.userId);

        for (FieldDef field : model.getFields()) {
            Object val = data.get(field.getName());
            if (val != null) {
                if (cols.length() > 0) cols.append(", ");
                if (vals.length() > 0) vals.append(", ");
                cols.append(field.getName());
                vals.append("?");
                args.add(val);
            }
        }

        String sql = "INSERT INTO astraion_data_" + modelName
            + " (" + cols + ") VALUES (" + vals + ") RETURNING id";

        try {
            Long id = jdbcTemplate.queryForObject(sql, Long.class, args.toArray());
            return ToolResult.ok("创建成功，ID=" + id, Map.of("id", id));
        } catch (Exception e) {
            return ToolResult.fail("创建失败: " + e.getMessage());
        }
    }
}
