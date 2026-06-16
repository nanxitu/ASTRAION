package com.astraion.web;

import com.astraion.core.engine.DynamicTableManager;
import com.astraion.core.metadata.MetadataEngine;
import com.astraion.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理控制器
 */
@RestController
@RequestMapping("/api/v1/models")
public class ModelController {

    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;

    public ModelController(MetadataEngine metadataEngine, DynamicTableManager tableManager) {
        this.metadataEngine = metadataEngine;
        this.tableManager = tableManager;
    }

    @PostMapping
    public ApiResponse<ModelMeta> create(@RequestBody ModelMeta meta, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) {
            throw new com.astraion.core.security.PermissionException("仅管理员可创建模型");
        }
        ModelMeta result = metadataEngine.createModel(meta);
        tableManager.createTable(result);
        return ApiResponse.ok("模型创建成功，数据表已自动建好", result);
    }

    @GetMapping
    public ApiResponse<List<ModelMeta>> list() {
        return ApiResponse.ok(metadataEngine.listModels());
    }

    @GetMapping("/{modelName}")
    public ApiResponse<ModelMeta> get(@PathVariable String modelName) {
        return ApiResponse.ok(metadataEngine.getModel(modelName));
    }

    @PutMapping("/{modelName}")
    public ApiResponse<ModelMeta> update(@PathVariable String modelName,
                                         @RequestBody UpdateModelRequest req) {
        ModelMeta result = metadataEngine.updateModel(modelName, req.getFields());
        if ("addFields".equals(req.getOp())) {
            tableManager.addColumns(result, req.getFields());
        }
        return ApiResponse.ok("字段已添加，数据库表已同步", result);
    }

    @DeleteMapping("/{modelName}")
    public ApiResponse<Void> delete(@PathVariable String modelName) {
        metadataEngine.deleteModel(modelName);
        tableManager.dropTable(modelName);
        return ApiResponse.ok("模型已删除", null);
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }

    @Data
    public static class UpdateModelRequest {
        private String op;
        private List<FieldDef> fields;
    }
}
