package com.astraion.core.engine;

import com.astraion.model.FieldDef;
import com.astraion.model.IndexDef;
import com.astraion.model.ModelMeta;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 动态表管理器 — 根据元数据 CREATE TABLE / ALTER TABLE
 */
@Component
public class DynamicTableManager {

    private final JdbcTemplate jdbcTemplate;
    private static final String TABLE_PREFIX = "astraion_data_";

    public DynamicTableManager(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String tableName(String modelName) {
        return TABLE_PREFIX + modelName;
    }

    /**
     * 根据模型元数据创建数据表
     */
    public void createTable(ModelMeta model) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName(model.getModelName())).append(" (");
        sql.append("id BIGSERIAL PRIMARY KEY, ");
        sql.append("_created_at TIMESTAMP NOT NULL DEFAULT NOW(), ");
        sql.append("_updated_at TIMESTAMP NOT NULL DEFAULT NOW(), ");
        sql.append("_created_by BIGINT, ");
        sql.append("_updated_by BIGINT, ");
        sql.append("_version INTEGER NOT NULL DEFAULT 1");

        for (FieldDef field : model.getFields()) {
            if (isSystemField(field.getName())) continue;
            sql.append(", ").append(field.getName()).append(" ").append(mapToSqlType(field));
            if (field.isRequired()) sql.append(" NOT NULL");
            if (field.getDefaultValue() != null) sql.append(" DEFAULT ").append(formatDefault(field));
        }

        sql.append(")");
        jdbcTemplate.execute(sql.toString());

        // 创建索引
        for (IndexDef index : model.getIndexes()) {
            String idxName = "idx_" + model.getModelName() + "_" + index.getName();
            String idxCols = index.getColumns();
            String unique = index.isUnique() ? "UNIQUE " : "";
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS " + idxName + " ON " + tableName(model.getModelName()) + " (" + idxCols + ")");
        }
    }

    /**
     * 动态添加列
     */
    public void addColumns(ModelMeta model, List<FieldDef> newFields) {
        for (FieldDef field : newFields) {
            if (isSystemField(field.getName())) continue;
            String sql = "ALTER TABLE " + tableName(model.getModelName())
                       + " ADD COLUMN IF NOT EXISTS " + field.getName() + " " + mapToSqlType(field);
            jdbcTemplate.execute(sql);
        }
    }

    /**
     * 删除表
     */
    public void dropTable(String modelName) {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + tableName(modelName));
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(String modelName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
            Integer.class, tableName(modelName).toLowerCase()
        );
        return count != null && count > 0;
    }

    // ---- helpers ----

    private boolean isSystemField(String name) {
        return name.startsWith("_") || "id".equals(name);
    }

    private String mapToSqlType(FieldDef field) {
        return switch (field.getType()) {
            case "string"   -> "VARCHAR(" + (field.getMaxLength() != null ? field.getMaxLength() : 255) + ")";
            case "text"     -> "TEXT";
            case "integer"  -> "INTEGER";
            case "decimal"  -> "DECIMAL(18,6)";
            case "boolean"  -> "BOOLEAN";
            case "date"     -> "DATE";
            case "datetime" -> "TIMESTAMP";
            case "email", "phone", "url", "autoNumber" -> "VARCHAR(255)";
            case "encrypted", "password" -> "VARCHAR(512)";
            case "file", "image" -> "VARCHAR(1024)";
            case "json", "enum", "enumMulti", "relationMulti" -> "JSONB";
            case "relation" -> "BIGINT";
            default -> "VARCHAR(255)";
        };
    }

    private String formatDefault(FieldDef field) {
        Object dv = field.getDefaultValue();
        if (dv == null) return null;
        if (dv instanceof String s) {
            if ("NOW()".equalsIgnoreCase(s) || "CURRENT_TIMESTAMP".equalsIgnoreCase(s)) {
                return "NOW()";
            }
            return "'" + s.replace("'", "''") + "'";
        }
        return String.valueOf(dv);
    }
}
