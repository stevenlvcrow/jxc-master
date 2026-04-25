package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierProfileMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class SupplierProfileRepositoryImpl implements SupplierProfileRepository {

    private final SupplierProfileMapper supplierProfileMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public SupplierProfileRepositoryImpl(SupplierProfileMapper supplierProfileMapperValue) {
        this.supplierProfileMapper = supplierProfileMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<SupplierProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfileDO>()
                .eq(SupplierProfileDO::getScopeType, scopeType)
                .eq(SupplierProfileDO::getScopeId, scopeId)
                .orderByDesc(SupplierProfileDO::getUpdatedAt)
                .orderByDesc(SupplierProfileDO::getId));
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<SupplierProfileDO> findById(Long id) {
        return Optional.ofNullable(supplierProfileMapper.selectById(id));
    }

    /** 保存业务数据。 */
    @Override
    public void save(SupplierProfileDO profile) {
        supplierProfileMapper.insert(profile);
    }

    /** 更新业务记录。 */
    @Override
    public void update(SupplierProfileDO profile) {
        supplierProfileMapper.updateById(profile);
    }
}
