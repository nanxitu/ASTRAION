package com.astraion.web;

import com.astraion.core.datasource.DataSourceManager;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/datasources")
public class DataSourceController {

    private final DataSourceManager dataSourceManager;

    public DataSourceController(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> add(@RequestBody AddRequest req, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可管理数据源");
        dataSourceManager.addDatasource(
            req.name, req.dbType, req.host, req.port, req.databaseName,
            req.username, req.password, req.mode != null ? req.mode : "readonly", ctx.getUserId()
        );
        return ApiResponse.ok("数据源已注册", null);
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可查看数据源");
        return ApiResponse.ok(dataSourceManager.listDatasources());
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> remove(@PathVariable String name, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可移除数据源");
        dataSourceManager.removeDatasource(name);
        return ApiResponse.ok("数据源已移除", null);
    }

    public static class AddRequest {
        public String name, dbType, host, databaseName, username, password, mode;
        public int port;
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
