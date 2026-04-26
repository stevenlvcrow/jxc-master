package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.SupplierQualificationRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierQualificationDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierQualificationMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class SupplierQualificationRepositoryImpl implements SupplierQualificationRepository {

    private final SupplierQualificationMapper supplierQualificationMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public SupplierQualificationRepositoryImpl(SupplierQualificationMapper supplierQualificationMapperValue) {
        this.supplierQualificationMapper = supplierQualificationMapperValue;
    }

    /** 查询By供应商标识排序。 */
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

    /** 删除By供应商标识。 */
    @Override
    public void deleteBySupplierId(Long supplierId) {
        if (supplierId == null) {
            return;
        }
        supplierQualificationMapper.delete(new LambdaQueryWrapper<SupplierQualificationDO>()
                .eq(SupplierQualificationDO::getSupplierId, supplierId));
    }

    /** 保存业务数据。 */
    @Override
    public void save(SupplierQualificationDO qualification) {
        supplierQualificationMapper.insert(qualification);
    }
}
