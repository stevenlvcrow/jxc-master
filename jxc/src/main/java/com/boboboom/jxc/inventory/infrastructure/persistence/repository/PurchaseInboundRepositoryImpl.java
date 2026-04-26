package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundMapper;

/** 库存仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class PurchaseInboundRepositoryImpl implements PurchaseInboundRepository {

    private final PurchaseInboundMapper purchaseInboundMapper;

    /** 库存仓储实现，负责通过持久层组件完成数据读写。 */
    public PurchaseInboundRepositoryImpl(PurchaseInboundMapper purchaseInboundMapperValue) {
        this.purchaseInboundMapper = purchaseInboundMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<PurchaseInboundDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return purchaseInboundMapper.selectList(new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .orderByDesc(PurchaseInboundDO::getCreatedAt)
                .orderByDesc(PurchaseInboundDO::getId));
    }

    /** 查询By作用域And标识。 */
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
            query.and(wrapper -> wrapper
                    .eq(PurchaseInboundDO::getCreatedBy, createdBy)
                    .or()
                    .eq(PurchaseInboundDO::getSalesmanUserId, createdBy));
        }
        return purchaseInboundMapper.selectList(query);
    }

    /** 查询By作用域And标识。 */
    @Override
    public Optional<PurchaseInboundDO> findByScopeAndId(String scopeType, Long scopeId, Long createdBy, boolean viewAll, Long id) {
        if (id == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<PurchaseInboundDO> query = new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .eq(PurchaseInboundDO::getId, id)
                .orderByDesc(PurchaseInboundDO::getId);
        if (!viewAll) {
            query.and(wrapper -> wrapper
                    .eq(PurchaseInboundDO::getCreatedBy, createdBy)
                    .or()
                    .eq(PurchaseInboundDO::getSalesmanUserId, createdBy));
        }
        return purchaseInboundMapper.selectList(query)
                .stream()
                .findFirst();
    }

    /** 统计By作用域AndDocument编码数量。 */
    @Override
    public Long countByScopeAndDocumentCode(String scopeType, Long scopeId, String documentCode) {
        return purchaseInboundMapper.selectCount(new LambdaQueryWrapper<PurchaseInboundDO>()
                .eq(PurchaseInboundDO::getScopeType, scopeType)
                .eq(PurchaseInboundDO::getScopeId, scopeId)
                .eq(PurchaseInboundDO::getDocumentCode, documentCode));
    }

    /** 保存业务数据。 */
    @Override
    public void save(PurchaseInboundDO header) {
        purchaseInboundMapper.insert(header);
    }

    /** 更新业务记录。 */
    @Override
    public void update(PurchaseInboundDO header) {
        purchaseInboundMapper.updateById(header);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        purchaseInboundMapper.deleteById(id);
    }
}
