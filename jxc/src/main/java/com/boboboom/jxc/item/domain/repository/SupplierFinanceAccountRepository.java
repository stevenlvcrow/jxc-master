package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierFinanceAccountDO;

import java.util.List;

public interface SupplierFinanceAccountRepository {

    List<SupplierFinanceAccountDO> findBySupplierIdOrdered(Long supplierId);

    void deleteBySupplierId(Long supplierId);

    void save(SupplierFinanceAccountDO financeAccount);
}
