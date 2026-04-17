package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.application.auth.TokenService;
import com.boboboom.jxc.identity.application.auth.UnauthorizedException;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
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
    private final TokenService tokenService;
    private final OrgScopeService orgScopeService;

    public AuthController(UserAccountMapper userAccountMapper,
                          TokenService tokenService,
                          OrgScopeService orgScopeService) {
        this.userAccountMapper = userAccountMapper;
        this.tokenService = tokenService;
        this.orgScopeService = orgScopeService;
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
        result.setRefreshToken(session.getRefreshToken());
        result.setUserName(user.getRealName());
        result.setPlatformAdmin(orgScopeService.isPlatformAdmin(user.getId()));
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/refresh")
    public CodeDataResponse<AuthRefreshResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginSession oldSession = tokenService.getSessionByRefreshToken(request.getRefreshToken());
        if (oldSession == null) {
            throw new UnauthorizedException("登录已过期，请重新登录");
        }
        tokenService.removeRefreshSession(request.getRefreshToken());

        LoginSession newSession = tokenService.createSession(
                oldSession.getUserId(),
                oldSession.getPhone(),
                oldSession.getRealName()
        );
        AuthRefreshResult result = new AuthRefreshResult();
        result.setAccessToken(newSession.getToken());
        result.setRefreshToken(newSession.getRefreshToken());
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
}
