package com.boboboom.jxc.inventory.application.service;

import org.springframework.stereotype.Service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.service.DataScopeAccessService;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;

/**
 * 通用库存单据权限判断服务。
 */
@Service
public class InventoryDocumentPermissionService {

    private final WorkflowActionService workflowActionService;
    private final DataScopeAccessService dataScopeAccessService;

    /** 库存服务，负责相关业务规则和流程协作。 */
    public InventoryDocumentPermissionService(WorkflowActionService workflowActionServiceValue,
                                              DataScopeAccessService dataScopeAccessServiceValue) {
        this.workflowActionService = workflowActionServiceValue;
        this.dataScopeAccessService = dataScopeAccessServiceValue;
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
        if (isSystemGeneratedInventoryCheckDocument(type)) {
            return new PermissionSnapshot(false, false, false, false, false);
        }
        boolean canManageAll = canViewAll(scopeType, scopeId, groupId, operatorId);
        if (!type.isWorkflowEnabled()) {
            boolean canApprove = type == InventoryDocumentType.WAREHOUSE_OPENING_BALANCE;
            return new PermissionSnapshot(true, true, true, canApprove, false);
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
        return dataScopeAccessService.canViewScopeData(scopeType, scopeId, groupId, operatorId);
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
        if (isSystemGeneratedInventoryCheckDocument(type)) {
            throw new BusinessException(type.getBusinessName() + "由盘点审核自动生成，不允许手工操作");
        }
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
        if (isSystemGeneratedInventoryCheckDocument(type)) {
            throw new BusinessException(type.getBusinessName() + "由盘点审核自动生成，不支持手工审核");
        }
        if (type == InventoryDocumentType.WAREHOUSE_OPENING_BALANCE) {
            return;
        }
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

    private boolean isSystemGeneratedInventoryCheckDocument(InventoryDocumentType type) {
        return type == InventoryDocumentType.PROFIT_INBOUND || type == InventoryDocumentType.LOSS_OUTBOUND;
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
