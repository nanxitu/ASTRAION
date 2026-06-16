package com.astraion.core.ai;

import java.util.Arrays;
import java.util.List;

/**
 * AI 模型提供商预设
 * 每种提供商包含默认 Base URL、可选模型列表和推荐参数
 */
public enum AIModelProvider {

    DEEPSEEK("deepseek", "DeepSeek",
        "https://api.deepseek.com",
        Arrays.asList("deepseek-v4-flash", "deepseek-v4-pro", "deepseek-chat", "deepseek-reasoner"),
        "deepseek-v4-flash",
        1.0, 8192),

    OPENAI("openai", "OpenAI",
        "https://api.openai.com",
        Arrays.asList("gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "o1", "o3-mini"),
        "gpt-4o-mini",
        0.7, 4096),

    QWEN("qwen", "通义千问",
        "https://dashscope.aliyuncs.com",
        Arrays.asList("qwen-max", "qwen-plus", "qwen-turbo"),
        "qwen-plus",
        0.7, 4096),

    OLLAMA("ollama", "Ollama 本地",
        "http://localhost:11434",
        Arrays.asList("llama3", "qwen2.5", "deepseek-r1"),
        "llama3",
        0.7, 4096),

    CUSTOM("custom", "自定义",
        "",
        Arrays.asList(""),
        "",
        0.7, 4096);

    /** 提供商标识，存储到数据库 */
    private final String code;

    /** 显示名称 */
    private final String displayName;

    /** 默认 Base URL */
    private final String defaultBaseUrl;

    /** 可选模型列表 */
    private final List<String> models;

    /** 默认模型 */
    private final String defaultModel;

    /** 推荐 temperature */
    private final double defaultTemperature;

    /** 推荐 maxTokens */
    private final int defaultMaxTokens;

    AIModelProvider(String code, String displayName, String defaultBaseUrl,
                    List<String> models, String defaultModel,
                    double defaultTemperature, int defaultMaxTokens) {
        this.code = code;
        this.displayName = displayName;
        this.defaultBaseUrl = defaultBaseUrl;
        this.models = models;
        this.defaultModel = defaultModel;
        this.defaultTemperature = defaultTemperature;
        this.defaultMaxTokens = defaultMaxTokens;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public String getDefaultBaseUrl() { return defaultBaseUrl; }
    public List<String> getModels() { return models; }
    public String getDefaultModel() { return defaultModel; }
    public double getDefaultTemperature() { return defaultTemperature; }
    public int getDefaultMaxTokens() { return defaultMaxTokens; }

    /** 按 code 查找提供商 */
    public static AIModelProvider fromCode(String code) {
        for (AIModelProvider p : values()) {
            if (p.code.equalsIgnoreCase(code)) {
                return p;
            }
        }
        return CUSTOM;
    }
}
