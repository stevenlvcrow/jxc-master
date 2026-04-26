package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierContractRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierContractMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class SupplierContractRepositoryImpl implements SupplierContractRepository {

    private final SupplierContractMapper supplierContractMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public SupplierContractRepositoryImpl(SupplierContractMapper supplierContractMapperValue) {
        this.supplierContractMapper = supplierContractMapperValue;
    }

    /** 查询By供应商标识排序。 */
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

    /** 删除By供应商标识。 */
    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierContractMapper.delete(new LambdaQueryWrapper<SupplierContractDO>()
                .eq(SupplierContractDO::getSupplierId, supplierId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(SupplierContractDO contract) {
        supplierContractMapper.insert(contract);
    }
}
