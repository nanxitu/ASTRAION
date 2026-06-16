package com.astraion.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LLM 调用服务 — 兼容 OpenAI Chat Completions 接口
 * 支持 DeepSeek V4 / OpenAI / 通义千问 / Ollama 等
 */
@Service
public class LLMService {

    private static final Logger log = LoggerFactory.getLogger(LLMService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public LLMService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 测试 AI 模型连接
     * 发送一条简单消息，验证 API Key 和模型是否可用
     *
     * @param baseUrl   API 基础地址，如 https://api.deepseek.com
     * @param apiKey    API Key
     * @param model     模型名，如 deepseek-v4-flash
     * @return 测试结果
     */
    public TestResult testConnection(String baseUrl, String apiKey, String model) {
        try {
            String url = baseUrl;
            // 自动补全 /v1/chat/completions
            if (!url.endsWith("/chat/completions")) {
                if (!url.endsWith("/v1")) {
                    url = url.endsWith("/") ? url + "v1/chat/completions" : url + "/v1/chat/completions";
                } else {
                    url = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
                }
            }

            // 构建请求体
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", "user");
            msg.put("content", "Respond with only: OK");
            messages.add(msg);
            body.set("messages", messages);
            body.put("max_tokens", 32);
            body.put("stream", false);

            String jsonBody = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String reply = root.path("choices").get(0).path("message").path("content").asText("");
                return TestResult.success("连接成功: " + reply.trim());
            } else {
                // 读取错误信息
                String errorMsg;
                try {
                    JsonNode err = objectMapper.readTree(response.body());
                    errorMsg = err.path("error").path("message").asText(response.body());
                } catch (Exception e) {
                    errorMsg = response.body();
                }
                return TestResult.fail("请求失败 (" + response.statusCode() + "): " + errorMsg);
            }

        } catch (java.net.ConnectException e) {
            return TestResult.fail("无法连接服务器，请检查 Base URL 是否可访问");
        } catch (java.net.http.HttpTimeoutException e) {
            return TestResult.fail("连接超时，请检查网络或 Base URL");
        } catch (Exception e) {
            log.error("AI 连接测试失败", e);
            return TestResult.fail("测试失败: " + e.getMessage());
        }
    }

    /**
     * 发送完整请求（含工具），返回原始响应 JSON
     */
    public String sendRequest(String baseUrl, String apiKey, JsonNode messages,
                               double temperature, int maxTokens, String model, JsonNode tools) throws Exception {
        String url = normalizeUrl(baseUrl);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);
        if (tools != null && tools.size() > 0) {
            body.set("tools", tools);
            body.put("tool_choice", "auto");
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .timeout(Duration.ofSeconds(60))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM 调用失败 (" + response.statusCode() + "): " + response.body());
        }

        return response.body();
    }

    private String normalizeUrl(String baseUrl) {
        String url = baseUrl;
        if (!url.endsWith("/chat/completions")) {
            if (!url.endsWith("/v1")) {
                url = url.endsWith("/") ? url + "v1/chat/completions" : url + "/v1/chat/completions";
            } else {
                url = url.endsWith("/") ? url + "chat/completions" : url + "/chat/completions";
            }
        }
        return url;
    }

    public static class TestResult {
        private final boolean success;
        private final String message;

        private TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static TestResult success(String message) {
            return new TestResult(true, message);
        }

        public static TestResult fail(String message) {
            return new TestResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
