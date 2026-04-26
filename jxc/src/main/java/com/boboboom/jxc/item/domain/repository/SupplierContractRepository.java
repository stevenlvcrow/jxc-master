package com.boboboom.jxc.item.domain.repository;

import java.util.List;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;

/** 物品与供应商仓储接口，定义领域需要的数据访问能力。 */
public interface SupplierContractRepository {

    List<SupplierContractDO> findBySupplierIdOrdered(Long supplierId);

    void deleteBySupplierId(Long supplierId);

    void save(SupplierContractDO contract);
}
