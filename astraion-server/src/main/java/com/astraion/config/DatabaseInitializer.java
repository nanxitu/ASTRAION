package com.astraion.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化 — 建表 + 写入初始数据
 * 直接使用 application.yml 中配置的 PostgreSQL
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        initMetaTables();
        initSystemConfig();
        ensureDbInitialized();
        initRootUser();
    }

    private void initMetaTables() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_model (id BIGSERIAL PRIMARY KEY, model_name VARCHAR(128) NOT NULL UNIQUE, display_name VARCHAR(256) NOT NULL, description TEXT, builtin BOOLEAN NOT NULL DEFAULT FALSE, version INTEGER NOT NULL DEFAULT 1, config TEXT NOT NULL DEFAULT '{}', status VARCHAR(32) NOT NULL DEFAULT 'active', created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW(), created_by BIGINT)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_model_field (id BIGSERIAL PRIMARY KEY, model_id BIGINT NOT NULL REFERENCES astraion_model(id) ON DELETE CASCADE, field_name VARCHAR(128) NOT NULL, field_type VARCHAR(64) NOT NULL, label VARCHAR(256), required BOOLEAN DEFAULT FALSE, unique_flag BOOLEAN DEFAULT FALSE, field_order INTEGER DEFAULT 0, created_at TIMESTAMP DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_user (id BIGSERIAL PRIMARY KEY, username VARCHAR(128) NOT NULL UNIQUE, password_hash VARCHAR(256) NOT NULL, display_name VARCHAR(256), email VARCHAR(256), phone VARCHAR(32), role VARCHAR(32) NOT NULL DEFAULT 'user', dept_id BIGINT, status VARCHAR(32) NOT NULL DEFAULT 'active', extra_fields TEXT DEFAULT '{}', last_login_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_role (id BIGSERIAL PRIMARY KEY, name VARCHAR(128) NOT NULL, code VARCHAR(128) NOT NULL UNIQUE, description TEXT, created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_system_config (id BIGSERIAL PRIMARY KEY, config_key VARCHAR(128) NOT NULL UNIQUE, config_value TEXT NOT NULL, encrypted BOOLEAN NOT NULL DEFAULT FALSE, description TEXT, updated_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_workflow (id BIGSERIAL PRIMARY KEY, name VARCHAR(256) NOT NULL, model_name VARCHAR(128) NOT NULL, trigger VARCHAR(64) NOT NULL, config TEXT NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'active', created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_workflow_instance (id BIGSERIAL PRIMARY KEY, workflow_id BIGINT NOT NULL REFERENCES astraion_workflow(id), model_name VARCHAR(128) NOT NULL, record_id BIGINT NOT NULL, current_step VARCHAR(128), status VARCHAR(32) NOT NULL DEFAULT 'running', initiator_id BIGINT, started_at TIMESTAMP NOT NULL DEFAULT NOW(), finished_at TIMESTAMP)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_task (id BIGSERIAL PRIMARY KEY, workflow_inst_id BIGINT REFERENCES astraion_workflow_instance(id), model_name VARCHAR(128) NOT NULL, record_id BIGINT NOT NULL, step_id VARCHAR(128), title VARCHAR(512) NOT NULL, assignee_id BIGINT, status VARCHAR(32) NOT NULL DEFAULT 'pending', comment TEXT, due_at TIMESTAMP, completed_at TIMESTAMP, created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_document (id BIGSERIAL PRIMARY KEY, file_name VARCHAR(512) NOT NULL, file_url VARCHAR(1024) NOT NULL, file_size BIGINT, mime_type VARCHAR(128), model_name VARCHAR(128), record_id BIGINT, field_name VARCHAR(128), version INTEGER NOT NULL DEFAULT 1, uploaded_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_notification (id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL, title VARCHAR(256) NOT NULL, content TEXT, type VARCHAR(64), ref_model VARCHAR(128), ref_record_id BIGINT, read_flag BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_audit_log (id BIGSERIAL PRIMARY KEY, user_id BIGINT, action VARCHAR(64) NOT NULL, model_name VARCHAR(128), record_id BIGINT, changes TEXT, ip_address VARCHAR(64), created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_plugin (id BIGSERIAL PRIMARY KEY, plugin_name VARCHAR(128) NOT NULL UNIQUE, display_name VARCHAR(256), version VARCHAR(64), jar_path VARCHAR(1024), hook_points TEXT DEFAULT '[]', status VARCHAR(32) NOT NULL DEFAULT 'active', config TEXT DEFAULT '{}', created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_datasource (id BIGSERIAL PRIMARY KEY, name VARCHAR(128) NOT NULL, db_type VARCHAR(32) NOT NULL, host VARCHAR(256) NOT NULL, port INTEGER NOT NULL, database_name VARCHAR(128) NOT NULL, username VARCHAR(128) NOT NULL, password_enc VARCHAR(512) NOT NULL, mode VARCHAR(16) NOT NULL DEFAULT 'readonly', status VARCHAR(32) NOT NULL DEFAULT 'active', created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        // v2 agent
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_agent (id BIGSERIAL PRIMARY KEY, name VARCHAR(256) NOT NULL, agent_code VARCHAR(128) NOT NULL UNIQUE, model_override VARCHAR(128), system_prompt TEXT NOT NULL, personality VARCHAR(64), expertise TEXT DEFAULT '[]', tool_filter TEXT DEFAULT '[]', knowledge_bases TEXT DEFAULT '[]', schedule_tasks TEXT DEFAULT '[]', auto_reply BOOLEAN DEFAULT FALSE, collaboration VARCHAR(64) DEFAULT 'standalone', trigger_words TEXT DEFAULT '[]', priority INTEGER DEFAULT 5, status VARCHAR(32) DEFAULT 'inactive', created_by BIGINT, created_at TIMESTAMP NOT NULL DEFAULT NOW(), updated_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_agent_session (id BIGSERIAL PRIMARY KEY, agent_id BIGINT NOT NULL, user_id BIGINT, title VARCHAR(512), status VARCHAR(32) DEFAULT 'active', created_at TIMESTAMP NOT NULL DEFAULT NOW())");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS astraion_agent_message (id BIGSERIAL PRIMARY KEY, from_agent_id BIGINT, to_agent_id BIGINT, content TEXT NOT NULL, message_type VARCHAR(64), task_id VARCHAR(128), created_at TIMESTAMP NOT NULL DEFAULT NOW())");
    }

    /**
     * 初始化系统配置
     * 数据库已在 application.yml 中配好 PostgreSQL，直接写入配置
     */
    private void initSystemConfig() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM astraion_system_config", Integer.class);
        if (count != null && count > 0) return;

        // 写入 DB 配置条目（运行时从 application.yml 同步）
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.type', 'postgresql', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.host', 'localhost', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.port', '5432', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.name', 'astraion', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.user', 'postgres', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.password', '', true)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.initialized', 'true', false)");

        // 写入空占位，在初始化向导中填充

        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.provider', '', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.model', '', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.base_url', '', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.api_key', '', true)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.temperature', '', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('ai.max_tokens', '', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('system.initialized', 'false', false)");
        jdbcTemplate.update("INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.initialized', 'true', false)");
    }

    private void ensureDbInitialized() {
        // 确保 db.initialized 存在（兼容旧数据）
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM astraion_system_config WHERE config_key='db.initialized'",
            Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.update(
                "INSERT INTO astraion_system_config (config_key, config_value, encrypted) VALUES ('db.initialized', 'true', false)"
            );
        }
    }

    private void initRootUser() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM astraion_user WHERE username='ASTRAION'", Integer.class);
        if (count != null && count > 0) return;

        jdbcTemplate.update("INSERT INTO astraion_user (username, password_hash, display_name, role, status) VALUES ('ASTRAION', 'ASTRAION', 'ASTRAION Root', 'root', 'active')");
    }
}
