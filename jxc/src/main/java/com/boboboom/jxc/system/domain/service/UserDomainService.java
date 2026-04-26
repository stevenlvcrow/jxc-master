package com.boboboom.jxc.system.domain.service;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.system.domain.repository.UserRepository;

/** 系统用户服务，负责相关业务规则和流程协作。 */
@Service
public class UserDomainService {

    private final UserRepository userRepository;

    /** 系统用户服务，负责相关业务规则和流程协作。 */
    public UserDomainService(UserRepository userRepositoryValue) {
        this.userRepository = userRepositoryValue;
    }

    /** 校验手机号唯一性。 */
    public void checkPhoneUnique(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已存在");
        }
    }
}

