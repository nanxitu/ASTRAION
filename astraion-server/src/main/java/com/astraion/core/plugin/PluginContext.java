package com.astraion.core.plugin;

import com.astraion.model.UserContext;

import java.util.Map;

/**
 * 插件执行上下文 — 传递给插件的数据
 */
public class PluginContext {
    private String modelName;
    private String hookPoint;
    private Map<String, Object> data;
    private Map<String, Object> oldData;
    private UserContext user;

    public PluginContext() {}

    public PluginContext(String modelName, String hookPoint, Map<String, Object> data, UserContext user) {
        this.modelName = modelName;
        this.hookPoint = hookPoint;
        this.data = data;
        this.user = user;
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getHookPoint() { return hookPoint; }
    public void setHookPoint(String hookPoint) { this.hookPoint = hookPoint; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
    public Map<String, Object> getOldData() { return oldData; }
    public void setOldData(Map<String, Object> oldData) { this.oldData = oldData; }
    public UserContext getUser() { return user; }
    public void setUser(UserContext user) { this.user = user; }
}
