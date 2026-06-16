package com.astraion.web;

import com.astraion.config.SystemInitService;
import com.astraion.core.ai.AIModelProvider;
import com.astraion.core.ai.LLMService;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/system")
public class SystemInitController {

    private final SystemInitService initService;
    private final LLMService llmService;
    private final JdbcTemplate jdbcTemplate;

    public SystemInitController(SystemInitService initService, LLMService llmService, JdbcTemplate jdbcTemplate) {
        this.initService = initService;
        this.llmService = llmService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> status() {
        boolean initialized = initService.isInitialized();
        return ApiResponse.ok(Map.of("initialized", initialized));
    }

    /**
     * 获取 AI 提供商预设列表（含 DeepSeek V4）
     */
    @GetMapping("/ai-providers")
    public ApiResponse<List<Map<String, Object>>> getAiProviders() {
        List<Map<String, Object>> providers = new ArrayList<>();
        for (AIModelProvider p : AIModelProvider.values()) {
            if (p == AIModelProvider.CUSTOM) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", p.getCode());
            item.put("displayName", p.getDisplayName());
            item.put("defaultBaseUrl", p.getDefaultBaseUrl());
            item.put("models", p.getModels());
            item.put("defaultModel", p.getDefaultModel());
            item.put("defaultTemperature", p.getDefaultTemperature());
            item.put("defaultMaxTokens", p.getDefaultMaxTokens());
            providers.add(item);
        }
        return ApiResponse.ok(providers);
    }

    /**
     * 获取当前数据库配置（脱敏）
     */
    @GetMapping("/db-config")
    public ApiResponse<Map<String, String>> getDbConfig(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        if (!ctx.isRoot()) return ApiResponse.fail(403, "仅 root 可查看");
        return ApiResponse.ok(initService.getDbConfig());
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test-db")
    public ApiResponse<Map<String, Object>> testDbConnection(@RequestBody TestDbRequest req) {
        try {
            String url = "jdbc:" + req.dbType + "://" + req.host + ":" + req.port + "/" + req.databaseName;
            java.sql.Connection conn = java.sql.DriverManager.getConnection(url, req.username, req.password);
            conn.close();
            return ApiResponse.ok(Map.of("success", true, "message", "数据库连接成功"));
        } catch (Exception e) {
            return ApiResponse.ok(Map.of("success", false, "message", "连接失败: " + e.getMessage()));
        }
    }

    /**
     * 测试 AI 模型连接（无需登录）
     */
    @PostMapping("/test-ai")
    public ApiResponse<Map<String, Object>> testAi(@RequestBody TestAiRequest req) {
        LLMService.TestResult result = llmService.testConnection(req.baseUrl, req.apiKey, req.model);
        return ApiResponse.ok(Map.of(
            "success", result.isSuccess(),
            "message", result.getMessage()
        ));
    }

    /**
     * 获取当前的 AI 配置（管理员可见，API Key 脱敏）
     */
    @GetMapping("/ai-config")
    public ApiResponse<Map<String, String>> getAiConfig(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        return ApiResponse.ok(initService.getAiConfig(ctx));
    }

    /**
     * 管理员更新 AI 模型配置
     */
    @PutMapping("/ai-config")
    public ApiResponse<Void> updateAiConfig(HttpServletRequest request, @RequestBody AIModelConfig config) {
        UserContext ctx = getCtx(request);
        initService.updateAIModelByAdmin(ctx, config.provider, config.model, config.baseUrl,
            config.apiKey, config.temperature, config.maxTokens);
        return ApiResponse.ok("AI config updated", null);
    }

    @PostMapping("/init/database")
    public ApiResponse<Void> configureDatabase(HttpServletRequest request, @RequestBody DatabaseConfig config) {
        UserContext ctx = getCtx(request);
        initService.configureDatabase(ctx, config.dbType, config.host, config.port,
            config.databaseName, config.username, config.password);
        return ApiResponse.ok("DB OK", null);
    }

    @PostMapping("/init/ai-model")
    public ApiResponse<Void> configureAIModel(HttpServletRequest request, @RequestBody AIModelConfig config) {
        UserContext ctx = getCtx(request);
        initService.configureAIModel(ctx, config.provider, config.model, config.baseUrl,
            config.apiKey, config.temperature, config.maxTokens);
        return ApiResponse.ok("AI OK", null);
    }

    @PostMapping("/init/admin")
    public ApiResponse<Map<String, Object>> createAdmin(HttpServletRequest request, @RequestBody CreateAdminRequest req) {
        UserContext ctx = getCtx(request);
        initService.createAdmin(ctx, req.username, req.password, req.displayName);
        return ApiResponse.ok("Admin OK", Map.of("username", req.username));
    }

    @PostMapping("/init/complete")
    public ApiResponse<Void> completeInit(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        initService.markInitialized(ctx);
        return ApiResponse.ok("Done", null);
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, Object>> getConfig(HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        return ApiResponse.ok(initService.getSystemConfig(ctx));
    }

    @PutMapping("/config/ai-model")
    public ApiResponse<Void> updateAIModel(HttpServletRequest request, @RequestBody AIModelConfig config) {
        UserContext ctx = getCtx(request);
        initService.configureAIModel(ctx, config.provider, config.model, config.baseUrl,
            config.apiKey, config.temperature, config.maxTokens);
        return ApiResponse.ok("AI updated", null);
    }

    /**
     * 修改数据库配置 — 仅 root
     */
    @PutMapping("/config/database")
    public ApiResponse<Void> updateDatabase(HttpServletRequest request, @RequestBody DatabaseConfig config) {
        UserContext ctx = getCtx(request);
        initService.configureDatabase(ctx, config.dbType, config.host, config.port,
            config.databaseName, config.username, config.password);
        return ApiResponse.ok("数据库配置已更新", null);
    }

    /**
     * 重置管理员密码 — 仅 root
     */
    @PutMapping("/admin/{id}/reset-password")
    public ApiResponse<Void> resetAdminPassword(@PathVariable Long id, @RequestBody Map<String, String> body,
                                                 HttpServletRequest request) {
        UserContext ctx = getCtx(request);
        if (!ctx.isRoot()) return ApiResponse.fail(403, "仅 ASTRAION root 可重置管理员密码");
        String newPassword = body.get("password");
        if (newPassword == null || newPassword.isEmpty()) return ApiResponse.fail(400, "密码不能为空");

        var users = jdbcTemplate.queryForList("SELECT role FROM astraion_user WHERE id=?", id);
        if (users.isEmpty()) return ApiResponse.fail(404, "用户不存在");
        String role = (String) users.get(0).get("role");
        if (!"admin".equals(role)) return ApiResponse.fail(400, "只能重置管理员密码");

        jdbcTemplate.update("UPDATE astraion_user SET password_hash=?, updated_at=NOW() WHERE id=?",
            newPassword, id); // TODO: BCrypt
        return ApiResponse.ok("密码已重置", null);
    }

    private UserContext getCtx(HttpServletRequest r) {
        return (UserContext) r.getAttribute("userContext");
    }

    // ---- DTOs ----

    public static class DatabaseConfig {
        public String dbType, host, databaseName, username, password;
        public int port;
    }

    public static class AIModelConfig {
        public String provider, model, baseUrl, apiKey;
        public double temperature = 0.7;
        public int maxTokens = 4096;
    }

    public static class CreateAdminRequest {
        public String username, password, displayName;
    }

    public static class TestAiRequest {
        public String baseUrl, apiKey, model;
    }

    public static class TestDbRequest {
        public String dbType, host, databaseName, username, password;
        public int port;
    }
}
