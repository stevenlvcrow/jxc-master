package com.boboboom.jxc.inventory.domain.repository;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;

import java.util.List;
import java.util.Optional;

public interface PurchaseInboundRepository {

    List<PurchaseInboundDO> findByScopeOrdered(String scopeType, Long scopeId);

    List<PurchaseInboundDO> findByScopeAndIds(String scopeType, Long scopeId, Long createdBy, boolean viewAll, List<Long> ids);

    Optional<PurchaseInboundDO> findByScopeAndId(String scopeType, Long scopeId, Long createdBy, boolean viewAll, Long id);

    Long countByScopeAndDocumentCode(String scopeType, Long scopeId, String documentCode);

    void save(PurchaseInboundDO header);

    void update(PurchaseInboundDO header);

    void deleteById(Long id);
}
