package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
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

    @Override
    public Optional<WarehouseDO> findByWarehouseCode(String warehouseCode) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getWarehouseCode, warehouseCode)
                .orderByDesc(WarehouseDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public List<WarehouseDO> findAllOrdered() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .orderByDesc(WarehouseDO::getCreatedAt)
                .orderByDesc(WarehouseDO::getId));
    }

    @Override
    public List<WarehouseDO> findByGroupId(Long groupId) {
        if (groupId == null) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getGroupId, groupId)
                .orderByDesc(WarehouseDO::getCreatedAt)
                .orderByDesc(WarehouseDO::getId));
    }

    @Override
    public List<WarehouseDO> findByStoreId(Long storeId) {
        if (storeId == null) {
            return Collections.emptyList();
        }
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getStoreId, storeId)
                .orderByDesc(WarehouseDO::getCreatedAt)
                .orderByDesc(WarehouseDO::getId));
    }

    @Override
    public List<String> findAllWarehouseCodes() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                        .select(WarehouseDO::getWarehouseCode))
                .stream()
                .map(WarehouseDO::getWarehouseCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    @Override
    public void save(WarehouseDO warehouse) {
        warehouseMapper.insert(warehouse);
    }

    @Override
    public void update(WarehouseDO warehouse) {
        warehouseMapper.updateById(warehouse);
    }

    @Override
    public void resetDefaultByStoreId(Long storeId) {
        if (storeId == null) {
            return;
        }
        WarehouseDO resetDefault = new WarehouseDO();
        resetDefault.setIsDefault(false);
        warehouseMapper.update(resetDefault, new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getStoreId, storeId));
    }

    @Override
    public void deleteById(Long id) {
        warehouseMapper.deleteById(id);
    }
}
