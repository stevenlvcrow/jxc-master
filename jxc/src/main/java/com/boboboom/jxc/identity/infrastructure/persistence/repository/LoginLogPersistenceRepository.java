package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.LoginLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LoginLogPersistenceRepository extends AbstractMpRepository<LoginLogDO> {

    private final LoginLogMapper mapper;

    public LoginLogPersistenceRepository(LoginLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<LoginLogDO> mapper() {
        return mapper;
    }
}

