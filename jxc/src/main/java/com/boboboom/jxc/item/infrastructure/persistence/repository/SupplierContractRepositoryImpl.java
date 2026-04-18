package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierContractRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierContractMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class SupplierContractRepositoryImpl implements SupplierContractRepository {

    private final SupplierContractMapper supplierContractMapper;

    public SupplierContractRepositoryImpl(SupplierContractMapper supplierContractMapper) {
        this.supplierContractMapper = supplierContractMapper;
    }

    @Override
    public List<SupplierContractDO> findBySupplierIdOrdered(Long supplierId) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        return supplierContractMapper.selectList(new LambdaQueryWrapper<SupplierContractDO>()
                .eq(SupplierContractDO::getSupplierId, supplierId)
                .orderByAsc(SupplierContractDO::getSortNo)
                .orderByAsc(SupplierContractDO::getId));
    }

    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierContractMapper.delete(new LambdaQueryWrapper<SupplierContractDO>()
                .eq(SupplierContractDO::getSupplierId, supplierId));
    }

    @Override
    public void save(SupplierContractDO contract) {
        supplierContractMapper.insert(contract);
    }
}
