package com.astraion.core.ai.tools;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class CreateUserTool implements AstraionTool {

    private final JdbcTemplate jdbcTemplate;

    public CreateUserTool(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @Override
    public String getName() { return "createUser"; }

    @Override
    public String getDescription() { return "创建一个新用户。仅管理员可操作。需要 username, password, displayName, role。"; }

    @Override
    public Map<String, Object> getParameterSchema() {
        return Map.of("username", "string", "password", "string", "displayName", "string", "role", "string");
    }

    @Override
    public ToolResult execute(Map<String, Object> params, ToolContext ctx) {
        if (!"admin".equals(ctx.role) && !"root".equals(ctx.role)) return ToolResult.fail("仅管理员可创建用户");
        try {
            String username = (String) params.get("username");
            String password = (String) params.get("password");
            String displayName = (String) params.getOrDefault("displayName", username);
            String role = (String) params.getOrDefault("role", "user");
            if (username == null || password == null) return ToolResult.fail("缺少用户名或密码");
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM astraion_user WHERE username=?", Integer.class, username);
            if (count != null && count > 0) return ToolResult.fail("用户名已存在: " + username);
            jdbcTemplate.update("INSERT INTO astraion_user (username, password_hash, display_name, role, status) VALUES (?,?,?,?,'active')", username, password, displayName, role);
            return ToolResult.ok("用户 " + username + " 创建成功", Map.of("username", username, "role", role));
        } catch (Exception e) {
            return ToolResult.fail("创建用户失败: " + e.getMessage());
        }
    }
}
