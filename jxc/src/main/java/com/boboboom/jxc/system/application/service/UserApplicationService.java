package com.boboboom.jxc.system.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.system.application.command.CreateUserCommand;
import com.boboboom.jxc.system.application.dto.UserDTO;
import com.boboboom.jxc.system.domain.model.User;
import com.boboboom.jxc.system.domain.repository.UserRepository;
import com.boboboom.jxc.system.domain.service.UserDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    public UserApplicationService(UserRepository userRepository, UserDomainService userDomainService) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
    }

    @Transactional
    public Long createUser(CreateUserCommand command) {
        userDomainService.checkPhoneUnique(command.phone());
        User user = User.create(command.username(), command.phone());
        return userRepository.save(user).getId();
    }

    @Transactional(readOnly = true)
    public UserDTO getUser(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional
    public void disableUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        user.disable();
        userRepository.save(user);
    }

    private UserDTO toDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

