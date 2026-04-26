package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningDO;

/** 周期期初库存 MyBatis Mapper。 */
public interface InventoryPeriodOpeningMapper extends BaseMapper<InventoryPeriodOpeningDO> {

    List<InventoryPeriodOpeningDO> selectByScopeOrdered(@Param("scopeType") String scopeType,
                                                        @Param("scopeId") Long scopeId);

    InventoryPeriodOpeningDO selectByScopeAndId(@Param("scopeType") String scopeType,
                                                @Param("scopeId") Long scopeId,
                                                @Param("id") Long id);

    InventoryPeriodOpeningDO selectByPeriod(@Param("scopeType") String scopeType,
                                            @Param("scopeId") Long scopeId,
                                            @Param("warehouseName") String warehouseName,
                                            @Param("periodType") String periodType,
                                            @Param("periodStartDate") LocalDate periodStartDate);
}
