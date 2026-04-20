package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.workflow.application.service.PurchaseInboundWorkflowService;
import org.springframework.stereotype.Service;

/**
 * 采购入库权限判断服务。
 */
@Service
public class PurchaseInboundPermissionService {

    private final PurchaseInboundWorkflowService purchaseInboundWorkflowService;
    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;

    public PurchaseInboundPermissionService(PurchaseInboundWorkflowService purchaseInboundWorkflowService,
                                            UserRoleRelRepository userRoleRelRepository,
                                            RoleRepository roleRepository) {
        this.purchaseInboundWorkflowService = purchaseInboundWorkflowService;
        this.userRoleRelRepository = userRoleRelRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 计算采购入库页面权限。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    所属集团 ID
     * @param operatorId 操作人 ID
     * @return 页面权限快照
     */
    public PermissionSnapshot resolvePermissions(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        boolean canManageAll = canViewAll(scopeType, scopeId, groupId, operatorId);
        boolean canApprove = canReview(scopeType, scopeId, groupId, operatorId);
        return new PermissionSnapshot(
                canManageAll || hasWorkflowOperationPermission(scopeType, scopeId, groupId, operatorId, "CREATE"),
                canManageAll || hasWorkflowOperationPermission(scopeType, scopeId, groupId, operatorId, "UPDATE"),
                canManageAll || hasWorkflowOperationPermission(scopeType, scopeId, groupId, operatorId, "DELETE"),
                canApprove,
                canApprove
        );
    }

    /**
     * 判断当前用户是否可查看当前作用域下全部采购入库单据。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    所属集团 ID
     * @param operatorId 操作人 ID
     * @return 是否可查看全部
     */
    public boolean canViewAll(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (operatorId == null) {
            return false;
        }
        if (hasRoleInScope(operatorId, "STORE_ADMIN", "STORE", scopeId)) {
            return true;
        }
        return "STORE".equals(scopeType)
                && groupId != null
                && hasRoleInScope(operatorId, "GROUP_ADMIN", "GROUP", groupId);
    }

    /**
     * 判断当前用户是否可审核采购入库。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    所属集团 ID
     * @param operatorId 操作人 ID
     * @return 是否可审核
     */
    public boolean canReview(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        return purchaseInboundWorkflowService.hasBusinessReviewPermission(scopeType, scopeId, groupId, operatorId);
    }

    /**
     * 判断当前用户是否为门店业务员。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param operatorId 操作人 ID
     * @return 是否为门店业务员
     */
    public boolean isStoreSalesmanOperator(String scopeType, Long scopeId, Long operatorId) {
        return "STORE".equals(scopeType)
                && scopeId != null
                && hasRoleInScope(operatorId, "SALESMAN", "STORE", scopeId);
    }

    /**
     * 校验采购入库操作权限。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    所属集团 ID
     * @param operatorId 操作人 ID
     * @param action     操作类型
     */
    public void ensureOperationPermission(String scopeType,
                                          Long scopeId,
                                          Long groupId,
                                          Long operatorId,
                                          String action) {
        if (canViewAll(scopeType, scopeId, groupId, operatorId)) {
            return;
        }
        if (!hasWorkflowOperationPermission(scopeType, scopeId, groupId, operatorId, action)) {
            throw new BusinessException("当前账号无采购入库操作权限");
        }
    }

    /**
     * 校验采购入库审核权限。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    所属集团 ID
     * @param operatorId 操作人 ID
     */
    public void ensureReviewPermission(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (!canReview(scopeType, scopeId, groupId, operatorId)) {
            throw new BusinessException("当前账号无采购入库审核权限");
        }
    }

    private boolean hasWorkflowOperationPermission(String scopeType,
                                                   Long scopeId,
                                                   Long groupId,
                                                   Long operatorId,
                                                   String action) {
        return purchaseInboundWorkflowService.hasBusinessOperationPermission(
                scopeType,
                scopeId,
                groupId,
                operatorId,
                action
        );
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
