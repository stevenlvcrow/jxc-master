package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.DishProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishProfileMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class DishProfileRepositoryImpl implements DishProfileRepository {

    private final DishProfileMapper dishProfileMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public DishProfileRepositoryImpl(DishProfileMapper dishProfileMapperValue) {
        this.dishProfileMapper = dishProfileMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<DishProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return dishProfileMapper.selectList(new LambdaQueryWrapper<DishProfileDO>()
                .eq(DishProfileDO::getScopeType, scopeType)
                .eq(DishProfileDO::getScopeId, scopeId)
                .orderByDesc(DishProfileDO::getUpdatedAt)
                .orderByDesc(DishProfileDO::getId));
    }
}
