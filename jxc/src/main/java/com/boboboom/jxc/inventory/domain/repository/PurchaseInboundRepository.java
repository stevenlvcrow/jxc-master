package com.boboboom.jxc.inventory.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;

/** 库存仓储接口，定义领域需要的数据访问能力。 */
public interface PurchaseInboundRepository {

    List<PurchaseInboundDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<PurchaseInboundDO> findByScopeAndIds(String scopeType, Long scopeId, Long createdBy, boolean viewAll, List<Long> ids);

    Optional<PurchaseInboundDO> findByScopeAndId(String scopeType, Long scopeId, Long createdBy, boolean viewAll, Long id);

    Long countByScopeAndDocumentCode(String scopeType, Long scopeId, String documentCode);

    void save(PurchaseInboundDO header);

    void update(PurchaseInboundDO header);

    int deleteById(Long id);
}
