package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;

import java.util.List;

public interface UnitRepository {

    List<UnitDO> findByScope(String scopeType, Long scopeId);

    List<UnitDO> findPlatformTemplates();

    List<UnitDO> findStoreRows(Long storeId);

    java.util.Optional<UnitDO> findByIdAndScope(Long id, String scopeType, Long scopeId);

    java.util.Optional<UnitDO> findByScopeAndUnitCode(String scopeType, Long scopeId, String unitCode);

    java.util.Optional<UnitDO> findByScopeAndUnitName(String scopeType, Long scopeId, String unitName);

    void save(UnitDO unit);

    void update(UnitDO unit);

    void deleteByIdAndScope(Long id, String scopeType, Long scopeId);
}
