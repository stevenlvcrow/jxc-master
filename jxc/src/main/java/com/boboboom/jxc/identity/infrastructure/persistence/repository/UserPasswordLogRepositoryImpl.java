package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.UserPasswordLogRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserPasswordLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserPasswordLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserPasswordLogRepositoryImpl implements UserPasswordLogRepository {

    private final UserPasswordLogMapper userPasswordLogMapper;

    public UserPasswordLogRepositoryImpl(UserPasswordLogMapper userPasswordLogMapper) {
        this.userPasswordLogMapper = userPasswordLogMapper;
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        userPasswordLogMapper.delete(new LambdaQueryWrapper<UserPasswordLogDO>()
                .eq(UserPasswordLogDO::getUserId, userId));
    }
}
