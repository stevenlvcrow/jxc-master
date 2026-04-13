package com.boboboom.jxc.infrastructure.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

@Component
public class PostgresDdlExecutor {

    private static final Logger log = LoggerFactory.getLogger(PostgresDdlExecutor.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public PostgresDdlExecutor(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    public void execute(String sql) {
        log.info("Executing DDL: {}", sql);
        jdbcTemplate.execute(sql);
    }

    public void executeBatch(List<String> sqlList) {
        for (String sql : sqlList) {
            execute(sql);
        }
    }

    public boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (var resultSet = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect table metadata", ex);
        }
    }

    public String currentSchema() {
        return jdbcTemplate.queryForObject("select current_schema()", String.class);
    }
}

