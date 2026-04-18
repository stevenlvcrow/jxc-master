package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishCategoryDO;

import java.util.List;

public interface DishCategoryRepository {

    List<DishCategoryDO> findByScopeOrdered(String scopeType, Long scopeId);
}
