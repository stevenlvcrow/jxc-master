package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.LoginLogRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.LoginLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.LoginLogMapper;

/** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper loginLogMapper;

    /** 身份与权限仓储实现，负责通过持久层组件完成数据读写。 */
    public LoginLogRepositoryImpl(LoginLogMapper loginLogMapperValue) {
        this.loginLogMapper = loginLogMapperValue;
    }

    /** 删除By用户标识。 */
    @Override
    public void deleteByUserId(Long userId) {
        if (userId == null) {
            return;
        }
        loginLogMapper.delete(new LambdaQueryWrapper<LoginLogDO>()
                .eq(LoginLogDO::getUserId, userId));
    }
}
