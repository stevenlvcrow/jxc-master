package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierFinanceAccountRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierFinanceAccountDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierFinanceAccountMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class SupplierFinanceAccountRepositoryImpl implements SupplierFinanceAccountRepository {

    private final SupplierFinanceAccountMapper supplierFinanceAccountMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public SupplierFinanceAccountRepositoryImpl(SupplierFinanceAccountMapper supplierFinanceAccountMapperValue) {
        this.supplierFinanceAccountMapper = supplierFinanceAccountMapperValue;
    }

    /** 查询By供应商标识排序。 */
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

    /** 删除By供应商标识。 */
    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierFinanceAccountMapper.delete(new LambdaQueryWrapper<SupplierFinanceAccountDO>()
                .eq(SupplierFinanceAccountDO::getSupplierId, supplierId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(SupplierFinanceAccountDO financeAccount) {
        supplierFinanceAccountMapper.insert(financeAccount);
    }
}
