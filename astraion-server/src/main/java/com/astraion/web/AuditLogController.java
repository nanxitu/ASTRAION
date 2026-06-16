package com.astraion.web;

import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 审计日志控制器 — 仅管理员可查看
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
public class AuditLogController {

    private final JdbcTemplate jdbcTemplate;

    public AuditLogController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String modelName,
            @RequestParam(required = false) String action,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        if (!ctx.isAdmin()) return ApiResponse.fail(403, "仅管理员可查看审计日志");

        int offset = (page - 1) * size;
        StringBuilder where = new StringBuilder();
        if (modelName != null) where.append(" AND model_name='").append(modelName).append("'");
        if (action != null) where.append(" AND action='").append(action).append("'");

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_audit_log WHERE 1=1" + where, Long.class);
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT al.*, u.username, u.display_name FROM astraion_audit_log al " +
            "LEFT JOIN astraion_user u ON al.user_id=u.id WHERE 1=1" + where +
            " ORDER BY al.created_at DESC LIMIT ? OFFSET ?", size, offset);

        return ApiResponse.ok(Map.of("total", total, "items", items, "page", page, "size", size));
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
