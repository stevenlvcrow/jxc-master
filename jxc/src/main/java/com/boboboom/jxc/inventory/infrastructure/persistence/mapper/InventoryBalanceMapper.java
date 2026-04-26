package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;

/** 库存 MyBatis Mapper，承载数据库映射访问能力。 */
public interface InventoryBalanceMapper extends BaseMapper<InventoryBalanceDO> {

    InventoryBalanceDO selectForUpdate(@Param("scopeType") String scopeType,
                                       @Param("scopeId") Long scopeId,
                                       @Param("warehouseName") String warehouseName,
                                       @Param("itemCode") String itemCode);
}
