package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UnitRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UnitMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class UnitRepositoryImpl implements UnitRepository {

    private static final String PLATFORM_SCOPE = "PLATFORM";
    private static final String STORE_SCOPE = "STORE";

    private final UnitMapper unitMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public UnitRepositoryImpl(UnitMapper unitMapperValue) {
        this.unitMapper = unitMapperValue;
    }

    /** 查询By作用域。 */
    @Override
    public List<UnitDO> findByScope(String scopeType, Long scopeId) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId)
                .orderByDesc(UnitDO::getCreatedAt)
                .orderByDesc(UnitDO::getId));
    }

    /** 查询PlatformTemplates。 */
    @Override
    public List<UnitDO> findPlatformTemplates() {
        return findByScope(PLATFORM_SCOPE, 0L);
    }

    /** 查询门店Rows。 */
    @Override
    public List<UnitDO> findStoreRows(Long storeId) {
        return unitMapper.selectList(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getScopeType, STORE_SCOPE)
                .eq(UnitDO::getScopeId, storeId)
                .select(UnitDO::getUnitCode, UnitDO::getUnitName));
    }

    /** 查询By标识And作用域。 */
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

    /** 查询By作用域And单位编码。 */
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

    /** 查询By作用域And单位名称。 */
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

    /** 保存业务数据。 */
    @Override
    public void save(UnitDO unit) {
        unitMapper.insert(unit);
    }

    /** 更新业务记录。 */
    @Override
    public void update(UnitDO unit) {
        unitMapper.updateById(unit);
    }

    /** 删除By标识And作用域。 */
    @Override
    public void deleteByIdAndScope(Long id, String scopeType, Long scopeId) {
        unitMapper.delete(new LambdaQueryWrapper<UnitDO>()
                .eq(UnitDO::getId, id)
                .eq(UnitDO::getScopeType, scopeType)
                .eq(UnitDO::getScopeId, scopeId));
    }
}
