package com.astraion.core.ai.tools;

import com.astraion.core.engine.DynamicTableManager;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CreateModelTool implements AstraionTool {

    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;

    public CreateModelTool(MetadataEngine metadataEngine, DynamicTableManager tableManager) {
        this.metadataEngine = metadataEngine; this.tableManager = tableManager;
    }

    @Override
    public String getName() { return "createModel"; }

    @Override
    public String getDescription() { return "创建一个新的业务模型（对应数据库表）。需要提供模型名称、显示名称、字段列表。字段类型支持: string, text, integer, decimal, boolean, date, datetime, enum, email, phone, url, relation"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("modelName", "string", "displayName", "string", "description", "string", "fields", "array"); }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        if (!"admin".equals(ctx.role) && !"root".equals(ctx.role)) return ToolResult.fail("仅管理员可创建模型");
        try {
            String modelName = (String) params.get("modelName");
            String displayName = (String) params.get("displayName");
            String description = (String) params.get("description");
            if (modelName == null || displayName == null) return ToolResult.fail("缺少 modelName 或 displayName");

            List<FieldDef> fields = new ArrayList<>();
            Object fieldsObj = params.get("fields");
            if (fieldsObj instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?,?> fm) {
                        FieldDef fd = new FieldDef();
                        fd.setName((String) fm.get("name"));
                        fd.setType((String) fm.get("type"));
                        Object labelObj = fm.get("label");
                        fd.setLabel(labelObj != null ? (String) labelObj : fd.getName());
                        fd.setRequired(Boolean.TRUE.equals(fm.get("required")));
                        fd.setOrder(fields.size() + 1);
                        fields.add(fd);
                    }
                }
            }

            ModelMeta meta = new ModelMeta();
            meta.setModelName(modelName); meta.setDisplayName(displayName);
            meta.setDescription(description); meta.setBuiltin(false); meta.setVersion(1); meta.setFields(fields);
            ModelMeta result = metadataEngine.createModel(meta);
            tableManager.createTable(result);
            return ToolResult.ok("模型 " + displayName + " 创建成功，数据库表已自动建好", Map.of("modelName", result.getModelName(), "fieldCount", fields.size()));
        } catch (Exception e) {
            return ToolResult.fail("创建模型失败: " + e.getMessage());
        }
    }
}
