package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.DishProfileRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishProfileDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishProfileMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DishProfileRepositoryImpl implements DishProfileRepository {

    private final DishProfileMapper dishProfileMapper;

    public DishProfileRepositoryImpl(DishProfileMapper dishProfileMapper) {
        this.dishProfileMapper = dishProfileMapper;
    }

    @Override
    public List<DishProfileDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return dishProfileMapper.selectList(new LambdaQueryWrapper<DishProfileDO>()
                .eq(DishProfileDO::getScopeType, scopeType)
                .eq(DishProfileDO::getScopeId, scopeId)
                .orderByDesc(DishProfileDO::getUpdatedAt)
                .orderByDesc(DishProfileDO::getId));
    }
}
