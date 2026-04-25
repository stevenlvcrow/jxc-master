package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;

/** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
@Repository
public class UserAccountPersistenceRepository extends AbstractMpRepository<UserAccountDO> {

    private final UserAccountMapper mapper;

    /** 身份与权限持久化仓储，封装基础 CRUD 能力。 */
    public UserAccountPersistenceRepository(UserAccountMapper mapperValue) {
        this.mapper = mapperValue;
    }

    @Override
    protected BaseMapper<UserAccountDO> mapper() {
        return mapper;
    }

    /** 查询By手机号。 */
    public Optional<UserAccountDO> findByPhone(String phone) {
        return mapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getPhone, phone)
                .orderByDesc(UserAccountDO::getId))
                .stream()
                .findFirst();
    }
}

