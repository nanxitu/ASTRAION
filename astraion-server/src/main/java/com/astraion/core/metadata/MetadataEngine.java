package com.astraion.core.metadata;

import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 元数据引擎 — 管理所有模型定义
 */
@Component
public class MetadataEngine {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /** 模型缓存 */
    private final Map<String, ModelMeta> modelCache = new ConcurrentHashMap<>();

    public MetadataEngine(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建新模型
     */
    public ModelMeta createModel(ModelMeta meta) {
        validateModelName(meta.getModelName());
        validateFields(meta.getFields());

        try {
            String config = objectMapper.writeValueAsString(meta);

            jdbcTemplate.update(
                "INSERT INTO astraion_model (model_name, display_name, description, builtin, version, config, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)",
                meta.getModelName(),
                meta.getDisplayName(),
                meta.getDescription(),
                meta.isBuiltin(),
                meta.getVersion(),
                config,
                meta.getStatus(),
                LocalDateTime.now(),
                LocalDateTime.now()
            );

            modelCache.put(meta.getModelName(), meta);
            return meta;
        } catch (JsonProcessingException e) {
            throw new MetadataException("序列化模型定义失败: " + e.getMessage());
        }
    }

    /**
     * 获取模型定义（缓存优先）
     */
    public ModelMeta getModel(String modelName) {
        return modelCache.computeIfAbsent(modelName, this::loadFromDb);
    }

    /**
     * 列出所有活跃模型
     */
    public List<ModelMeta> listModels() {
        return jdbcTemplate.query(
            "SELECT config FROM astraion_model WHERE status = 'active' ORDER BY model_name",
            (rs, rowNum) -> parseConfig(rs.getString("config"))
        );
    }

    /**
     * 更新模型（增删字段）
     */
    public ModelMeta updateModel(String modelName, List<FieldDef> addFields) {
        ModelMeta model = getModel(modelName);
        if (model == null) throw new MetadataException("模型不存在: " + modelName);

        model.getFields().addAll(addFields);
        model.setVersion(model.getVersion() + 1);

        try {
            String config = objectMapper.writeValueAsString(model);
            jdbcTemplate.update(
                "UPDATE astraion_model SET config=?, version=?, updated_at=? WHERE model_name=?",
                config, model.getVersion(), LocalDateTime.now(), modelName
            );
            modelCache.put(modelName, model);
            return model;
        } catch (JsonProcessingException e) {
            throw new MetadataException("序列化失败: " + e.getMessage());
        }
    }

    /**
     * 删除模型
     */
    public void deleteModel(String modelName) {
        ModelMeta model = getModel(modelName);
        if (model != null && model.isBuiltin()) {
            throw new MetadataException("内置模型不可删除: " + modelName);
        }
        jdbcTemplate.update("DELETE FROM astraion_model WHERE model_name=?", modelName);
        modelCache.remove(modelName);
    }

    /**
     * 加载所有模型到缓存（启动时调用）
     */
    public void loadAllModels() {
        List<ModelMeta> models = jdbcTemplate.query(
            "SELECT config FROM astraion_model WHERE status = 'active'",
            (rs, rowNum) -> parseConfig(rs.getString("config"))
        );
        for (ModelMeta model : models) {
            modelCache.put(model.getModelName(), model);
        }
    }

    /**
     * 缓存失效
     */
    public void invalidateCache(String modelName) {
        modelCache.remove(modelName);
    }

    // ---- private helpers ----

    private ModelMeta loadFromDb(String modelName) {
        List<ModelMeta> results = jdbcTemplate.query(
            "SELECT config FROM astraion_model WHERE model_name=? AND status='active'",
            (rs, rowNum) -> parseConfig(rs.getString("config")),
            modelName
        );
        return results.isEmpty() ? null : results.get(0);
    }

    private ModelMeta parseConfig(String config) {
        try {
            return objectMapper.readValue(config, ModelMeta.class);
        } catch (JsonProcessingException e) {
            throw new MetadataException("解析模型配置失败: " + e.getMessage());
        }
    }

    private void validateModelName(String name) {
        if (name == null || !name.matches("^[a-z][a-z0-9_]*$")) {
            throw new MetadataException("模型名必须是小写字母开头，只能包含小写字母、数字和下划线: " + name);
        }
    }

    private void validateFields(List<FieldDef> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new MetadataException("模型至少需要一个字段");
        }
        Set<String> names = new HashSet<>();
        for (FieldDef f : fields) {
            if (!names.add(f.getName())) {
                throw new MetadataException("字段名重复: " + f.getName());
            }
        }
    }
}
