package com.astraion.core.plugin;

import com.astraion.model.UserContext;
import java.util.List;
import java.util.Map;

/**
 * ASTRAION 插件接口 — 所有业务插件必须实现
 */
public interface AstraionPlugin {
    String getName();
    String getVersion();
    List<String> getHookPoints();
    PluginResult execute(PluginContext ctx);
    default Map<String, Object> getDefaultConfig() { return Map.of(); }
}
