package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.StoreAdminView;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreMapper storeMapper;

    public StoreRepositoryImpl(StoreMapper storeMapper) {
        this.storeMapper = storeMapper;
    }

    @Override
    public Optional<StoreDO> findById(Long id) {
        return Optional.ofNullable(storeMapper.selectById(id));
    }

    @Override
    public Optional<StoreDO> findByStoreCode(String storeCode) {
        return Optional.ofNullable(storeMapper.selectOne(new LambdaQueryWrapper<StoreDO>()
                .eq(StoreDO::getStoreCode, storeCode)
                .last("limit 1")));
    }

    @Override
    public List<StoreDO> findAllOrdered() {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .orderByDesc(StoreDO::getCreatedAt)
                .orderByDesc(StoreDO::getId));
    }

    @Override
    public List<String> findAllStoreCodes() {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                        .select(StoreDO::getStoreCode))
                .stream()
                .map(StoreDO::getStoreCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }

    @Override
    public List<StoreDO> findByGroupId(Long groupId) {
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .eq(StoreDO::getGroupId, groupId)
                .orderByDesc(StoreDO::getCreatedAt)
                .orderByDesc(StoreDO::getId));
    }

    @Override
    public List<StoreDO> findByGroupIds(List<Long> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }
        return storeMapper.selectList(new LambdaQueryWrapper<StoreDO>()
                .select(StoreDO::getId)
                .in(StoreDO::getGroupId, groupIds));
    }

    @Override
    public Long countByGroupId(Long groupId) {
        return storeMapper.selectCount(new LambdaQueryWrapper<StoreDO>().eq(StoreDO::getGroupId, groupId));
    }

    @Override
    public List<StoreAdminView> findStoreAdminViewsByGroupId(Long groupId, String status) {
        return storeMapper.selectStoreAdminViewByGroupId(groupId, status);
    }

    @Override
    public void save(StoreDO store) {
        storeMapper.insert(store);
    }

    @Override
    public void update(StoreDO store) {
        storeMapper.updateById(store);
    }

    @Override
    public void deleteById(Long id) {
        storeMapper.deleteById(id);
    }
}
