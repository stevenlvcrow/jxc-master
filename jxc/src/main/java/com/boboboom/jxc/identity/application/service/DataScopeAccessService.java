package com.boboboom.jxc.identity.application.service;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;

/**
 * 统一判断当前用户在机构范围内的数据可见范围。
 */
@Service
public class DataScopeAccessService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";
    private static final String DATA_SCOPE_ALL = "ALL";
    private static final String DATA_SCOPE_GROUP = "GROUP";
    private static final String DATA_SCOPE_STORE = "STORE";

    private final UserAccountRepository userAccountRepository;
    private final OrgScopeService orgScopeService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public DataScopeAccessService(UserAccountRepository userAccountRepositoryValue,
                                  OrgScopeService orgScopeServiceValue) {
        this.userAccountRepository = userAccountRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public boolean canViewScopeData(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (operatorId == null || scopeType == null || scopeId == null) {
            return false;
        }
        if (orgScopeService.isPlatformAdmin(operatorId)) {
            return true;
        }
        List<UserRoleView> roles = userAccountRepository.findUserRoles(operatorId);
        if (SCOPE_GROUP.equals(scopeType)) {
            return canViewGroupScopeData(roles, scopeId);
        }
        if (SCOPE_STORE.equals(scopeType)) {
            return canViewStoreScopeData(roles, scopeId, groupId);
        }
        return roles.stream().anyMatch(role -> DATA_SCOPE_ALL.equals(role.getDataScopeType()));
    }

    private boolean canViewGroupScopeData(List<UserRoleView> roles, Long scopeId) {
        return roles.stream().anyMatch(role -> matchesScopeData(role, SCOPE_GROUP, scopeId, DATA_SCOPE_GROUP));
    }

    private boolean canViewStoreScopeData(List<UserRoleView> roles, Long scopeId, Long groupId) {
        if (roles.stream().anyMatch(role -> matchesScopeData(role, SCOPE_STORE, scopeId, DATA_SCOPE_STORE))) {
            return true;
        }
        return groupId != null && canViewGroupScopeData(roles, groupId);
    }

    private boolean matchesScopeData(UserRoleView role, String scopeType, Long scopeId, String dataScopeType) {
        return scopeType.equals(role.getScopeType())
                && Objects.equals(role.getScopeId(), scopeId)
                && dataScopeType.equals(role.getDataScopeType());
    }
}
