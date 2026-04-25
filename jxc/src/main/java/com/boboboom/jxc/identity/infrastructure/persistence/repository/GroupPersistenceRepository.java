package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.GroupMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class GroupPersistenceRepository extends AbstractMpRepository<GroupDO> {

    private final GroupMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public GroupPersistenceRepository(GroupMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<GroupDO> mapper() {
        return mapper;
    }
}

