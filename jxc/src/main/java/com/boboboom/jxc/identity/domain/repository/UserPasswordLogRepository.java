package com.boboboom.jxc.identity.domain.repository;

public interface UserPasswordLogRepository {

    void deleteByUserId(Long userId);
}
