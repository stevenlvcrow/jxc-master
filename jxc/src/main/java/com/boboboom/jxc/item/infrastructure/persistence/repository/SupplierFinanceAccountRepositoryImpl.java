package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierFinanceAccountRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierFinanceAccountDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierFinanceAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class SupplierFinanceAccountRepositoryImpl implements SupplierFinanceAccountRepository {

    private final SupplierFinanceAccountMapper supplierFinanceAccountMapper;

    public SupplierFinanceAccountRepositoryImpl(SupplierFinanceAccountMapper supplierFinanceAccountMapper) {
        this.supplierFinanceAccountMapper = supplierFinanceAccountMapper;
    }

    @Override
    public List<SupplierFinanceAccountDO> findBySupplierIdOrdered(Long supplierId) {
        if (supplierId == null) {
            return Collections.emptyList();
        }
        return supplierFinanceAccountMapper.selectList(new LambdaQueryWrapper<SupplierFinanceAccountDO>()
                .eq(SupplierFinanceAccountDO::getSupplierId, supplierId)
                .orderByAsc(SupplierFinanceAccountDO::getSortNo)
                .orderByAsc(SupplierFinanceAccountDO::getId));
    }

    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierFinanceAccountMapper.delete(new LambdaQueryWrapper<SupplierFinanceAccountDO>()
                .eq(SupplierFinanceAccountDO::getSupplierId, supplierId));
    }

    @Override
    public void save(SupplierFinanceAccountDO financeAccount) {
        supplierFinanceAccountMapper.insert(financeAccount);
    }
}
