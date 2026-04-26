package com.boboboom.jxc.item.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.item.domain.repository.DishCategoryRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.DishCategoryMapper;

/** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class DishCategoryRepositoryImpl implements DishCategoryRepository {

    private final DishCategoryMapper dishCategoryMapper;

    /** 物品与供应商仓储实现，负责通过持久层组件完成数据读写。 */
    public DishCategoryRepositoryImpl(DishCategoryMapper dishCategoryMapperValue) {
        this.dishCategoryMapper = dishCategoryMapperValue;
    }

    /** 查询By作用域排序。 */
    @Override
    public List<DishCategoryDO> findByScopeOrdered(String scopeType, Long scopeId) {
        return dishCategoryMapper.selectList(new LambdaQueryWrapper<DishCategoryDO>()
                .eq(DishCategoryDO::getScopeType, scopeType)
                .eq(DishCategoryDO::getScopeId, scopeId)
                .orderByAsc(DishCategoryDO::getCreatedAt)
                .orderByAsc(DishCategoryDO::getId));
    }
}
