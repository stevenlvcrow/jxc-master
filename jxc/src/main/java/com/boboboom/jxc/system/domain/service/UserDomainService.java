package com.boboboom.jxc.system.domain.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.system.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserDomainService {

    private final UserRepository userRepository;

    public UserDomainService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void checkPhoneUnique(String phone) {
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("手机号已存在");
        }
    }
}

