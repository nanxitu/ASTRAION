package com.astraion.core.security;

import com.astraion.core.engine.QueryParams;
import com.astraion.model.ModelMeta;
import com.astraion.model.PermissionDef;
import com.astraion.model.UserContext;
import com.googlecode.aviator.AviatorEvaluator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 权限引擎 — 解析和执行权限规则
 *
 * 支持表达式：
 *   role:admin          — 角色判断
 *   authenticated       — 已登录
 *   owner:currentUser   — 数据所有者
 *   dept == currentUser.dept  — Aviator 表达式
 */
@Component
public class PermissionEngine {

    public PermissionEngine() {
        // Aviator 安全设置
        System.setProperty("aviator.preferClassloaderDefiner", "false");
    }

    /**
     * 检查操作权限
     */
    public void check(ModelMeta model, String action, Object data, UserContext ctx) {
        // root 拥有所有权限
        if (ctx.isRoot()) return;

        PermissionDef perm = model.getPermissions();
        if (perm == null) return;

        String rule = perm.getRule(action);
        if (rule == null && "create".equals(action)) rule = perm.getRule("create");
        if (rule == null && "read".equals(action)) rule = perm.getRule("read");
        if (rule == null) return; // 无规则 = 允许

        if (!evaluate(rule, ctx, data)) {
            throw new PermissionException("无权执行此操作: " + action);
        }
    }

    /**
     * 追加行级过滤条件到查询参数
     */
    public void applyRowFilter(ModelMeta model, String action, QueryParams params, UserContext ctx) {
        // root 和 admin 看到所有数据
        if (ctx.isAdmin()) return;

        PermissionDef perm = model.getPermissions();
        if (perm == null || perm.getRowPermissions() == null) return;

        for (PermissionDef.RowPermission rp : perm.getRowPermissions()) {
            if (rp.getActions() != null && rp.getActions().contains(action)) {
                String condition = convertToSql(rp.getRule(), ctx);
                if (condition != null) {
                    params.addFilter(condition);
                }
            }
        }
    }

    /**
     * 执行权限表达式
     */
    private boolean evaluate(String expression, UserContext ctx, Object data) {
        if (expression == null || expression.isBlank()) return true;

        // 角色判断
        if (expression.startsWith("role:")) {
            String role = expression.substring(5);
            return ctx.getRoles() != null && ctx.getRoles().contains(role);
        }

        // 已登录
        if ("authenticated".equals(expression)) return true;

        // 数据所有者
        if (expression.startsWith("owner:currentUser")) {
            if (data instanceof Map) {
                Object createdBy = ((Map<?, ?>) data).get("_created_by");
                return ctx.getUserId().equals(createdBy);
            }
            return false;
        }

        // Aviator 表达式
        try {
            Map<String, Object> env = Map.of(
                "currentUser", ctx,
                "userId", ctx.getUserId(),
                "dept", ctx.getDeptId() != null ? ctx.getDeptId().toString() : "",
                "data", data != null ? data : Map.of()
            );
            Object result = AviatorEvaluator.execute(expression, env);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 将权限规则转为 SQL 条件
     */
    private String convertToSql(String rule, UserContext ctx) {
        if (rule == null) return null;

        // owner:currentUser → _created_by = currentUserId
        if (rule.startsWith("owner:currentUser")) {
            return "_created_by = " + ctx.getUserId();
        }

        // dept == currentUser.dept → _字段_ = currentDeptId
        if (rule.contains("dept") && rule.contains("currentUser.dept")) {
            return "dept_id = " + ctx.getDeptId();
        }

        return null;
    }
}
