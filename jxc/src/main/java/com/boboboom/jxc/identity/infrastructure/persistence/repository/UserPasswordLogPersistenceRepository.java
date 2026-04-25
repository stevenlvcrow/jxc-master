package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserPasswordLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserPasswordLogMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class UserPasswordLogPersistenceRepository extends AbstractMpRepository<UserPasswordLogDO> {

    private final UserPasswordLogMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public UserPasswordLogPersistenceRepository(UserPasswordLogMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<UserPasswordLogDO> mapper() {
        return mapper;
    }
}

