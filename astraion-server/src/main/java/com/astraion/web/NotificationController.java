package com.astraion.web;

import com.astraion.model.ApiResponse;
import com.astraion.model.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通知管理控制器
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final JdbcTemplate jdbcTemplate;

    public NotificationController(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    /** 获取通知列表 */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        int offset = (page - 1) * size;
        String where = unread ? " AND read_flag=false" : "";
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_notification WHERE user_id=?" + where, Long.class, ctx.getUserId());
        List<Map<String, Object>> items = jdbcTemplate.queryForList(
            "SELECT * FROM astraion_notification WHERE user_id=?" + where + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            ctx.getUserId(), size, offset);
        return ApiResponse.ok(Map.of("total", total, "items", items, "page", page, "size", size));
    }

    /** 标记已读 */
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        jdbcTemplate.update("UPDATE astraion_notification SET read_flag=true WHERE id=? AND user_id=?", id, ctx.getUserId());
        return ApiResponse.ok(null);
    }

    /** 全部已读 */
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead(HttpServletRequest request) {
        UserContext ctx = getUserContext(request);
        jdbcTemplate.update("UPDATE astraion_notification SET read_flag=true WHERE user_id=?", ctx.getUserId());
        return ApiResponse.ok(null);
    }

    private UserContext getUserContext(HttpServletRequest request) {
        return (UserContext) request.getAttribute("userContext");
    }
}
