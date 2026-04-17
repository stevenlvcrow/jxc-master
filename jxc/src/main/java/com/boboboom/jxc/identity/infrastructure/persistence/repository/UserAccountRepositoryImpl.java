package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserAccountRepositoryImpl implements UserAccountRepository {

    private final UserAccountMapper userAccountMapper;

    public UserAccountRepositoryImpl(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    @Override
    public Optional<UserAccountDO> findById(Long id) {
        return Optional.ofNullable(userAccountMapper.selectById(id));
    }
}
