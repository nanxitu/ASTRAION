package com.astraion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 当前用户上下文 — 贯穿整个请求链
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserContext {

    private Long userId;
    private String username;
    private String displayName;

    /** root / admin / user */
    private String role;

    /** 所属部门 ID（可扩展） */
    private Long deptId;

    /** 角色列表 */
    private List<String> roles;

    /** 自定义属性 */
    private Object extra;

    public boolean isRoot() {
        return "root".equals(role);
    }

    public boolean isAdmin() {
        return "admin".equals(role) || "root".equals(role);
    }

    public boolean isUser() {
        return "user".equals(role);
    }
}
