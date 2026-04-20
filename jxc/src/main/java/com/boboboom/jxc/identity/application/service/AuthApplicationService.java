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
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserAccountChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPasswordChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPhoneChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AuthApplicationService {

    private static final String ENABLED_STATUS = "ENABLED";

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final OrgScopeService orgScopeService;
    private final IdentityAdminLookupService identityAdminLookupService;

    public AuthApplicationService(UserAccountRepository userAccountRepository,
                                  TokenService tokenService,
                                  OrgScopeService orgScopeService,
                                  IdentityAdminLookupService identityAdminLookupService) {
        this.userAccountRepository = userAccountRepository;
        this.tokenService = tokenService;
        this.orgScopeService = orgScopeService;
        this.identityAdminLookupService = identityAdminLookupService;
    }

    public AuthLoginResult login(AuthLoginRequest request) {
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
        result.setAccount(user.getUsername());
        result.setPhone(user.getPhone());
        result.setPlatformAdmin(orgScopeService.isPlatformAdmin(user.getId()));
        return result;
    }

    public AuthRefreshResult refresh(RefreshTokenRequest request) {
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
        return result;
    }

    public CurrentUserResult me() {
        LoginSession session = AuthContextHolder.require();
        UserAccountDO user = requireEnabledUser(session.getUserId());
        return toCurrentUserResult(user);
    }

    @Transactional
    public void changeCurrentPassword(CurrentUserPasswordChangeRequest request) {
        LoginSession session = AuthContextHolder.require();
        UserAccountDO user = requireEnabledUser(session.getUserId());
        String oldPassword = normalizePassword(request.getOldPassword(), "原密码不能为空");
        String newPassword = normalizePassword(request.getNewPassword(), "新密码不能为空");
        if (!PasswordCodec.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPasswordHash(PasswordCodec.encode(newPassword));
        user.setFirstLoginChangedPwd(Boolean.TRUE);
        userAccountRepository.update(user);
        tokenService.removeSession(session.getToken());
    }

    @Transactional
    public CurrentUserResult changeCurrentPhone(CurrentUserPhoneChangeRequest request) {
        LoginSession session = AuthContextHolder.require();
        UserAccountDO user = requireEnabledUser(session.getUserId());
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        boolean duplicated = userAccountRepository.findByPhone(phone)
                .map(UserAccountDO::getId)
                .filter(existingId -> !existingId.equals(user.getId()))
                .isPresent();
        if (duplicated) {
            throw new BusinessException("手机号已存在");
        }
        user.setPhone(phone);
        userAccountRepository.update(user);
        return toCurrentUserResult(user);
    }

    @Transactional
    public CurrentUserResult changeCurrentAccount(CurrentUserAccountChangeRequest request) {
        LoginSession session = AuthContextHolder.require();
        UserAccountDO user = requireEnabledUser(session.getUserId());
        String account = normalizeUsername(request.getAccount());
        boolean duplicated = userAccountRepository.findLoginUserByAccount(account)
                .map(UserAccountDO::getId)
                .filter(existingId -> !existingId.equals(user.getId()))
                .isPresent();
        if (duplicated) {
            throw new BusinessException("账号已存在");
        }
        user.setUsername(account);
        userAccountRepository.update(user);
        return toCurrentUserResult(user);
    }

    public List<CurrentUserRoleResult> meRoles(String orgId) {
        LoginSession session = AuthContextHolder.require();
        List<UserRoleView> allRoles = userAccountRepository.findUserRoles(session.getUserId());
        if (orgId == null || orgId.trim().isEmpty()) {
            return mapRoles(allRoles);
        }
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(session.getUserId(), orgId);
        List<CurrentUserRoleResult> roles = mapRoles(allRoles.stream()
                .filter(role -> matchesSelectedScope(role, scope))
                .toList());
        return roles;
    }

    public void logout() {
        LoginSession session = AuthContextHolder.require();
        tokenService.removeSession(session.getToken());
    }

    private String normalizeAccount(String account) {
        return account == null ? "" : account.trim();
    }

    private String normalizePassword(String password, String emptyMessage) {
        String value = password == null ? "" : password.trim();
        if (value.isEmpty()) {
            throw new BusinessException(emptyMessage);
        }
        if (value.length() < 6 || value.length() > 32) {
            throw new BusinessException("密码长度需为6-32位");
        }
        return value;
    }

    private String normalizeUsername(String account) {
        String value = account == null ? "" : account.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new BusinessException("账号不能为空");
        }
        if (!value.matches("^[a-z][a-z0-9_]{4,19}$")) {
            throw new BusinessException("账号必须为5-20位字母数字下划线，且以字母开头");
        }
        return value;
    }

    private UserAccountDO requireEnabledUser(Long userId) {
        UserAccountDO user = userAccountRepository.findById(userId).orElse(null);
        if (user == null || !ENABLED_STATUS.equals(user.getStatus())) {
            throw new UnauthorizedException("登录已失效，请重新登录");
        }
        return user;
    }

    private CurrentUserResult toCurrentUserResult(UserAccountDO user) {
        CurrentUserResult result = new CurrentUserResult();
        result.setUserId(user.getId());
        result.setUserName(user.getRealName());
        result.setAccount(user.getUsername());
        result.setPhone(user.getPhone());
        return result;
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

    private boolean matchesSelectedScope(UserRoleView role, OrgScopeService.AccessibleScope scope) {
        if (role == null || scope == null) {
            return false;
        }
        String roleScopeType = role.getScopeType();
        Long roleScopeId = role.getScopeId();
        if (roleScopeType == null) {
            return false;
        }
        if ("PLATFORM".equalsIgnoreCase(scope.scopeType())) {
            return "PLATFORM".equalsIgnoreCase(roleScopeType);
        }
        if ("GROUP".equalsIgnoreCase(scope.scopeType())) {
            return "PLATFORM".equalsIgnoreCase(roleScopeType)
                    || ("GROUP".equalsIgnoreCase(roleScopeType) && Objects.equals(roleScopeId, scope.scopeId()));
        }
        if ("STORE".equalsIgnoreCase(scope.scopeType())) {
            return "PLATFORM".equalsIgnoreCase(roleScopeType)
                    || ("STORE".equalsIgnoreCase(roleScopeType) && Objects.equals(roleScopeId, scope.scopeId()))
                    || ("GROUP".equalsIgnoreCase(roleScopeType) && Objects.equals(roleScopeId, scope.groupId()));
        }
        return false;
    }

    public record CurrentUserRoleResult(String roleCode,
                                        String roleName,
                                        String scopeType,
                                        String scopeName) {
    }
}
