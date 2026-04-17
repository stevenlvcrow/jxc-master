package com.boboboom.jxc.system.interfaces.rest;

import com.boboboom.jxc.common.ApiResponse;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
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
    private final OrgScopeService orgScopeService;

    public UserController(UserApplicationService userApplicationService,
                          OrgScopeService orgScopeService) {
        this.userApplicationService = userApplicationService;
        this.orgScopeService = orgScopeService;
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
        Long userId = AuthContextHolder.requireUserId("仅平台管理员可执行此操作");
        if (!orgScopeService.isPlatformAdmin(userId)) {
            throw new BusinessException("仅平台管理员可执行此操作");
        }
    }
}
