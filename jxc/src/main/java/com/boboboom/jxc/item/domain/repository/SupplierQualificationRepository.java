package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierQualificationDO;

import java.util.List;

public interface SupplierQualificationRepository {

    List<SupplierQualificationDO> findBySupplierIdOrdered(Long supplierId);

    void deleteBySupplierId(Long supplierId);

    void save(SupplierQualificationDO qualification);
}
