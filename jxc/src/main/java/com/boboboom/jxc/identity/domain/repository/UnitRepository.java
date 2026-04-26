package com.boboboom.jxc.identity.domain.repository;

import java.util.List;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UnitDO;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
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
