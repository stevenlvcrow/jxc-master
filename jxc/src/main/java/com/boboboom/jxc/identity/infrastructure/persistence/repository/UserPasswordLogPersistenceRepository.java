package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserPasswordLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserPasswordLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserPasswordLogPersistenceRepository extends AbstractMpRepository<UserPasswordLogDO> {

    private final UserPasswordLogMapper mapper;

    public UserPasswordLogPersistenceRepository(UserPasswordLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<UserPasswordLogDO> mapper() {
        return mapper;
    }
}

