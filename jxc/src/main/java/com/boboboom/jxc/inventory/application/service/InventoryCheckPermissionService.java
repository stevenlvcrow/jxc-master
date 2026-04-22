package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import org.springframework.stereotype.Service;

/**
 * 盘点单权限判断服务。
 */
@Service
public class InventoryCheckPermissionService {

    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;

    public InventoryCheckPermissionService(UserRoleRelRepository userRoleRelRepository,
                                           RoleRepository roleRepository) {
        this.userRoleRelRepository = userRoleRelRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 计算页面权限。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param groupId 所属集团 ID
     * @param operatorId 操作人 ID
     * @return 权限快照
     */
    public PermissionSnapshot resolvePermissions(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        boolean canManageAll = canViewAll(scopeType, scopeId, groupId, operatorId);
        return new PermissionSnapshot(canManageAll, canManageAll, canManageAll, canManageAll, canManageAll);
    }

    /**
     * 判断当前用户是否可查看当前作用域下全部盘点单。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param groupId 所属集团 ID
     * @param operatorId 操作人 ID
     * @return 是否可查看全部
     */
    public boolean canViewAll(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        if (hasRoleInScope(operatorId, "PLATFORM_SUPER_ADMIN", "PLATFORM", 0L)) {
            return true;
        }
        if (hasRoleInScope(operatorId, "STORE_ADMIN", "STORE", scopeId)) {
            return true;
        }
        return "STORE".equals(scopeType)
                && groupId != null
                && hasRoleInScope(operatorId, "GROUP_ADMIN", "GROUP", groupId);
    }

    /**
     * 校验盘点单操作权限。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param groupId 所属集团 ID
     * @param operatorId 操作人 ID
     * @param action 操作类型
     */
    public void ensureOperationPermission(String scopeType,
                                          Long scopeId,
                                          Long groupId,
                                          Long operatorId,
                                          String action) {
        if (canViewAll(scopeType, scopeId, groupId, operatorId)) {
            return;
        }
        throw new BusinessException("当前账号无盘点单" + action + "权限");
    }

    /**
     * 校验盘点审核权限。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param groupId 所属集团 ID
     * @param operatorId 操作人 ID
     */
    public void ensureReviewPermission(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (!canViewAll(scopeType, scopeId, groupId, operatorId)) {
            throw new BusinessException("当前账号无盘点单审核权限");
        }
    }

    private boolean hasRoleInScope(Long operatorId, String roleCode, String scopeType, Long scopeId) {
        if (operatorId == null || scopeId == null) {
            return false;
        }
        RoleDO role = roleRepository.findByRoleCode(roleCode).orElse(null);
        if (role == null) {
            return false;
        }
        return userRoleRelRepository.findByUserIdRoleAndScope(operatorId, role.getId(), scopeType, scopeId).isPresent();
    }

    public record PermissionSnapshot(boolean canCreate,
                                     boolean canUpdate,
                                     boolean canDelete,
                                     boolean canApprove,
                                     boolean canUnapprove) {
    }
}
