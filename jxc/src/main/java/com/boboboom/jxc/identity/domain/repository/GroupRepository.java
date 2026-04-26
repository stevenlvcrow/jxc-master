package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
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
