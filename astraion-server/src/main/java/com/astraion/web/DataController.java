package com.astraion.web;

import com.astraion.core.engine.*;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 动态数据 CRUD 控制器 — 所有业务数据的统一入口
 */
@RestController
@RequestMapping("/api/v1/data")
public class DataController {

    private final DynamicCRUD dynamicCRUD;

    public DataController(DynamicCRUD dynamicCRUD) {
        this.dynamicCRUD = dynamicCRUD;
    }

    @PostMapping("/{modelName}")
    public ApiResponse<Map<String, Object>> create(@PathVariable String modelName,
                                                    @RequestBody Map<String, Object> data,
                                                    HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        return ApiResponse.ok(dynamicCRUD.create(modelName, data, ctx));
    }

    @GetMapping("/{modelName}")
    public ApiResponse<PageResult> list(@PathVariable String modelName,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(required = false) String sort,
                                         @RequestParam(required = false) String filter,
                                         HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        QueryParams params = QueryParams.builder()
            .page(page).size(size).sort(sort)
            .filters(FilterParser.parse(filter))
            .build();
        return ApiResponse.ok(dynamicCRUD.queryList(modelName, params, ctx));
    }

    @GetMapping("/{modelName}/{id}")
    public ApiResponse<Map<String, Object>> get(@PathVariable String modelName,
                                                 @PathVariable Long id,
                                                 HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        return ApiResponse.ok(dynamicCRUD.findById(modelName, id, ctx));
    }

    @PutMapping("/{modelName}/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable String modelName,
                                                    @PathVariable Long id,
                                                    @RequestBody Map<String, Object> data,
                                                    HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        return ApiResponse.ok(dynamicCRUD.update(modelName, id, data, ctx));
    }

    @DeleteMapping("/{modelName}/{id}")
    public ApiResponse<Void> delete(@PathVariable String modelName,
                                     @PathVariable Long id,
                                     HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        dynamicCRUD.delete(modelName, id, ctx);
        return ApiResponse.ok(null);
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
