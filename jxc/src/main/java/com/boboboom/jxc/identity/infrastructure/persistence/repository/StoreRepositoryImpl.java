package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreMapper storeMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public StoreRepositoryImpl(StoreMapper storeMapperValue) {
        this.storeMapper = storeMapperValue;
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<StoreDO> findById(Long id) {
        return Optional.ofNullable(storeMapper.selectById(id));
    }

    /** 查询By门店编码。 */
    @Override
    public Optional<StoreDO> findByStoreCode(String storeCode) {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .eq(StoreDO::getStoreCode, storeCode)
                .orderByDesc(StoreDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询All排序。 */
    @Override
    public List<StoreDO> findAllOrdered() {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .orderByDesc(StoreDO::getCreatedAt)
                .orderByDesc(StoreDO::getId));
    }

    /** 查询All门店Codes。 */
    @Override
    public List<String> findAllStoreCodes() {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                        .select(StoreDO::getStoreCode))
                .stream()
                .map(StoreDO::getStoreCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    /** 查询By集团标识。 */
    @Override
    public List<StoreDO> findByGroupId(Long groupId) {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .eq(StoreDO::getGroupId, groupId)
                .orderByDesc(StoreDO::getCreatedAt)
                .orderByDesc(StoreDO::getId));
    }

    /** 查询By集团标识。 */
    @Override
    public List<StoreDO> findByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .select(StoreDO::getId)
                .in(StoreDO::getGroupId, groupIds));
    }

    /** 统计By集团标识数量。 */
    @Override
    public Long countByGroupId(Long groupId) {
        return storeMapper.selectCount(new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getGroupId, groupId));
    }

    /** 查询门店管理ViewsBy集团标识。 */
    @Override
    public List<StoreAdminView> findStoreAdminViewsByGroupId(Long groupId, String status) {
        return storeMapper.selectStoreAdminViewByGroupId(groupId, status);
    }

    /** 保存业务数据。 */
    @Override
    public void save(StoreDO store) {
        storeMapper.insert(store);
    }

    /** 更新业务记录。 */
    @Override
    public void update(StoreDO store) {
        storeMapper.updateById(store);
    }

    /** 按主键删除记录。 */
    @Override
    public void deleteById(Long id) {
        storeMapper.deleteById(id);
    }
}
