package com.astraion.core.ai.tools;

import com.astraion.core.ai.tools.AstraionTool.ToolContext;
import com.astraion.core.ai.tools.AstraionTool.ToolResult;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.ModelMeta;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 模型列表工具 — listModels
 */
@Component
public class ModelListTool implements AstraionTool {

    private final MetadataEngine metadataEngine;

    public ModelListTool(MetadataEngine metadataEngine) {
        this.metadataEngine = metadataEngine;
    }

    @Override
    public String getName() { return "listModels"; }

    @Override
    public String getDescription() { return "列出所有可用的业务模型及其字段。"; }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of();
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        List<ModelMeta> models = metadataEngine.listModels();
        List<Map<String, Object>> result = new ArrayList<>();
        for (ModelMeta m : models) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("modelName", m.getModelName());
            item.put("displayName", m.getDisplayName());
            item.put("description", m.getDescription());
            item.put("fields", m.getFields());
            result.add(item);
        }
        return ToolResult.ok("共 " + result.size() + " 个模型", result);
    }
}
