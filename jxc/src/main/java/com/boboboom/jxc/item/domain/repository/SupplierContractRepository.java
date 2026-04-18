package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierContractDO;

import java.util.List;

public interface SupplierContractRepository {

    List<SupplierContractDO> findBySupplierIdOrdered(Long supplierId);

    void deleteBySupplierId(Long supplierId);

    void save(SupplierContractDO contract);
}
