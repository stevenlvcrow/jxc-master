package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierProfileMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class SupplierProfileRepositoryImpl implements SupplierProfileRepository {

    private final SupplierProfileMapper supplierProfileMapper;

    public SupplierProfileRepositoryImpl(SupplierProfileMapper supplierProfileMapper) {
        this.supplierProfileMapper = supplierProfileMapper;
    }

    @Override
    public List<SupplierProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return supplierProfileMapper.selectList(new LambdaQueryWrapper<SupplierProfileDO>()
                .eq(SupplierProfileDO::getScopeType, scopeType)
                .eq(SupplierProfileDO::getScopeId, scopeId)
                .orderByDesc(SupplierProfileDO::getUpdatedAt)
                .orderByDesc(SupplierProfileDO::getId));
    }

    @Override
    public Optional<SupplierProfileDO> findById(Long id) {
        return Optional.ofNullable(supplierProfileMapper.selectById(id));
    }

    @Override
    public void save(SupplierProfileDO profile) {
        supplierProfileMapper.insert(profile);
    }

    @Override
    public void update(SupplierProfileDO profile) {
        supplierProfileMapper.updateById(profile);
    }
}
