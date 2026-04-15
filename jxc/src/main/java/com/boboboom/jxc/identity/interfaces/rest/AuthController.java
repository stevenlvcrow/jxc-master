package com.boboboom.jxc.identity.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.application.auth.TokenService;
import com.boboboom.jxc.identity.application.auth.UnauthorizedException;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/auth", "/api/identity/auth"})
public class AuthController {

    private final UserAccountMapper userAccountMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final TokenService tokenService;

    public AuthController(UserAccountMapper userAccountMapper,
                          RoleMapper roleMapper,
                          UserRoleRelMapper userRoleRelMapper,
                          TokenService tokenService) {
        this.userAccountMapper = userAccountMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public CodeDataResponse<AuthLoginResult> login(@Valid @RequestBody AuthLoginRequest request) {
        String account = normalizeAccount(request.getAccount());
        UserAccountDO user = userAccountMapper.selectLoginUserByAccount(account);
        if (user == null || !PasswordCodec.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (!"ENABLED".equals(user.getStatus())) {
            throw new BusinessException("账号已禁用");
        }

        LoginSession session = tokenService.createSession(user.getId(), user.getPhone(), user.getRealName());
        AuthLoginResult result = new AuthLoginResult();
        result.setAccessToken(session.getToken());
        result.setRefreshToken(session.getToken());
        result.setUserName(user.getRealName());
        result.setPlatformAdmin(isPlatformAdmin(user.getId()));
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/refresh")
    public CodeDataResponse<AuthRefreshResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginSession oldSession = tokenService.getSession(request.getRefreshToken());
        if (oldSession == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        tokenService.removeSession(request.getRefreshToken());

        LoginSession newSession = tokenService.createSession(
                oldSession.getUserId(),
                oldSession.getPhone(),
                oldSession.getRealName()
        );
        AuthRefreshResult result = new AuthRefreshResult();
        result.setAccessToken(newSession.getToken());
        result.setRefreshToken(newSession.getToken());
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/me")
    public CodeDataResponse<CurrentUserResult> me() {
        LoginSession session = AuthContextHolder.require();
        CurrentUserResult result = new CurrentUserResult();
        result.setUserId(session.getUserId());
        result.setUserName(session.getRealName());
        result.setPhone(session.getPhone());
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/logout")
    public CodeDataResponse<Void> logout() {
        LoginSession session = AuthContextHolder.require();
        tokenService.removeSession(session.getToken());
        return CodeDataResponse.ok();
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim();
    }

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, "PLATFORM")
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }
}
