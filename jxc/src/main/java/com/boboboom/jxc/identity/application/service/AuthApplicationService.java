package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.LoginSession;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import com.boboboom.jxc.identity.application.auth.TokenService;
import com.boboboom.jxc.identity.application.auth.UnauthorizedException;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthApplicationService {

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final OrgScopeService orgScopeService;

    public AuthApplicationService(UserAccountRepository userAccountRepository,
                                  TokenService tokenService,
                                  OrgScopeService orgScopeService) {
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
        this.orgScopeService = orgScopeService;
    }

    public CodeDataResponse<AuthLoginResult> login(AuthLoginRequest request) {
        String account = normalizeAccount(request.getAccount());
        UserAccountDO user = userAccountRepository.findLoginUserByAccount(account).orElse(null);
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

    public CodeDataResponse<AuthRefreshResult> refresh(RefreshTokenRequest request) {
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

    public CodeDataResponse<CurrentUserResult> me() {
        LoginSession session = AuthContextHolder.require();
        CurrentUserResult result = new CurrentUserResult();
        result.setUserId(session.getUserId());
        result.setUserName(session.getRealName());
        result.setPhone(session.getPhone());
        return CodeDataResponse.ok(result);
    }

    public CodeDataResponse<List<CurrentUserRoleResult>> meRoles(String orgId) {
        LoginSession session = AuthContextHolder.require();
        List<UserRoleView> allRoles = userAccountRepository.findUserRoles(session.getUserId());
        if (orgId == null || orgId.trim().isEmpty()) {
            return CodeDataResponse.ok(mapRoles(allRoles));
        }
        OrgScopeService.MenuScope scope = orgScopeService.resolveMenuScope(orgId);
        List<CurrentUserRoleResult> roles = mapRoles(allRoles.stream()
                .filter(role -> matchesSelectedScope(role, scope))
                .toList());
        return CodeDataResponse.ok(roles);
    }

    public CodeDataResponse<Void> logout() {
        LoginSession session = AuthContextHolder.require();
        tokenService.removeSession(session.getToken());
        return CodeDataResponse.ok();
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim();
    }

    private List<CurrentUserRoleResult> mapRoles(List<UserRoleView> roles) {
        return roles.stream()
                .filter(role -> role != null && role.getRoleCode() != null && role.getRoleName() != null)
                .map(role -> new CurrentUserRoleResult(
                        role.getRoleCode(),
                        role.getRoleName(),
                        role.getScopeType(),
                        role.getScopeName()
                ))
                .toList();
    }

    private boolean matchesSelectedScope(UserRoleView role, OrgScopeService.MenuScope scope) {
        if (role == null || scope == null) {
            return false;
        }
        String roleScopeType = role.getScopeType();
        Long roleScopeId = role.getScopeId();
        if (roleScopeType == null || roleScopeId == null) {
            return false;
        }
        if (!roleScopeType.equalsIgnoreCase(scope.scopeType())) {
            return false;
        }
        return roleScopeId.equals(scope.scopeId());
    }

    public record CurrentUserRoleResult(String roleCode,
                                        String roleName,
                                        String scopeType,
                                        String scopeName) {
    }
}
