package com.boboboom.jxc.system.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.system.application.command.CreateUserCommand;
import com.boboboom.jxc.system.application.dto.UserDTO;
import com.boboboom.jxc.system.domain.model.User;
import com.boboboom.jxc.system.domain.repository.UserRepository;
import com.boboboom.jxc.system.domain.service.UserDomainService;

/** 系统用户应用服务，负责业务流程编排、权限校验和事务边界。 */
@Service
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;

    /** 系统用户应用服务，负责业务流程编排、权限校验和事务边界。 */
    public UserApplicationService(UserRepository userRepositoryValue, UserDomainService userDomainServiceValue) {
        this.userRepository = userRepositoryValue;
        this.userDomainService = userDomainServiceValue;
    }

    /** 创建用户。 */
    @Transactional
    public Long createUser(CreateUserCommand command) {
        userDomainService.checkPhoneUnique(command.phone());
        User user = User.create(command.username(), command.phone());
        return userRepository.save(user).getId();
    }

    /** 获取User。 */
    @Transactional(readOnly = true)
    public UserDTO getUser(Long id) {
        return userRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessException("用户不存在"));
    }

    /** 处理Transactional。 */
    @Transactional(readOnly = true)
    public List<UserDTO> listUsers() {
        return userRepository.findAll().stream().map(this::toDTO).toList();
    }

    /** 处理停用用户。 */
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
