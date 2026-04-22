package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreAdminRelMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class StoreAdminRelRepositoryImpl implements StoreAdminRelRepository {

    private final StoreAdminRelMapper storeAdminRelMapper;

    public StoreAdminRelRepositoryImpl(StoreAdminRelMapper storeAdminRelMapper) {
        this.storeAdminRelMapper = storeAdminRelMapper;
    }

    @Override
    public Long countByStoreId(Long storeId) {
        if (storeId == null) {
            return 0L;
        }
        return storeAdminRelMapper.selectCount(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getStoreId, storeId));
    }

    @Override
    public Optional<StoreAdminRelDO> findByStoreId(Long storeId) {
        if (storeId == null) {
            return Optional.empty();
        }
        return storeAdminRelMapper.selectList(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getStoreId, storeId)
                .orderByDesc(StoreAdminRelDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<StoreAdminRelDO> findByUserId(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return storeAdminRelMapper.selectList(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getUserId, userId)
                .orderByDesc(StoreAdminRelDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void save(StoreAdminRelDO rel) {
        storeAdminRelMapper.insert(rel);
    }

    @Override
    public void update(StoreAdminRelDO rel) {
        storeAdminRelMapper.updateById(rel);
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        storeAdminRelMapper.delete(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getUserId, userId));
    }
}
