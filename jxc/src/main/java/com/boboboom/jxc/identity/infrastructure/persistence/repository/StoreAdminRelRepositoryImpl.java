package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.StoreAdminRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreAdminRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreAdminRelMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class StoreAdminRelRepositoryImpl implements StoreAdminRelRepository {

    private final StoreAdminRelMapper storeAdminRelMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public StoreAdminRelRepositoryImpl(StoreAdminRelMapper storeAdminRelMapperValue) {
        this.storeAdminRelMapper = storeAdminRelMapperValue;
    }

    /** 统计By门店标识数量。 */
    @Override
    public Long countByStoreId(Long storeId) {
        if (storeId == null) {
            return 0L;
        }
        return storeAdminRelMapper.selectCount(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getStoreId, storeId));
    }

    /** 查询By门店标识。 */
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

    /** 查询By用户标识。 */
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

    /** 保存业务数据。 */
    @Override
    public void save(StoreAdminRelDO rel) {
        storeAdminRelMapper.insert(rel);
    }

    /** 更新业务记录。 */
    @Override
    public void update(StoreAdminRelDO rel) {
        storeAdminRelMapper.updateById(rel);
    }

    /** 删除By用户标识。 */
    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        storeAdminRelMapper.delete(new LambdaQueryWrapper<StoreAdminRelDO>()
                .eq(StoreAdminRelDO::getUserId, userId));
    }
}
