package com.boboboom.jxc.identity.domain.repository;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;

import java.util.Optional;

public interface UserAccountRepository {

    Optional<UserAccountDO> findById(Long id);
}
