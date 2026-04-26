package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierCategoryMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class SupplierCategoryRepositoryImpl implements SupplierCategoryRepository {

    private final SupplierCategoryMapper supplierCategoryMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public SupplierCategoryRepositoryImpl(SupplierCategoryMapper supplierCategoryMapperValue) {
        this.supplierCategoryMapper = supplierCategoryMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<SupplierCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return supplierCategoryMapper.selectList(new LambdaQueryWrapper<SupplierCategoryDO>()
                .eq(SupplierCategoryDO::getScopeType, scopeType)
                .eq(SupplierCategoryDO::getScopeId, scopeId)
                .orderByAsc(SupplierCategoryDO::getCreatedAt)
                .orderByAsc(SupplierCategoryDO::getId));
    }

    /** 查询By作用域And名称。 */
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

    /** 查询By作用域And编码。 */
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

    /** 保存业务数据。 */
    @Override
    public void save(SupplierCategoryDO category) {
        supplierCategoryMapper.insert(category);
    }
}
