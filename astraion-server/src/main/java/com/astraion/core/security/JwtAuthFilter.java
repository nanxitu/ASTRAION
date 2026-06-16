package com.astraion.core.security;

import com.astraion.model.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器 — 每个请求前解析 Token 并注入 UserContext
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     FilterChain chain) throws IOException, ServletException {

        // 跳过公开接口
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth/login")
            || path.startsWith("/api/v1/system/status")
            || path.startsWith("/api/v1/system/ai-providers")
            || path.startsWith("/api/v1/system/test-ai")
            || path.startsWith("/api/v1/system/test-db")) {
            chain.doFilter(request, response);
            return;
        }

        // 解析 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseToken(token);
                UserContext ctx = UserContext.builder()
                    .userId(claims.get("userId", Long.class))
                    .username(claims.get("username", String.class))
                    .role(claims.get("role", String.class))
                    .roles(List.of(claims.get("role", String.class)))
                    .build();
                request.setAttribute("userContext", ctx);
            } catch (Exception e) {
                response.setStatus(401);
                response.getWriter().write("{\"code\":401,\"message\":\"Token无效或已过期\"}");
                return;
            }
        } else {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"请先登录\"}");
            return;
        }

        chain.doFilter(request, response);
    }
}
