package com.boboboom.jxc.system.interfaces.rest;

import com.boboboom.jxc.common.ApiResponse;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.system.application.command.CreateUserCommand;
import com.boboboom.jxc.system.application.dto.UserDTO;
import com.boboboom.jxc.system.application.service.UserApplicationService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserApplicationService userApplicationService;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;

    public UserController(UserApplicationService userApplicationService,
                          RoleMapper roleMapper,
                          UserRoleRelMapper userRoleRelMapper) {
        this.userApplicationService = userApplicationService;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CreateUserCommand command) {
        requirePlatformAdmin();
        Long id = userApplicationService.createUser(command);
        return ApiResponse.ok(Map.of("id", id));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDTO> get(@PathVariable Long id) {
        requirePlatformAdmin();
        return ApiResponse.ok(userApplicationService.getUser(id));
    }

    @GetMapping
    public ApiResponse<List<UserDTO>> list() {
        requirePlatformAdmin();
        return ApiResponse.ok(userApplicationService.listUsers());
    }

    @PutMapping("/{id}/disable")
    public ApiResponse<Void> disable(@PathVariable Long id) {
        requirePlatformAdmin();
        userApplicationService.disableUser(id);
        return ApiResponse.ok();
    }

    private void requirePlatformAdmin() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
        Long userId = AuthContextHolder.get().getUserId();
        RoleDO role = roleMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, "PLATFORM")
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        if (rel == null) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
    }
}

