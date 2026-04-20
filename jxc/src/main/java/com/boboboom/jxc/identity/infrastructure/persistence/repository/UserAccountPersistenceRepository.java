package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserAccountPersistenceRepository extends AbstractMpRepository<UserAccountDO> {

    private final UserAccountMapper mapper;

    public UserAccountPersistenceRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected BaseMapper<UserAccountDO> mapper() {
        return mapper;
    }

    public Optional<UserAccountDO> findByPhone(String phone) {
        return mapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                .eq(UserAccountDO::getPhone, phone)
                .orderByDesc(UserAccountDO::getId))
                .stream()
                .findFirst();
    }
}

