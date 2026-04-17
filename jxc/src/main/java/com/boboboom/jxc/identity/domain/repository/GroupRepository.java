package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {

    Optional<GroupDO> findById(Long id);

    List<GroupStoreSummary> findActiveGroupStoreSummaries(String status);
}
