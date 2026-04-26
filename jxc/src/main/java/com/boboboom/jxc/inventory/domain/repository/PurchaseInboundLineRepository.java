package com.boboboom.jxc.inventory.domain.repository;

import java.util.List;

import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;

/** 库存仓储接口，定义领域需要的数据访问能力。 */
public interface PurchaseInboundLineRepository {

    List<PurchaseInboundLineDO> findByInboundIds(List<Long> inboundIds);

    List<PurchaseInboundLineDO> findByInboundId(Long inboundId);

    void deleteByInboundId(Long inboundId);

    void save(PurchaseInboundLineDO line);
}
