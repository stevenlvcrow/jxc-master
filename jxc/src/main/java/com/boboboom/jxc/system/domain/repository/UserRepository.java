package com.boboboom.jxc.system.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.system.domain.model.User;

/** 系统用户仓储接口，定义领域需要的数据访问能力。 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByPhone(String phone);
}

