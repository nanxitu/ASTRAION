package com.astraion.web;

import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final JdbcTemplate jdbcTemplate;

    public RoleController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 获取所有角色 */
    @GetMapping
    public ApiResponse<List<Map<String, Object>>> listRoles() {
        List<Map<String, Object>> roles = jdbcTemplate.queryForList("SELECT * FROM astraion_role");
        return ApiResponse.ok(roles);
    }

    /** 创建角色 */
    @PostMapping
    public ApiResponse<Map<String, Object>> createRole(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可管理角色");

        String name = (String) body.get("name");
        String code = (String) body.get("code");
        String description = (String) body.get("description");

        jdbcTemplate.update("INSERT INTO astraion_role (name, code, description) VALUES (?,?,?)", name, code, description);
        Long id = jdbcTemplate.queryForObject("SELECT LASTVAL()", Long.class);
        return ApiResponse.ok("角色创建成功", Map.of("id", id, "name", name));
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRole(@PathVariable Long id, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可管理角色");
        jdbcTemplate.update("DELETE FROM astraion_role WHERE id=?", id);
        return ApiResponse.ok(null);
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
