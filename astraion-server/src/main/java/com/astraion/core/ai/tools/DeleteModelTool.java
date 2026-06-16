package com.astraion.core.ai.tools;

import com.astraion.core.engine.DynamicTableManager;
import com.astraion.core.metadata.MetadataEngine;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class DeleteModelTool implements AstraionTool {

    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;

    public DeleteModelTool(MetadataEngine metadataEngine, DynamicTableManager tableManager) {
        this.metadataEngine = metadataEngine; this.tableManager = tableManager;
    }

    @Override
    public String getName() { return "deleteModel"; }

    @Override
    public String getDescription() { return "删除一个业务模型及其所有数据。操作不可逆！需要用户明确确认。"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("modelName", "string"); }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        if (!"admin".equals(ctx.role) && !"root".equals(ctx.role)) return ToolResult.fail("仅管理员可删除模型");
        try {
            String modelName = (String) params.get("modelName");
            if (modelName == null) return ToolResult.fail("缺少 modelName");
            metadataEngine.deleteModel(modelName);
            tableManager.dropTable(modelName);
            return ToolResult.ok("模型 " + modelName + " 已删除", Map.of("modelName", modelName));
        } catch (Exception e) {
            return ToolResult.fail("删除模型失败: " + e.getMessage());
        }
    }
}
