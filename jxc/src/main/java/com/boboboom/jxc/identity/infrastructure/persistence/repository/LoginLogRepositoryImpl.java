package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.LoginLogRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.LoginLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.LoginLogMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper loginLogMapper;

    public LoginLogRepositoryImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        loginLogMapper.delete(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getUserId, userId));
    }
}
