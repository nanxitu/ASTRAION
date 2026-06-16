package com.astraion.core.ai.tools;

import com.astraion.core.engine.DynamicCRUD;
import com.astraion.model.UserContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeleteDataTool implements AstraionTool {

    private final DynamicCRUD dynamicCRUD;

    public DeleteDataTool(DynamicCRUD dynamicCRUD) { this.dynamicCRUD = dynamicCRUD; }

    @Override
    public String getName() { return "deleteData"; }

    @Override
    public String getDescription() { return "删除指定模型中的一条数据。需要 modelName 和 recordId。"; }

    @Override
    public Map<String, Object> getParameterSchema() { return Map.of("modelName", "string", "recordId", "integer"); }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        try {
            String modelName = (String) params.get("modelName");
            Object recordIdObj = params.get("recordId");
            if (modelName == null || recordIdObj == null) return ToolResult.fail("缺少 modelName 或 recordId");
            Long recordId = toLong(recordIdObj);
            UserContext userCtx = buildCtx(ctx);
            dynamicCRUD.delete(modelName, recordId, userCtx);
            return ToolResult.ok("数据已删除", Map.of("modelName", modelName, "recordId", recordId));
        } catch (Exception e) {
            return ToolResult.fail("删除失败: " + e.getMessage());
        }
    }

    private Long toLong(Object o) { return o instanceof Number n ? n.longValue() : Long.parseLong(o.toString()); }

    private UserContext buildCtx(ToolContext ctx) {
        UserContext u = new UserContext();
        u.setUserId(Long.parseLong(ctx.userId)); u.setUsername(ctx.username); u.setRole(ctx.role);
        return u;
    }
}
