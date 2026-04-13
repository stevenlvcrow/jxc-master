package com.boboboom.jxc.system.domain.repository;

import com.boboboom.jxc.system.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    boolean existsByPhone(String phone);
}

