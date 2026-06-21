package com.hp.aiagent.maintenance.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@Slf4j
@ConditionalOnProperty(name = "app.persistence.jdbc.enabled", havingValue = "true")
public class MaintenanceDatabaseInitializer {

    public MaintenanceDatabaseInitializer(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/maintenance_schema.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("sql/maintenance_seed.sql"));
            log.info("Maintenance database schema and seed data initialized");
        } catch (Exception ex) {
            throw new IllegalStateException("检修业务数据库初始化失败：" + ex.getMessage(), ex);
        }
    }
}
