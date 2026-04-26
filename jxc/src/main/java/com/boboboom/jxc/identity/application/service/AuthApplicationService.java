package com.boboboom.jxc.identity.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserAccountChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPasswordChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPhoneChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;

/** 认证业务服务，负责登录、刷新令牌和当前用户信息组装。 */
@Service
public class AuthApplicationService {

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 32;

    private final UserAccountRepository userAccountRepository;
    private final TokenService tokenService;
    private final OrgScopeService orgScopeService;
    private final IdentityAdminLookupService identityAdminLookupService;

    /** 认证业务服务，负责登录、刷新令牌和当前用户信息组装。 */
    public AuthApplicationService(UserAccountRepository userAccountRepositoryValue,
                                  TokenService tokenServiceValue,
                                  OrgScopeService orgScopeServiceValue,
                                  IdentityAdminLookupService identityAdminLookupServiceValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.tokenService = tokenServiceValue;
        this.orgScopeService = orgScopeServiceValue;
        this.identityAdminLookupService = identityAdminLookupServiceValue;
    }

    /** 处理用户登录请求。 */
    public AuthLoginResult login(AuthLoginRequest request) {
        String account = normalizeAccount(request.getAccount());
        UserAccountDO user = userAccountRepository.findLoginUserByAccount(account).orElse(null);
        if (user == null || !PasswordCodec.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (!identityAdminLookupService.enabledStatus().equals(user.getStatus())) {
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

    /** 刷新登录令牌。 */
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

    /** 查询当前登录用户信息。 */
    public CurrentUserResult me() {
        LoginSession session = AuthContextHolder.require();
        UserAccountDO user = requireEnabledUser(session.getUserId());
        return toCurrentUserResult(user);
    }

    /** 处理change当前密码。 */
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

    /** 处理change当前手机号。 */
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

    /** 处理change当前账号。 */
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

    /** 处理me角色。 */
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

    /** 退出当前登录会话。 */
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
        if (value.length() < MIN_PASSWORD_LENGTH || value.length() > MAX_PASSWORD_LENGTH) {
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
        if (user == null || !identityAdminLookupService.enabledStatus().equals(user.getStatus())) {
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
        if (roleScopeType == null) {
            return false;
        }
        if ("PLATFORM".equalsIgnoreCase(scope.scopeType())) {
            return "PLATFORM".equalsIgnoreCase(roleScopeType);
        }
        if ("GROUP".equalsIgnoreCase(scope.scopeType())) {
            return matchesGroupSelectedScope(role, scope);
        }
        if ("STORE".equalsIgnoreCase(scope.scopeType())) {
            return matchesStoreSelectedScope(role, scope);
        }
        return false;
    }

    private boolean matchesGroupSelectedScope(UserRoleView role, OrgScopeService.AccessibleScope scope) {
        return "PLATFORM".equalsIgnoreCase(role.getScopeType())
                || matchesRoleScope(role, "GROUP", scope.scopeId());
    }

    private boolean matchesStoreSelectedScope(UserRoleView role, OrgScopeService.AccessibleScope scope) {
        return "PLATFORM".equalsIgnoreCase(role.getScopeType())
                || matchesRoleScope(role, "STORE", scope.scopeId())
                || matchesRoleScope(role, "GROUP", scope.groupId());
    }

    private boolean matchesRoleScope(UserRoleView role, String scopeType, Long scopeId) {
        return scopeType.equalsIgnoreCase(role.getScopeType()) && Objects.equals(role.getScopeId(), scopeId);
    }

    /** 身份与权限结果模型，承载业务处理结果。 */
    public record CurrentUserRoleResult(String roleCode,
                                        String roleName,
                                        String scopeType,
                                        String scopeName) {
    }
}
