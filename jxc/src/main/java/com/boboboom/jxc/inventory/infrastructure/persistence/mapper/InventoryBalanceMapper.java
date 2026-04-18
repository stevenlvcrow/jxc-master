package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface InventoryBalanceMapper extends BaseMapper<InventoryBalanceDO> {

    @Select("""
            SELECT id, scope_type, scope_id, warehouse_name, item_code, item_name, quantity, created_at, updated_at
            FROM dev.inventory_balance
            WHERE scope_type = #{scopeType}
              AND scope_id = #{scopeId}
              AND warehouse_name = #{warehouseName}
              AND item_code = #{itemCode}
            FOR UPDATE
            """)
    InventoryBalanceDO selectForUpdate(@Param("scopeType") String scopeType,
                                       @Param("scopeId") Long scopeId,
                                       @Param("warehouseName") String warehouseName,
                                       @Param("itemCode") String itemCode);
}
