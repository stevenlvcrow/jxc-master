package com.boboboom.jxc.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;

public interface PostgresMetadataMapper {

    void executeSql(@Param("sql") String sql);

    Boolean tableExists(@Param("schemaName") String schemaName,
                        @Param("tableName") String tableName);

    String currentSchema();
}
