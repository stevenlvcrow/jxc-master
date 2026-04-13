package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;
import org.springframework.stereotype.Repository;

@Repository
public class GroupPersistenceRepository extends AbstractMpRepository<GroupDO> {

    private final GroupMapper mapper;

    public GroupPersistenceRepository(GroupMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<GroupDO> mapper() {
        return mapper;
    }
}

