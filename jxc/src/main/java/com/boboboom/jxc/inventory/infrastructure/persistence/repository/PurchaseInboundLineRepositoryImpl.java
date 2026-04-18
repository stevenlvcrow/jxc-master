package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundLineMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class PurchaseInboundLineRepositoryImpl implements PurchaseInboundLineRepository {

    private final PurchaseInboundLineMapper purchaseInboundLineMapper;

    public PurchaseInboundLineRepositoryImpl(PurchaseInboundLineMapper purchaseInboundLineMapper) {
        this.purchaseInboundLineMapper = purchaseInboundLineMapper;
    }

    @Override
    public List<PurchaseInboundLineDO> findByInboundIds(List<Long> inboundIds) {
        if (inboundIds == null || inboundIds.isEmpty()) {
            return Collections.emptyList();
        }
        return purchaseInboundLineMapper.selectList(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .in(PurchaseInboundLineDO::getInboundId, inboundIds));
    }

    @Override
    public List<PurchaseInboundLineDO> findByInboundId(Long inboundId) {
        if (inboundId == null) {
            return Collections.emptyList();
        }
        return purchaseInboundLineMapper.selectList(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .eq(PurchaseInboundLineDO::getInboundId, inboundId)
                .orderByAsc(PurchaseInboundLineDO::getId));
    }

    @Override
    public void deleteByInboundId(Long inboundId) {
        if (inboundId == null) {
            return;
        }
        purchaseInboundLineMapper.delete(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .eq(PurchaseInboundLineDO::getInboundId, inboundId));
    }

    @Override
    public void save(PurchaseInboundLineDO line) {
        purchaseInboundLineMapper.insert(line);
    }
}
