package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;

import java.util.List;
import java.util.Optional;

public interface WarehouseRepository {

    Optional<WarehouseDO> findById(Long id);

    Optional<WarehouseDO> findByWarehouseCode(String warehouseCode);

    List<WarehouseDO> findAllOrdered();

    List<WarehouseDO> findByGroupId(Long groupId);

    List<WarehouseDO> findByStoreId(Long storeId);

    List<String> findAllWarehouseCodes();

    void save(WarehouseDO warehouse);

    void update(WarehouseDO warehouse);

    void resetDefaultByStoreId(Long storeId);

    void deleteById(Long id);
}
