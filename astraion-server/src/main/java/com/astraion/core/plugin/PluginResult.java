package com.astraion.core.plugin;

/**
 * 插件执行结果
 */
public class PluginResult {
    private final boolean success;
    private final String errorMessage;

    private PluginResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static PluginResult ok() {
        return new PluginResult(true, null);
    }

    public static PluginResult fail(String message) {
        return new PluginResult(false, message);
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
