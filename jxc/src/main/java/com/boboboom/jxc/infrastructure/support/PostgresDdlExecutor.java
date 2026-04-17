package com.boboboom.jxc.infrastructure.support;

import com.boboboom.jxc.infrastructure.persistence.mapper.PostgresMetadataMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostgresDdlExecutor {

    private static final Logger log = LoggerFactory.getLogger(PostgresDdlExecutor.class);

    private final PostgresMetadataMapper postgresMetadataMapper;

    public PostgresDdlExecutor(PostgresMetadataMapper postgresMetadataMapper) {
        this.postgresMetadataMapper = postgresMetadataMapper;
    }

    public void execute(String sql) {
        log.info("Executing DDL: {}", sql);
        postgresMetadataMapper.executeSql(sql);
    }

    public void executeBatch(List<String> sqlList) {
        for (String sql : sqlList) {
            execute(sql);
        }
    }

    public boolean tableExists(String tableName) {
        TableRef tableRef = parseTableRef(tableName);
        if (tableRef == null) {
            return false;
        }
        Boolean exists = postgresMetadataMapper.tableExists(
                tableRef.schemaName(),
                tableRef.tableName()
        );
        return Boolean.TRUE.equals(exists);
    }

    public String currentSchema() {
        return postgresMetadataMapper.currentSchema();
    }

    private TableRef parseTableRef(String tableName) {
        if (tableName == null) {
            return null;
        }
        String normalized = tableName.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String[] segments = normalized.split("\\.", 2);
        if (segments.length == 2) {
            String schemaName = stripIdentifierQuotes(segments[0]);
            String actualTableName = stripIdentifierQuotes(segments[1]);
            if (schemaName.isEmpty() || actualTableName.isEmpty()) {
                return null;
            }
            return new TableRef(schemaName, actualTableName);
        }
        String actualTableName = stripIdentifierQuotes(normalized);
        if (actualTableName.isEmpty()) {
            return null;
        }
        return new TableRef(currentSchema(), actualTableName);
    }

    private String stripIdentifierQuotes(String value) {
        return value == null ? "" : value.trim().replace("\"", "");
    }

    private record TableRef(String schemaName, String tableName) {
    }
}

