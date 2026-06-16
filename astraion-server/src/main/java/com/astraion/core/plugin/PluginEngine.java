package com.astraion.core.plugin;

import com.astraion.model.ModelMeta;
import com.astraion.model.UserContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件引擎 — 热加载和管理业务插件
 */
@Component
public class PluginEngine {

    private final Map<String, List<AstraionPlugin>> hookRegistry = new ConcurrentHashMap<>();
    private final Map<String, AstraionPlugin> plugins = new ConcurrentHashMap<>();

    public void registerPlugin(AstraionPlugin plugin) {
        plugins.put(plugin.getName(), plugin);
        for (String hook : plugin.getHookPoints()) {
            hookRegistry.computeIfAbsent(hook, k -> new CopyOnWriteArrayList<>()).add(plugin);
        }
    }

    public AstraionPlugin loadJarPlugin(Path jarPath) {
        try {
            URLClassLoader loader = new URLClassLoader(
                new URL[]{jarPath.toUri().toURL()},
                Thread.currentThread().getContextClassLoader()
            );
            ServiceLoader<AstraionPlugin> serviceLoader = ServiceLoader.load(AstraionPlugin.class, loader);
            for (AstraionPlugin plugin : serviceLoader) {
                registerPlugin(plugin);
                return plugin;
            }
            throw new PluginException("Jar包中未找到 AstraionPlugin 实现类");
        } catch (Exception e) {
            throw new PluginException("加载插件失败: " + e.getMessage());
        }
    }

    public void executeHooks(ModelMeta model, String hookPoint, Map<String, Object> data, UserContext ctx) {
        String key = hookPoint + ":" + model.getModelName();
        List<AstraionPlugin> hookPlugins = hookRegistry.get(key);
        if (hookPlugins == null) return;

        PluginContext pc = new PluginContext(model.getModelName(), hookPoint, data, ctx);

        for (AstraionPlugin plugin : hookPlugins) {
            PluginResult result = plugin.execute(pc);
            if (!result.isSuccess()) {
                throw new PluginException(result.getErrorMessage());
            }
        }
    }

    public void unregisterPlugin(String name) {
        AstraionPlugin plugin = plugins.remove(name);
        if (plugin != null) {
            for (String hook : plugin.getHookPoints()) {
                List<AstraionPlugin> list = hookRegistry.get(hook);
                if (list != null) list.remove(plugin);
            }
        }
    }

    public Map<String, AstraionPlugin> getAllPlugins() {
        return Collections.unmodifiableMap(plugins);
    }
}
