package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBatchBalanceDO;

/** 库存批次余额 Mapper，承载批次余额锁定查询。 */
public interface InventoryBatchBalanceMapper extends BaseMapper<InventoryBatchBalanceDO> {

    InventoryBatchBalanceDO selectForUpdate(@Param("scopeType") String scopeType,
                                            @Param("scopeId") Long scopeId,
                                            @Param("warehouseName") String warehouseName,
                                            @Param("itemCode") String itemCode,
                                            @Param("batchNo") String batchNo);
}
