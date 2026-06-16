package com.astraion.core.ai;

import com.astraion.core.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器 — 处理 AI 对话连接
 * 前端连到 ws://host/ws?token=xxx
 */
@Component
public class AIWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AIWebSocketHandler.class);

    private final JwtUtil jwtUtil;
    private final AIOrchestrator orchestrator;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public AIWebSocketHandler(JwtUtil jwtUtil, AIOrchestrator orchestrator, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.orchestrator = orchestrator;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token == null) {
            closeWithError(session, "Missing token");
            return;
        }
        try {
            Claims claims = jwtUtil.parseToken(token);
            String userId = String.valueOf(claims.get("userId"));
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);

            session.getAttributes().put("userId", userId);
            session.getAttributes().put("username", username);
            session.getAttributes().put("role", role);
            sessions.put(session.getId(), session);

            log.info("[WS] Connected: user={} role={} session={}", username, role, session.getId());
        } catch (Exception e) {
            closeWithError(session, "Invalid token: " + e.getMessage());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.path("type").asText("");
            String content = json.path("content").asText("");

            if ("user_message".equals(type) || "message".equals(type)) {
                // 异步处理，避免阻塞 WebSocket 线程
                String sessionId = session.getId();
                String userId = (String) session.getAttributes().get("userId");
                String username = (String) session.getAttributes().get("username");
                String role = (String) session.getAttributes().get("role");

                orchestrator.handleMessage(sessionId, userId, username, role, content,
                    response -> sendToSession(sessionId, response));
            }
        } catch (Exception e) {
            log.error("[WS] Error handling message", e);
            sendToSession(session.getId(), makeJson("error", "系统处理出错: " + e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("[WS] Disconnected: session={}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[WS] Transport error: session={} err={}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    // ---- helpers ----

    private String extractToken(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("token".equals(kv[0]) && kv.length > 1) {
                return kv[1];
            }
        }
        return null;
    }

    public void sendToSession(String sessionId, String json) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.error("[WS] Send failed session={}", sessionId, e);
                sessions.remove(sessionId);
            }
        }
    }

    private String makeJson(String type, String content) {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("type", type);
            node.put("content", content);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return "{\"type\":\"error\",\"content\":\"JSON error\"}";
        }
    }

    private void closeWithError(WebSocketSession session, String msg) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION.withReason(msg));
        } catch (IOException ignored) {}
    }
}
