package com.astraion.core.engine;

import com.astraion.core.security.PermissionEngine;
import com.astraion.model.FieldDef;
import com.astraion.model.ModelMeta;
import com.astraion.model.UserContext;
import com.astraion.core.metadata.MetadataEngine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态 CRUD 引擎 — 基于元数据自动生成增删改查
 */
@Component
public class DynamicCRUD {

    private final JdbcTemplate jdbcTemplate;
    private final MetadataEngine metadataEngine;
    private final DynamicTableManager tableManager;
    private final PermissionEngine permissionEngine;

    public DynamicCRUD(JdbcTemplate jdbcTemplate, MetadataEngine metadataEngine,
                       DynamicTableManager tableManager, PermissionEngine permissionEngine) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataEngine = metadataEngine;
        this.tableManager = tableManager;
        this.permissionEngine = permissionEngine;
    }

    /**
     * 创建记录
     */
    public Map<String, Object> create(String modelName, Map<String, Object> data, UserContext ctx) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) throw new CrudException("模型不存在: " + modelName);

        // 权限检查
        permissionEngine.check(model, "create", data, ctx);

        // 过滤掉不属于该模型的字段
        Set<String> fieldNames = model.getFields().stream().map(FieldDef::getName).collect(Collectors.toSet());
        data = data.entrySet().stream()
            .filter(e -> fieldNames.contains(e.getKey()))
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);

        // 自动填入系统字段
        data.put("_created_by", ctx.getUserId());
        data.put("_updated_by", ctx.getUserId());

        // 构建 INSERT SQL
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableManager.tableName(modelName));
        List<String> cols = new ArrayList<>(data.keySet());
        sql.append(" (").append(String.join(", ", cols)).append(")");
        sql.append(" VALUES (").append(cols.stream().map(c -> "?").collect(Collectors.joining(", "))).append(")");

        Object[] params = cols.stream().map(data::get).toArray();
        jdbcTemplate.update(sql.toString(), params);
        Long id = jdbcTemplate.queryForObject("SELECT SCOPE_IDENTITY()", Long.class);

        // 返回完整记录
        return findById(modelName, id, ctx);
    }

    /**
     * 查询列表（分页 + 筛选 + 排序）
     */
    public PageResult queryList(String modelName, QueryParams params, UserContext ctx) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) throw new CrudException("模型不存在: " + modelName);

        // 权限校验 + 自动追加行级过滤
        permissionEngine.applyRowFilter(model, "read", params, ctx);

        String table = tableManager.tableName(modelName);
        String where = buildWhereClause(params.getFilters());
        String orderBy = buildOrderClause(params.getSort());
        int offset = (params.getPage() - 1) * params.getSize();

        // 计数
        String countSql = "SELECT COUNT(*) FROM " + table + where;
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        // 查询
        String querySql = "SELECT * FROM " + table + where + orderBy + " LIMIT ? OFFSET ?";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(querySql, params.getSize(), offset);

        return new PageResult(total, items, params.getPage(), params.getSize());
    }

    /**
     * 查询单条
     */
    public Map<String, Object> findById(String modelName, Long id, UserContext ctx) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) throw new CrudException("模型不存在: " + modelName);

        String sql = "SELECT * FROM " + tableManager.tableName(modelName) + " WHERE id=?";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, id);
        if (results.isEmpty()) throw new CrudException("记录不存在: " + modelName + "/" + id);

        Map<String, Object> record = results.get(0);
        permissionEngine.check(model, "read", record, ctx);
        return record;
    }

    /**
     * 更新记录
     */
    public Map<String, Object> update(String modelName, Long id, Map<String, Object> data, UserContext ctx) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) throw new CrudException("模型不存在: " + modelName);

        Map<String, Object> existing = findById(modelName, id, ctx);
        permissionEngine.check(model, "update", existing, ctx);

        // 过滤字段 + 自动时间戳
        Set<String> fieldNames = model.getFields().stream().map(FieldDef::getName).collect(Collectors.toSet());
        data = data.entrySet().stream()
            .filter(e -> fieldNames.contains(e.getKey()))
            .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), HashMap::putAll);
        data.put("_updated_by", ctx.getUserId());
        data.put("_updated_at", LocalDateTime.now());

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableManager.tableName(modelName)).append(" SET ");
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            sql.append(e.getKey()).append("=?, ");
            params.add(e.getValue());
        }
        sql.setLength(sql.length() - 2); // 去掉最后的 ", "
        sql.append(" WHERE id=?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());
        return findById(modelName, id, ctx);
    }

    /**
     * 删除记录
     */
    public void delete(String modelName, Long id, UserContext ctx) {
        ModelMeta model = metadataEngine.getModel(modelName);
        if (model == null) throw new CrudException("模型不存在: " + modelName);

        Map<String, Object> existing = findById(modelName, id, ctx);
        permissionEngine.check(model, "delete", existing, ctx);

        jdbcTemplate.update("DELETE FROM " + tableManager.tableName(modelName) + " WHERE id=?", id);
    }

    // ---- private helpers ----

    private String buildWhereClause(List<Filter> filters) {
        if (filters == null || filters.isEmpty()) return "";
        return " WHERE " + filters.stream()
            .map(this::filterToSql)
            .collect(Collectors.joining(" AND "));
    }

    private String filterToSql(Filter f) {
        return switch (f.getOp()) {
            case "eq"    -> f.getField() + " = '" + escape(f.getValue()) + "'";
            case "neq"   -> f.getField() + " != '" + escape(f.getValue()) + "'";
            case "gt"    -> f.getField() + " > '" + escape(f.getValue()) + "'";
            case "lt"    -> f.getField() + " < '" + escape(f.getValue()) + "'";
            case "like"  -> f.getField() + " LIKE '" + escape(f.getValue()) + "'";
            case "in"    -> f.getField() + " IN (" + f.getValue() + ")";
            default      -> f.getField() + " = '" + escape(f.getValue()) + "'";
        };
    }

    private String buildOrderClause(String sort) {
        if (sort == null || sort.isBlank()) return " ORDER BY _created_at DESC";
        // sort = "name:asc,createdAt:desc"
        String[] parts = sort.split(",");
        return " ORDER BY " + Arrays.stream(parts)
            .map(p -> p.trim().replace(":", " "))
            .collect(Collectors.joining(", "));
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("'", "''");
    }
}
