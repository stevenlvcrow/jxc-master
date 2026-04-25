package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class UserRoleRelPersistenceRepository extends AbstractMpRepository<UserRoleRelDO> {

    private final UserRoleRelMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public UserRoleRelPersistenceRepository(UserRoleRelMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<UserRoleRelDO> mapper() {
        return mapper;
    }
}

