package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRoleRelPersistenceRepository extends AbstractMpRepository<UserRoleRelDO> {

    private final UserRoleRelMapper mapper;

    public UserRoleRelPersistenceRepository(UserRoleRelMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<UserRoleRelDO> mapper() {
        return mapper;
    }
}

