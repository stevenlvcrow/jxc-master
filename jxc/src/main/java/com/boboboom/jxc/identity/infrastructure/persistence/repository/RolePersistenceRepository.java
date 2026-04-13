package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RolePersistenceRepository extends AbstractMpRepository<RoleDO> {

    private final RoleMapper mapper;

    public RolePersistenceRepository(RoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<RoleDO> mapper() {
        return mapper;
    }
}

