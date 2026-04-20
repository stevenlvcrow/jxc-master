package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SupplierCategoryRepositoryImpl implements SupplierCategoryRepository {

    private final SupplierCategoryMapper supplierCategoryMapper;

    public SupplierCategoryRepositoryImpl(SupplierCategoryMapper supplierCategoryMapper) {
        this.supplierCategoryMapper = supplierCategoryMapper;
    }

    @Override
    public List<SupplierCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return supplierCategoryMapper.selectList(new LambdaQueryWrapper<SupplierCategoryDO>()
                .eq(SupplierCategoryDO::getScopeType, scopeType)
                .eq(SupplierCategoryDO::getScopeId, scopeId)
                .orderByAsc(SupplierCategoryDO::getCreatedAt)
                .orderByAsc(SupplierCategoryDO::getId));
    }

    @Override
    public Optional<SupplierCategoryDO> findByScopeAndName(String scopeType, Long scopeId, String categoryName) {
        return supplierCategoryMapper.selectList(new LambdaQueryWrapper<SupplierCategoryDO>()
                .eq(SupplierCategoryDO::getScopeType, scopeType)
                .eq(SupplierCategoryDO::getScopeId, scopeId)
                .eq(SupplierCategoryDO::getCategoryName, categoryName)
                .orderByDesc(SupplierCategoryDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<SupplierCategoryDO> findByScopeAndCode(String scopeType, Long scopeId, String categoryCode) {
        return supplierCategoryMapper.selectList(new LambdaQueryWrapper<SupplierCategoryDO>()
                .eq(SupplierCategoryDO::getScopeType, scopeType)
                .eq(SupplierCategoryDO::getScopeId, scopeId)
                .eq(SupplierCategoryDO::getCategoryCode, categoryCode)
                .orderByDesc(SupplierCategoryDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void save(SupplierCategoryDO category) {
        supplierCategoryMapper.insert(category);
    }
}
