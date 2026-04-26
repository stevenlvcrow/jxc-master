package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryBalanceDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryTransactionDO;

/** 库存 MyBatis Mapper，承载数据库映射访问能力。 */
public interface InventoryTransactionMapper extends BaseMapper<InventoryTransactionDO> {

    List<InventoryTransactionDO> selectByScopeOrdered(@Param("scopeType") String scopeType,
                                                      @Param("scopeId") Long scopeId);

    List<InventoryTransactionDO> selectByScopeAndCreatedAtRange(@Param("scopeType") String scopeType,
                                                                @Param("scopeId") Long scopeId,
                                                                @Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    List<InventoryTransactionDO> selectByScopeAndBusinessDateRange(@Param("scopeType") String scopeType,
                                                                   @Param("scopeId") Long scopeId,
                                                                   @Param("startDate") LocalDate startDate,
                                                                   @Param("endDate") LocalDate endDate);

    Long countByScopeWarehouseAndBusinessDateOnOrAfter(@Param("scopeType") String scopeType,
                                                       @Param("scopeId") Long scopeId,
                                                       @Param("warehouseName") String warehouseName,
                                                       @Param("businessDate") LocalDate businessDate);

    Long countByScopeAndBizType(@Param("scopeType") String scopeType,
                                @Param("scopeId") Long scopeId,
                                @Param("bizType") String bizType);

    List<InventoryBalanceDO> selectLatestBalancesBefore(@Param("scopeType") String scopeType,
                                                        @Param("scopeId") Long scopeId,
                                                        @Param("warehouseName") String warehouseName,
                                                        @Param("cutoff") LocalDateTime cutoff);
}

