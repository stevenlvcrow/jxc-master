package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepositoryImpl implements GroupRepository {

    private final GroupMapper groupMapper;

    public GroupRepositoryImpl(GroupMapper groupMapper) {
        this.groupMapper = groupMapper;
    }

    @Override
    public Optional<GroupDO> findById(Long id) {
        return Optional.ofNullable(groupMapper.selectById(id));
    }

    @Override
    public List<GroupStoreSummary> findActiveGroupStoreSummaries(String status) {
        return groupMapper.selectGroupStoreSummary(status);
    }
}
