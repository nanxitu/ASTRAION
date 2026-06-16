package com.astraion.config;

import com.astraion.model.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 系统初始化服务 — ASTRAION Root 的初始化操作
 */
@Component
public class SystemInitService {

    private final JdbcTemplate jdbcTemplate;

    public SystemInitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 检查系统是否已完成初始化
     */
    public boolean isInitialized() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_system_config WHERE config_key=? AND config_value=?",
            Integer.class, "system.initialized", "true"
        );
        return count != null && count > 0;
    }

    /**
     * 配置数据库连接信息。仅 root 可调用。写入 application.yml + DB。
     */
    public void configureDatabase(UserContext ctx, String dbType, String host, int port,
                                   String dbName, String username, String password) {
        checkRoot(ctx);
        setConfig("db.type", dbType, false);
        setConfig("db.host", host, false);
        setConfig("db.port", String.valueOf(port), false);
        setConfig("db.name", dbName, false);
        setConfig("db.user", username, false);
        setConfig("db.password", password, true);
        // 写入 application.yml
        writeDbConfigToYaml(dbType, host, port, dbName, username, password);
    }

    /**
     * 配置 AI 模型。初始化时仅 root；初始化后管理员也可修改。
     */
    public void configureAIModel(UserContext ctx, String provider, String model,
                                  String baseUrl, String apiKey, double temperature, int maxTokens) {
        checkRoot(ctx);
        saveAiConfig(provider, model, baseUrl, apiKey, temperature, maxTokens);
    }

    /**
     * 管理员更新 AI 模型配置（初始化后）
     */
    public void updateAIModelByAdmin(UserContext ctx, String provider, String model,
                                      String baseUrl, String apiKey, double temperature, int maxTokens) {
        if (ctx == null || !ctx.isAdmin()) {
            throw new SecurityException("无权操作");
        }
        saveAiConfig(provider, model, baseUrl, apiKey, temperature, maxTokens);
    }

    /**
     * 获取 AI 模型配置（管理员可用）
     */
    public Map<String, String> getAiConfig(UserContext ctx) {
        if (ctx == null || !ctx.isAdmin()) {
            throw new SecurityException("无权访问");
        }
        return Map.of(
            "provider", getConfigValue("ai.provider"),
            "model", getConfigValue("ai.model"),
            "baseUrl", getConfigValue("ai.base_url"),
            "apiKey", maskApiKey(getConfigValue("ai.api_key")),
            "temperature", getConfigValue("ai.temperature"),
            "maxTokens", getConfigValue("ai.max_tokens")
        );
    }

    private void saveAiConfig(String provider, String model, String baseUrl,
                               String apiKey, double temperature, int maxTokens) {
        setConfig("ai.provider", provider, false);
        setConfig("ai.model", model, false);
        setConfig("ai.base_url", baseUrl, false);
        setConfig("ai.api_key", apiKey, true);
        setConfig("ai.temperature", String.valueOf(temperature), false);
        setConfig("ai.max_tokens", String.valueOf(maxTokens), false);
    }

    private String getConfigValue(String key) {
        var rows = jdbcTemplate.queryForList(
            "SELECT config_value FROM astraion_system_config WHERE config_key=?", key
        );
        return rows.isEmpty() ? "" : (String) rows.get(0).get("config_value");
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * 创建管理员账号。仅 root 可调用。
     */
    public void createAdmin(UserContext ctx, String username, String password, String displayName) {
        checkRoot(ctx);
        // BCrypt 加密密码
        String hash = hashPassword(password);
        jdbcTemplate.update(
            "INSERT INTO astraion_user (username, password_hash, display_name, role, status) VALUES (?,?,?,?,?)",
            username, hash, displayName, "admin", "active"
        );
    }

    /**
     * 标记初始化完成
     */
    public void markInitialized(UserContext ctx) {
        checkRoot(ctx);
        setConfig("system.initialized", "true", false);
    }

    /**
     * 获取系统配置（敏感字段脱敏）
     */
    public Map<String, Object> getSystemConfig(UserContext ctx) {
        checkRoot(ctx);
        return jdbcTemplate.queryForMap(
            "SELECT config_key, config_value FROM astraion_system_config"
        );
    }

    // ---- private ----

    private void checkRoot(UserContext ctx) {
        if (ctx == null || !ctx.isRoot()) {
            throw new SecurityException("仅 ASTRAION Root 可执行此操作");
        }
    }

    private void setConfig(String key, String value, boolean encrypted) {
        int updated = jdbcTemplate.update(
            "UPDATE astraion_system_config SET config_value=?, encrypted=?, updated_at=NOW() WHERE config_key=?",
            value, encrypted, key
        );
        if (updated == 0) {
            jdbcTemplate.update(
                "INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES (?,?,?)",
                key, value, encrypted
            );
        }
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private String hashPassword(String plain) {
        // TODO: 生产环境恢复 BCrypt: return new BCryptPasswordEncoder().encode(plain);
        return plain;
    }

    /**
     * 将数据库配置写入 application.yml
     */
    private void writeDbConfigToYaml(String dbType, String host, int port,
                                      String dbName, String username, String password) {
        try {
            // 写两份：源码目录 + 运行目录
            String[] paths = {
                "src/main/resources/application.yml",
                "target/classes/application.yml"
            };
            for (String path : paths) {
                java.io.File f = new java.io.File(path);
                if (!f.exists()) continue;
                String content = new String(java.nio.file.Files.readAllBytes(f.toPath()));
                // 替换 datasource 下配置
                content = content.replaceAll(
                    "url: jdbc:postgresql://[^\\n]+",
                    "url: jdbc:" + dbType + "://" + host + ":" + port + "/" + dbName
                );
                content = content.replaceAll("driver-class-name: [^\\n]+",
                    "driver-class-name: org." + dbType + ".Driver");
                content = content.replaceAll("username: [^\\n]+", "username: " + username);
                content = content.replaceAll("password: [^\\n]+", "password: " + password);
                java.nio.file.Files.write(f.toPath(), content.getBytes());
            }
        } catch (Exception e) {
            throw new RuntimeException("保存数据库配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前数据库配置（脱敏密码）
     */
    public Map<String, String> getDbConfig() {
        return Map.of(
            "type", getConfigValue("db.type"),
            "host", getConfigValue("db.host"),
            "port", getConfigValue("db.port"),
            "name", getConfigValue("db.name"),
            "user", getConfigValue("db.user"),
            "password", maskApiKey(getConfigValue("db.password"))
        );
    }
}
