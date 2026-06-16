package com.astraion.core.ai.tools;

import com.astraion.core.engine.DynamicCRUD;
import com.astraion.model.UserContext;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 更新数据工具
 */
@Component
public class UpdateDataTool implements AstraionTool {

    private final DynamicCRUD dynamicCRUD;

    public UpdateDataTool(DynamicCRUD dynamicCRUD) {
        this.dynamicCRUD = dynamicCRUD;
    }

    @Override
    public String getName() { return "updateData"; }

    @Override
    public String getDescription() { return "更新指定模型中的一条数据。需要 modelName、recordId 和要更新的字段数据。"; }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of(
            "modelName", "string",
            "recordId", "integer",
            "data", "object"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        try {
            String modelName = (String) params.get("modelName");
            Object recordIdObj = params.get("recordId");
            Map<String, Object> data = (Map<String, Object>) params.get("data");

            if (modelName == null || recordIdObj == null || data == null) {
                return ToolResult.fail("缺少必要参数: modelName, recordId, data");
            }

            Long recordId = toLong(recordIdObj);

            UserContext userCtx = new UserContext();
            userCtx.setUserId(Long.parseLong(ctx.userId));
            userCtx.setUsername(ctx.username);
            userCtx.setRole(ctx.role);

            Map<String, Object> result = dynamicCRUD.update(modelName, recordId, data, userCtx);
            return ToolResult.ok("数据更新成功", result);
        } catch (Exception e) {
            return ToolResult.fail("更新数据失败: " + e.getMessage());
        }
    }

    private Long toLong(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }
}
