package com.boboboom.jxc.inventory.domain.repository;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;

import java.util.List;

public interface PurchaseInboundLineRepository {

    List<PurchaseInboundLineDO> findByInboundIds(List<Long> inboundIds);

    List<PurchaseInboundLineDO> findByInboundId(Long inboundId);

    void deleteByInboundId(Long inboundId);

    void save(PurchaseInboundLineDO line);
}
