package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.WarehouseRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class WarehouseRepositoryImpl implements WarehouseRepository {

    private final WarehouseMapper warehouseMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public WarehouseRepositoryImpl(WarehouseMapper warehouseMapperValue) {
        this.warehouseMapper = warehouseMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<WarehouseDO> findById(Long id) {
        return Optional.ofNullable(warehouseMapper.selectById(id));
    }

    /** 查询By仓库编码。 */
    @Override
    public Optional<WarehouseDO> findByWarehouseCode(String warehouseCode) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .eq(WarehouseDO::getWarehouseCode, warehouseCode)
                .orderByDesc(WarehouseDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询All排序。 */
    @Override
    public List<WarehouseDO> findAllOrdered() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                .orderByDesc(WarehouseDO::getCreatedAt)
                .orderByDesc(WarehouseDO::getId));
    }

    /** 查询By集团标识。 */
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

    /** 查询By门店标识。 */
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

    /** 查询All仓库Codes。 */
    @Override
    public List<String> findAllWarehouseCodes() {
        return warehouseMapper.selectList(new LambdaQueryWrapper<WarehouseDO>()
                        .select(WarehouseDO::getWarehouseCode))
                .stream()
                .map(WarehouseDO::getWarehouseCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    /** 保存业务数据。 */
    @Override
    public void save(WarehouseDO warehouse) {
        warehouseMapper.insert(warehouse);
    }

    /** 更新业务记录。 */
    @Override
    public void update(WarehouseDO warehouse) {
        warehouseMapper.updateById(warehouse);
    }

    /** 重置DefaultBy门店标识。 */
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

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        warehouseMapper.deleteById(id);
    }
}
