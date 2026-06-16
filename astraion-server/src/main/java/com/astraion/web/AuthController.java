package com.astraion.web;

import com.astraion.config.SystemInitService;
import com.astraion.core.security.JwtUtil;
import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器 — 登录 / 获取当前用户信息
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final JwtUtil jwtUtil;
    private final SystemInitService initService;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil, SystemInitService initService) {
        this.jdbcTemplate = jdbcTemplate;
        this.jwtUtil = jwtUtil;
        this.initService = initService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest req) {
        // 查用户
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, username, password_hash, display_name, role FROM astraion_user WHERE username=?",
            req.getUsername()
        );

        if (users.isEmpty()) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }

        Map<String, Object> user = users.get(0);
        String storedPassword = (String) user.get("password_hash");

        // TODO: 生产环境恢复 BCrypt
        if (!req.getPassword().equals(storedPassword)) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }

        Long userId = (Long) user.get("id");
        String role = (String) user.get("role");
        String username = (String) user.get("username");

        String token = jwtUtil.generateToken(userId, username, role);

        // 更新最后登录时间
        jdbcTemplate.update("UPDATE astraion_user SET last_login_at=NOW() WHERE id=?", userId);

        boolean initialized = initService.isInitialized();

        return ApiResponse.ok(Map.of(
            "token", token,
            "expiresIn", 86400,
            "initialized", initialized,
            "user", Map.of(
                "id", userId,
                "username", username,
                "displayName", user.getOrDefault("display_name", username),
                "role", role
            )
        ));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        // 客户端删除 Token 即可
        return ApiResponse.ok(null);
    }

    @GetMapping("/me")
    public ApiResponse<UserContext> me(@RequestAttribute("userContext") UserContext ctx) {
        return ApiResponse.ok(ctx);
    }

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
