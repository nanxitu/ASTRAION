package com.astraion.core.ai.tools;

import com.astraion.core.engine.DynamicTableManager;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class UpdateModelTool implements AstraionTool {

    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;

    public UpdateModelTool(MetadataEngine metadataEngine, DynamicTableManager tableManager) {
        this.metadataEngine = metadataEngine; this.tableManager = tableManager;
    }

    @Override
    public String getName() { return "updateModel"; }

    @Override
    public String getDescription() { return "为已有模型添加新字段。需要提供模型名和新字段列表。"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("modelName", "string", "fields", "array"); }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        if (!"admin".equals(ctx.role) && !"root".equals(ctx.role)) return ToolResult.fail("仅管理员可修改模型");
        try {
            String modelName = (String) params.get("modelName");
            Object fieldsObj = params.get("fields");
            if (modelName == null || !(fieldsObj instanceof List<?> list)) return ToolResult.fail("缺少 modelName 或 fields");
            List<FieldDef> newFields = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?,?> fm) {
                    FieldDef fd = new FieldDef();
                    fd.setName((String) fm.get("name")); fd.setType((String) fm.get("type"));
                    Object labelObj = fm.get("label");
                    fd.setLabel(labelObj != null ? (String) labelObj : fd.getName());
                    fd.setRequired(Boolean.TRUE.equals(fm.get("required")));
                    newFields.add(fd);
                }
            }
            ModelMeta result = metadataEngine.updateModel(modelName, newFields);
            tableManager.addColumns(result, newFields);
            return ToolResult.ok("模型已更新，新增 " + newFields.size() + " 个字段", Map.of("modelName", modelName));
        } catch (Exception e) {
            return ToolResult.fail("更新模型失败: " + e.getMessage());
        }
    }
}
