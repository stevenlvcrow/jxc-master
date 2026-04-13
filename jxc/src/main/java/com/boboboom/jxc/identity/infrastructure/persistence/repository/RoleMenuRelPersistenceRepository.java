package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMenuRelMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RoleMenuRelPersistenceRepository extends AbstractMpRepository<RoleMenuRelDO> {

    private final RoleMenuRelMapper mapper;

    public RoleMenuRelPersistenceRepository(RoleMenuRelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<RoleMenuRelDO> mapper() {
        return mapper;
    }
}

