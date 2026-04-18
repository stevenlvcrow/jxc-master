package com.boboboom.jxc.item.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.DishCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishCategoryMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class DishCategoryRepositoryImpl implements DishCategoryRepository {

    private final DishCategoryMapper dishCategoryMapper;

    public DishCategoryRepositoryImpl(DishCategoryMapper dishCategoryMapper) {
        this.dishCategoryMapper = dishCategoryMapper;
    }

    @Override
    public List<DishCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategoryDO>()
                .eq(DishCategoryDO::getScopeType, scopeType)
                .eq(DishCategoryDO::getScopeId, scopeId)
                .orderByAsc(DishCategoryDO::getCreatedAt)
                .orderByAsc(DishCategoryDO::getId));
    }
}
