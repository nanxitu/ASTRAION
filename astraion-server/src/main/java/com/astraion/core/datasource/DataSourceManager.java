package com.astraion.core.datasource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多数据源管理器 — 管理主数据库和外部数据源
 */
@Component
public class DataSourceManager {

    private final JdbcTemplate primaryJdbc;
    /** 外部数据源连接池 */
    private final Map<String, DataSource> externalSources = new ConcurrentHashMap<>();

    public DataSourceManager(JdbcTemplate primaryJdbc) {
        this.primaryJdbc = primaryJdbc;
    }

    /**
     * 添加外部数据源
     */
    public void addDatasource(String name, String dbType, String host, int port,
                               String dbName, String username, String password,
                               String mode, Long createdBy) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        String url = buildJdbcUrl(dbType, host, port, dbName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);

        // 测试连接
        new JdbcTemplate(ds).queryForObject("SELECT 1", Integer.class);

        externalSources.put(name, ds);

        // 保存到数据库
        primaryJdbc.update(
            "INSERT INTO astraion_datasource (name, db_type, host, port, database_name, username, password_enc, mode, created_by) VALUES (?,?,?,?,?,?,?,?,?)",
            name, dbType, host, port, dbName, username, password, mode, createdBy
        );
    }

    /**
     * 获取外部数据源
     */
    public JdbcTemplate getExternalJdbcTemplate(String name) {
        DataSource ds = externalSources.get(name);
        if (ds == null) {
            throw new RuntimeException("数据源不存在: " + name);
        }
        return new JdbcTemplate(ds);
    }

    /**
     * 移除外部数据源
     */
    public void removeDatasource(String name) {
        externalSources.remove(name);
        primaryJdbc.update("DELETE FROM astraion_datasource WHERE name=?", name);
    }

    /**
     * 列出所有外部数据源
     */
    public List<Map<String, Object>> listDatasources() {
        return primaryJdbc.queryForList(
            "SELECT id, name, db_type, host, port, database_name, mode, status, created_at FROM astraion_datasource WHERE status='active'"
        );
    }

    private String buildJdbcUrl(String dbType, String host, int port, String dbName) {
        return switch (dbType.toLowerCase()) {
            case "postgresql" -> "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            case "mysql" -> "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
            default -> throw new RuntimeException("不支持的数据库类型: " + dbType);
        };
    }
}
