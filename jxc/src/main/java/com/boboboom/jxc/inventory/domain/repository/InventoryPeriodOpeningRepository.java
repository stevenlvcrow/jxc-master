package com.boboboom.jxc.inventory.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningLineDO;

/** 周期期初库存仓储接口。 */
public interface InventoryPeriodOpeningRepository {

    List<InventoryPeriodOpeningDO> findByScopeOrdered(String scopeType, Long scopeId);

    Optional<InventoryPeriodOpeningDO> findByScopeAndId(String scopeType, Long scopeId, Long id);

    Optional<InventoryPeriodOpeningDO> findByPeriod(String scopeType,
                                                    Long scopeId,
                                                    String warehouseName,
                                                    String periodType,
                                                    LocalDate periodStartDate);

    List<InventoryPeriodOpeningLineDO> findLinesByHeaderId(Long headerId);

    List<InventoryPeriodOpeningLineDO> findLinesByHeaderIds(List<Long> headerIds);

    void saveHeader(InventoryPeriodOpeningDO header);

    void updateHeader(InventoryPeriodOpeningDO header);

    void deleteHeaderById(Long id);

    void deleteLinesByHeaderId(Long headerId);

    void saveLine(InventoryPeriodOpeningLineDO line);
}
