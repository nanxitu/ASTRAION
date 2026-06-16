package com.astraion.web;

import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final JdbcTemplate jdbcTemplate;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 创建用户（管理员权限）
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createUser(@RequestBody CreateUserRequest req,
                                                        HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) {
            return ApiResponse.fail(403, "仅管理员可创建用户");
        }

        // 检查用户名唯一性
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_user WHERE username=?", Integer.class, req.getUsername()
        );
        if (count != null && count > 0) {
            return ApiResponse.fail(400, "用户名已存在");
        }

        String hash = req.getPassword(); // TODO: BCrypt

        jdbcTemplate.update("""
            INSERT INTO astraion_user (username, password_hash, display_name, email, phone, role, dept_id)
            VALUES (?,?,?,?,?,?,?)
            """,
            req.getUsername(), hash, req.getDisplayName(),
            req.getEmail(), req.getPhone(),
            req.getRole() != null ? req.getRole() : "user",
            req.getDeptId()
        );

        Long userId = jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);

        return ApiResponse.ok("用户创建成功", Map.of("id", userId, "username", req.getUsername()));
    }

    /**
     * 用户列表
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) {
            return ApiResponse.fail(403, "仅管理员可查看用户列表");
        }

        int offset = (page - 1) * size;
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_user", Long.class
        );

        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, username, display_name, email, phone, role, dept_id, status, last_login_at, created_at " +
            "FROM astraion_user ORDER BY id DESC LIMIT ? OFFSET ?",
            size, offset
        );

        return ApiResponse.ok(Map.of(
            "total", total != null ? total : 0L,
            "items", users,
            "page", page,
            "size", size
        ));
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public ApiResponse<Map<String, Object>> getUser(@PathVariable Long userId) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, username, display_name, email, phone, role, dept_id, status, last_login_at, created_at " +
            "FROM astraion_user WHERE id=?", userId
        );
        if (users.isEmpty()) {
            return ApiResponse.fail(404, "用户不存在");
        }
        return ApiResponse.ok(users.get(0));
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{userId}")
    public ApiResponse<Map<String, Object>> updateUser(@PathVariable Long userId,
                                                        @RequestBody UpdateUserRequest req,
                                                        HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin() && !ctx.getUserId().equals(userId)) {
            return ApiResponse.fail(403, "无权修改此用户信息");
        }

        jdbcTemplate.update("""
            UPDATE astraion_user
            SET display_name=COALESCE(?, display_name),
                email=COALESCE(?, email),
                phone=COALESCE(?, phone),
                updated_at=NOW()
            WHERE id=?
            """,
            req.getDisplayName(), req.getEmail(), req.getPhone(), userId
        );

        return ApiResponse.ok("用户信息已更新", Map.of("id", userId));
    }

    /**
     * 修改密码
     */
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changePassword(@PathVariable Long userId,
                                             @RequestBody ChangePasswordRequest req,
                                             HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin() && !ctx.getUserId().equals(userId)) {
            return ApiResponse.fail(403, "无权修改此用户密码");
        }

        // 非管理员修改需要验证旧密码
        if (!ctx.isAdmin()) {
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                "SELECT password_hash FROM astraion_user WHERE id=?", userId
            );
            if (users.isEmpty()) {
                return ApiResponse.fail(404, "用户不存在");
            }
            String oldHash = (String) users.get(0).get("password_hash");
            if (!req.getOldPassword().equals(oldHash)) { // TODO: BCrypt
                return ApiResponse.fail(400, "旧密码不正确");
            }
        }

        String newHash = req.getNewPassword(); // TODO: BCrypt
        jdbcTemplate.update(
            "UPDATE astraion_user SET password_hash=?, updated_at=NOW() WHERE id=?",
            newHash, userId
        );

        return ApiResponse.ok("密码已修改", null);
    }

    /**
     * 删除用户（管理员权限）
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable Long userId, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) {
            return ApiResponse.fail(403, "仅管理员可删除用户");
        }

        // 不能删除自己
        if (ctx.getUserId().equals(userId)) {
            return ApiResponse.fail(400, "不能删除当前登录用户");
        }

        jdbcTemplate.update("DELETE FROM astraion_user WHERE id=?", userId);
        return ApiResponse.ok("用户已删除", null);
    }

    // ---- DTOs ----

    public static class CreateUserRequest {
        private String username;
        private String password;
        private String displayName;
        private String email;
        private String phone;
        private String role;
        private Long deptId;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
    }

    public static class UpdateUserRequest {
        private String displayName;
        private String email;
        private String phone;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;

        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    // ---- helpers ----

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
