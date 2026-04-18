package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {

    Optional<GroupDO> findById(Long id);

    Optional<GroupDO> findByGroupCode(String groupCode);

    List<GroupDO> findAllOrdered();

    List<GroupDO> findByIdsOrdered(List<Long> ids);

    List<String> findAllGroupCodes();

    List<GroupStoreSummary> findActiveGroupStoreSummaries(String status);

    void save(GroupDO group);

    void update(GroupDO group);

    void deleteById(Long id);
}
