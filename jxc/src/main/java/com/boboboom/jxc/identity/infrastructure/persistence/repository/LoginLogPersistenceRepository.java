package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.LoginLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.LoginLogMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class LoginLogPersistenceRepository extends AbstractMpRepository<LoginLogDO> {

    private final LoginLogMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public LoginLogPersistenceRepository(LoginLogMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<LoginLogDO> mapper() {
        return mapper;
    }
}

