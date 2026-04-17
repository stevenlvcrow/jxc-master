package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final WarehouseMapper warehouseMapper;

    public WarehouseRepositoryImpl(WarehouseMapper warehouseMapper) {
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public Optional<WarehouseDO> findById(Long id) {
        return Optional.ofNullable(warehouseMapper.selectById(id));
    }
}
