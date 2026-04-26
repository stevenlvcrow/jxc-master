package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictFieldBindingDO;

/**
 * 字典字段绑定 Mapper。
 */
public interface DictFieldBindingMapper extends BaseMapper<DictFieldBindingDO> {

    Long countReferences(@Param("tableName") String tableName, @Param("columnName") String columnName, @Param("itemCode") String itemCode);

    void migrateReferences(@Param("tableName") String tableName,
                           @Param("columnName") String columnName,
                           @Param("oldCode") String oldCode,
                           @Param("newCode") String newCode);
}
