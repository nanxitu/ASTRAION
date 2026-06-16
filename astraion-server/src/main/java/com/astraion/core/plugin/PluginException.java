package com.astraion.core.plugin;

/**
 * 插件操作异常
 */
public class PluginException extends RuntimeException {
    public PluginException(String message) {
        super(message);
    }
    public PluginException(String message, Throwable cause) {
        super(message, cause);
    }
}
