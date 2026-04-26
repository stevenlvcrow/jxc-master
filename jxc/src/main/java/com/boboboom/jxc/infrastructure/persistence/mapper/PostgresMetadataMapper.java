package com.boboboom.jxc.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;

/** 基础设施 MyBatis Mapper，承载数据库映射访问能力。 */
public interface PostgresMetadataMapper {

    void executeSql(@Param("sql") String sql);

    Boolean tableExists(@Param("schemaName") String schemaName,
                        @Param("tableName") String tableName);

    String currentSchema();
}
