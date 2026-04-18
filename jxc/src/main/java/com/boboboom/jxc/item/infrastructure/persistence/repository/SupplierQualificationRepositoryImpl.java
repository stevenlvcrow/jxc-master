package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierQualificationRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierQualificationDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierQualificationMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class SupplierQualificationRepositoryImpl implements SupplierQualificationRepository {

    private final SupplierQualificationMapper supplierQualificationMapper;

    public SupplierQualificationRepositoryImpl(SupplierQualificationMapper supplierQualificationMapper) {
        this.supplierQualificationMapper = supplierQualificationMapper;
    }

    @Override
    public List<SupplierQualificationDO> findBySupplierIdOrdered(Long supplierId) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        return supplierQualificationMapper.selectList(new LambdaQueryWrapper<SupplierQualificationDO>()
                .eq(SupplierQualificationDO::getSupplierId, supplierId)
                .orderByAsc(SupplierQualificationDO::getSortNo)
                .orderByAsc(SupplierQualificationDO::getId));
    }

    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierQualificationMapper.delete(new LambdaQueryWrapper<SupplierQualificationDO>()
                .eq(SupplierQualificationDO::getSupplierId, supplierId));
    }

    @Override
    public void save(SupplierQualificationDO qualification) {
        supplierQualificationMapper.insert(qualification);
    }
}
