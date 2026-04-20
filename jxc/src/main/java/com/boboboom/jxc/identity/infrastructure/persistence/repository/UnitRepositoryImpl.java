package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UnitRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UnitMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UnitRepositoryImpl implements UnitRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final UnitMapper unitMapper;

    public UnitRepositoryImpl(UnitMapper unitMapper) {
        this.unitMapper = unitMapper;
    }

    @Override
    public List<UnitDO> findByScope(String scopeType, Long scopeId) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId)
                .orderByDesc(UnitDO::getCreatedAt)
                .orderByDesc(UnitDO::getId));
    }

    @Override
    public List<UnitDO> findPlatformTemplates() {
        return findByScope(PLATFORM_SCOPE, 0L);
    }

    @Override
    public List<UnitDO> findStoreRows(Long storeId) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, STORE_SCOPE)
                .eq(UnitDO::getScopeId, storeId)
                .select(UnitDO::getUnitCode, UnitDO::getUnitName));
    }

    @Override
    public Optional<UnitDO> findByIdAndScope(Long id, String scopeType, Long scopeId) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getId, id)
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId)
                .orderByDesc(UnitDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UnitDO> findByScopeAndUnitCode(String scopeType, Long scopeId, String unitCode) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId)
                .eq(UnitDO::getUnitCode, unitCode)
                .orderByDesc(UnitDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UnitDO> findByScopeAndUnitName(String scopeType, Long scopeId, String unitName) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId)
                .eq(UnitDO::getUnitName, unitName)
                .orderByDesc(UnitDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void save(UnitDO unit) {
        unitMapper.insert(unit);
    }

    @Override
    public void update(UnitDO unit) {
        unitMapper.updateById(unit);
    }

    @Override
    public void deleteByIdAndScope(Long id, String scopeType, Long scopeId) {
        unitMapper.delete(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getId, id)
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId));
    }
}
