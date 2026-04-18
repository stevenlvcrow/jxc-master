package com.boboboom.jxc.item.domain.repository;

import com.boboboom.jxc.item.infrastructure.persistence.dataobject.DishProfileDO;

import java.util.List;

public interface DishProfileRepository {

    List<DishProfileDO> findByScopeOrdered(String scopeType, Long scopeId);
}
