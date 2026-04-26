package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundLineRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundLineDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundLineMapper;

/** 库存仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class PurchaseInboundLineRepositoryImpl implements PurchaseInboundLineRepository {

    private final PurchaseInboundLineMapper purchaseInboundLineMapper;

    /** 库存仓储实现，负责通过持久层组件完成数据读写。 */
    public PurchaseInboundLineRepositoryImpl(PurchaseInboundLineMapper purchaseInboundLineMapperValue) {
        this.purchaseInboundLineMapper = purchaseInboundLineMapperValue;
    }

    /** 查询By入库标识。 */
    @Override
    public List<PurchaseInboundLineDO> findByInboundIds(List<Long> inboundIds) {
        if (inboundIds == null || inboundIds.isEmpty()) {
            return Collections.emptyList();
        }
        return purchaseInboundLineMapper.selectList(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .in(PurchaseInboundLineDO::getInboundId, inboundIds));
    }

    /** 查询By入库标识。 */
    @Override
    public List<PurchaseInboundLineDO> findByInboundId(Long inboundId) {
        if (inboundId == null) {
            return Collections.emptyList();
        }
        return purchaseInboundLineMapper.selectList(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .eq(PurchaseInboundLineDO::getInboundId, inboundId)
                .orderByAsc(PurchaseInboundLineDO::getId));
    }

    /** 删除By入库标识。 */
    @Override
    public void deleteByInboundId(Long inboundId) {
        if (inboundId == null) {
            return;
        }
        purchaseInboundLineMapper.delete(new LambdaQueryWrapper<PurchaseInboundLineDO>()
                .eq(PurchaseInboundLineDO::getInboundId, inboundId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(PurchaseInboundLineDO line) {
        purchaseInboundLineMapper.insert(line);
    }
}
