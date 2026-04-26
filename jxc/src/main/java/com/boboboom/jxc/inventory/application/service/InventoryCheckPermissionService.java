package com.boboboom.jxc.inventory.application.service;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;

/**
 * 盘点单权限判断服务。
 */
@Service
public class InventoryCheckPermissionService {

    private final WorkflowActionService workflowActionService;
    private final DataScopeAccessService dataScopeAccessService;

    /** 库存服务，负责相关业务规则和流程协作。 */
    public InventoryCheckPermissionService(WorkflowActionService workflowActionServiceValue,
                                           DataScopeAccessService dataScopeAccessServiceValue) {
        this.workflowActionService = workflowActionServiceValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
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
    public PermissionSnapshot resolvePermissions(InventoryCheckKind kind,
                                                 String scopeType,
                                                 Long scopeId,
                                                 Long groupId,
                                                 Long operatorId) {
        boolean canManageAll = canViewAll(scopeType, scopeId, groupId, operatorId);
        boolean canApprove = canReview(kind, scopeType, scopeId, groupId, operatorId) || canManageAll;
        return new PermissionSnapshot(
                canManageAll || hasWorkflowOperationPermission(kind, scopeType, scopeId, groupId, operatorId, "CREATE"),
                canManageAll || hasWorkflowOperationPermission(kind, scopeType, scopeId, groupId, operatorId, "UPDATE"),
                canManageAll || hasWorkflowOperationPermission(kind, scopeType, scopeId, groupId, operatorId, "DELETE"),
                canApprove,
                canApprove
        );
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
        return dataScopeAccessService.canViewScopeData(scopeType, scopeId, groupId, operatorId);
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
    public void ensureOperationPermission(InventoryCheckKind kind,
                                          String scopeType,
                                          Long scopeId,
                                          Long groupId,
                                          Long operatorId,
                                          String action) {
        if (canViewAll(scopeType, scopeId, groupId, operatorId)
                || hasWorkflowOperationPermission(kind, scopeType, scopeId, groupId, operatorId, action)) {
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
    public void ensureReviewPermission(InventoryCheckKind kind, String scopeType, Long scopeId, Long groupId, Long operatorId) {
        if (!canViewAll(scopeType, scopeId, groupId, operatorId)
                && !canReview(kind, scopeType, scopeId, groupId, operatorId)) {
            throw new BusinessException("当前账号无盘点单审核权限");
        }
    }

    /** 判断当前操作人是否可以审核。 */
    public boolean canReview(InventoryCheckKind kind,
                             String scopeType,
                             Long scopeId,
                             Long groupId,
                             Long operatorId) {
        return workflowActionService.hasConditionNodePermission(
                kind.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                operatorId
        );
    }

    private boolean hasWorkflowOperationPermission(InventoryCheckKind kind,
                                                   String scopeType,
                                                   Long scopeId,
                                                   Long groupId,
                                                   Long operatorId,
                                                   String action) {
        return workflowActionService.hasActionPermission(
                kind.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                operatorId,
                action
        );
    }

    /** 库存快照模型，承载权限快照查询结果。 */
    public record PermissionSnapshot(boolean canCreate,
                                     boolean canUpdate,
                                     boolean canDelete,
                                     boolean canApprove,
                                     boolean canUnapprove) {
    }
}
