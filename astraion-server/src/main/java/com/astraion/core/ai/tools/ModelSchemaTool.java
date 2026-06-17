package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.ModelMeta;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 模型结构工具 — getModelSchema
 */
@Component
public class ModelSchemaTool implements AstraionTool {

    private final MetadataEngine metadataEngine;

    public ModelSchemaTool(MetadataEngine metadataEngine) {
        this.metadataEngine = metadataEngine;
    }

    @Override
    public String getName() { return "getModelSchema"; }

    @Override
    public String getDescription() {
        return "获取指定业务模型的字段结构（字段名、类型、是否必填等）。";
    }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "modelName", Map.of(
                    "type", "string",
                    "description", "模型名称，如 department、employee"
                )
            ),
            "required", List.of("modelName")
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        String modelName = (String) params.get("modelName");
        if (modelName == null || modelName.isBlank()) {
            return ToolResult.fail("请指定模型名称 (modelName)");
        }

        try {
            ModelMeta model = metadataEngine.getModel(modelName);
            if (model == null) {
                return ToolResult.fail("模型不存在: " + modelName);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("modelName", model.getModelName());
            result.put("displayName", model.getDisplayName());
            result.put("description", model.getDescription());
            result.put("fields", model.getFields());
            result.put("relations", model.getRelations());
            return ToolResult.ok("模型 " + modelName + " 的结构", result);
        } catch (Exception e) {
            return ToolResult.fail("获取模型结构失败: " + e.getMessage());
        }
    }
}
