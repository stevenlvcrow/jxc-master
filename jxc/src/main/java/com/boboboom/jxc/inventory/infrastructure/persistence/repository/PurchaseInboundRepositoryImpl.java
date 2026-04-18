package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class PurchaseInboundRepositoryImpl implements PurchaseInboundRepository {

    private final PurchaseInboundMapper purchaseInboundMapper;

    public PurchaseInboundRepositoryImpl(PurchaseInboundMapper purchaseInboundMapper) {
        this.purchaseInboundMapper = purchaseInboundMapper;
    }

    @Override
    public List<PurchaseInboundDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return purchaseInboundMapper.selectList(new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .orderByDesc(PurchaseInboundDO::getCreatedAt)
                .orderByDesc(PurchaseInboundDO::getId));
    }

    @Override
    public List<PurchaseInboundDO> findByScopeAndIds(String scopeType, Long scopeId, Long createdBy, boolean viewAll, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<PurchaseInboundDO> query = new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .in(PurchaseInboundDO::getId, ids);
        if (!viewAll) {
            query.eq(PurchaseInboundDO::getCreatedBy, createdBy);
        }
        return purchaseInboundMapper.selectList(query);
    }

    @Override
    public Optional<PurchaseInboundDO> findByScopeAndId(String scopeType, Long scopeId, Long createdBy, boolean viewAll, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<PurchaseInboundDO> query = new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .eq(PurchaseInboundDO::getId, id)
                .last("limit 1");
        if (!viewAll) {
            query.eq(PurchaseInboundDO::getCreatedBy, createdBy);
        }
        return Optional.ofNullable(purchaseInboundMapper.selectOne(query));
    }

    @Override
    public Long countByScopeAndDocumentCode(String scopeType, Long scopeId, String documentCode) {
        return purchaseInboundMapper.selectCount(new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .eq(PurchaseInboundDO::getDocumentCode, documentCode));
    }

    @Override
    public void save(PurchaseInboundDO header) {
        purchaseInboundMapper.insert(header);
    }

    @Override
    public void update(PurchaseInboundDO header) {
        purchaseInboundMapper.updateById(header);
    }

    @Override
    public void deleteById(Long id) {
        purchaseInboundMapper.deleteById(id);
    }
}
