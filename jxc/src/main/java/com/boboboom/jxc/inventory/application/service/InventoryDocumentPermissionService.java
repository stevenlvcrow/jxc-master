package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import org.springframework.stereotype.Service;

/**
 * 通用库存单据权限判断服务。
 */
@Service
public class InventoryDocumentPermissionService {

    private final WorkflowActionService workflowActionService;
    private final UserRoleRelRepository userRoleRelRepository;
    private final RoleRepository roleRepository;

    public InventoryDocumentPermissionService(WorkflowActionService workflowActionService,
                                              UserRoleRelRepository userRoleRelRepository,
                                              RoleRepository roleRepository) {
        this.workflowActionService = workflowActionService;
        this.userRoleRelRepository = userRoleRelRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * 计算页面权限。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @return 权限快照
     */
    public PermissionSnapshot resolvePermissions(InventoryDocumentType type,
                                                 String scopeType,
                                                 Long scopeId,
                                                 Long groupId,
                                                 Long operatorId) {
        boolean canManageAll = canViewAll(scopeType, scopeId, groupId, operatorId);
        if (!type.isWorkflowEnabled()) {
            return new PermissionSnapshot(true, true, true, false, false);
        }
        boolean canApprove = canReview(type, scopeType, scopeId, groupId, operatorId) || canManageAll;
        return new PermissionSnapshot(
                canManageAll || hasWorkflowOperationPermission(type, scopeType, scopeId, groupId, operatorId, "CREATE"),
                canManageAll || hasWorkflowOperationPermission(type, scopeType, scopeId, groupId, operatorId, "UPDATE"),
                canManageAll || hasWorkflowOperationPermission(type, scopeType, scopeId, groupId, operatorId, "DELETE"),
                canApprove,
                canApprove
        );
    }

    /**
     * 判断是否可查看全部。
     *
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
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
     * 判断是否可审核。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @return 是否可审核
     */
    public boolean canReview(InventoryDocumentType type,
                             String scopeType,
                             Long scopeId,
                             Long groupId,
                             Long operatorId) {
        return workflowActionService.hasConditionNodePermission(
                type.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                operatorId
        );
    }

    /**
     * 校验操作权限。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @param action     操作动作
     */
    public void ensureOperationPermission(InventoryDocumentType type,
                                          String scopeType,
                                          Long scopeId,
                                          Long groupId,
                                          Long operatorId,
                                          String action) {
        if (!type.isWorkflowEnabled()) {
            return;
        }
        if (canViewAll(scopeType, scopeId, groupId, operatorId)) {
            return;
        }
        if (!hasWorkflowOperationPermission(type, scopeType, scopeId, groupId, operatorId, action)) {
            throw new BusinessException("当前账号无" + type.getBusinessName() + "操作权限");
        }
    }

    /**
     * 校验审核权限。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     */
    public void ensureReviewPermission(InventoryDocumentType type,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       Long operatorId) {
        if (!type.isWorkflowEnabled()) {
            throw new BusinessException(type.getBusinessName() + "不支持审核");
        }
        if (!canReview(type, scopeType, scopeId, groupId, operatorId) && !canViewAll(scopeType, scopeId, groupId, operatorId)) {
            throw new BusinessException("当前账号无" + type.getBusinessName() + "审核权限");
        }
    }

    private boolean hasWorkflowOperationPermission(InventoryDocumentType type,
                                                   String scopeType,
                                                   Long scopeId,
                                                   Long groupId,
                                                   Long operatorId,
                                                   String action) {
        return workflowActionService.hasActionPermission(
                type.getBusinessCode(),
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

    /**
     * 通用权限快照。
     *
     * @param canCreate 是否可创建
     * @param canUpdate 是否可编辑
     * @param canDelete 是否可删除
     * @param canApprove 是否可审核
     * @param canUnapprove 是否可反审核
     */
    public record PermissionSnapshot(boolean canCreate,
                                     boolean canUpdate,
                                     boolean canDelete,
                                     boolean canApprove,
                                     boolean canUnapprove) {
    }
}
