package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.inventory.domain.repository.InventoryPeriodOpeningRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningLineDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryPeriodOpeningLineMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryPeriodOpeningMapper;

/** 周期期初库存仓储实现。 */
@Repository
public class InventoryPeriodOpeningRepositoryImpl implements InventoryPeriodOpeningRepository {

    private final InventoryPeriodOpeningMapper headerMapper;
    private final InventoryPeriodOpeningLineMapper lineMapper;

    /** 周期期初库存仓储实现。 */
    public InventoryPeriodOpeningRepositoryImpl(InventoryPeriodOpeningMapper headerMapperValue,
                                                InventoryPeriodOpeningLineMapper lineMapperValue) {
        this.headerMapper = headerMapperValue;
        this.lineMapper = lineMapperValue;
    }

    @Override
    public List<InventoryPeriodOpeningDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return headerMapper.selectByScopeOrdered(scopeType, scopeId);
    }

    @Override
    public Optional<InventoryPeriodOpeningDO> findByScopeAndId(String scopeType, Long scopeId, Long id) {
        return Optional.ofNullable(headerMapper.selectByScopeAndId(scopeType, scopeId, id));
    }

    @Override
    public Optional<InventoryPeriodOpeningDO> findByPeriod(String scopeType,
                                                           Long scopeId,
                                                           String warehouseName,
                                                           String periodType,
                                                           LocalDate periodStartDate) {
        return Optional.ofNullable(headerMapper.selectByPeriod(scopeType, scopeId, warehouseName, periodType, periodStartDate));
    }

    @Override
    public List<InventoryPeriodOpeningLineDO> findLinesByHeaderId(Long headerId) {
        return lineMapper.selectByHeaderId(headerId);
    }

    @Override
    public List<InventoryPeriodOpeningLineDO> findLinesByHeaderIds(List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return List.of();
        }
        return lineMapper.selectByHeaderIds(headerIds);
    }

    @Override
    public void saveHeader(InventoryPeriodOpeningDO header) {
        headerMapper.insert(header);
    }

    @Override
    public void updateHeader(InventoryPeriodOpeningDO header) {
        headerMapper.updateById(header);
    }

    @Override
    public void deleteHeaderById(Long id) {
        headerMapper.deleteById(id);
    }

    @Override
    public void deleteLinesByHeaderId(Long headerId) {
        lineMapper.deleteByHeaderId(headerId);
    }

    @Override
    public void saveLine(InventoryPeriodOpeningLineDO line) {
        lineMapper.insert(line);
    }
}
