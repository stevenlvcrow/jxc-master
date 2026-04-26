package com.boboboom.jxc.infrastructure.support;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.boboboom.jxc.infrastructure.persistence.mapper.PostgresMetadataMapper;

/** PostgreSQL DDL 执行器，负责初始化阶段执行结构变更。 */
@Component
public class PostgresDdlExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PostgresDdlExecutor.class);

    private final PostgresMetadataMapper postgresMetadataMapper;

    /** PostgreSQL DDL 执行器，负责初始化阶段执行结构变更。 */
    public PostgresDdlExecutor(PostgresMetadataMapper postgresMetadataMapperValue) {
        this.postgresMetadataMapper = postgresMetadataMapperValue;
    }

    /** 执行数据库结构变更语句。 */
    public void execute(String sql) {
        LOG.info("Executing DDL: {}", sql);
        postgresMetadataMapper.executeSql(sql);
    }

    /** 批量执行数据库结构变更语句。 */
    public void executeBatch(List<String> sqlList) {
        for (String sql : sqlList) {
            execute(sql);
        }
    }

    /** 检查当前 schema 下数据表是否存在。 */
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

    /** 查询当前数据库 schema。 */
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

