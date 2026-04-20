package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowBindingResolverService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 通用库存单据流程协作服务。
 */
@Service
public class InventoryDocumentWorkflowService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String WORKFLOW_STATUS_NONE = "NONE";
    private static final String WORKFLOW_STATUS_RUNNING = "RUNNING";
    private static final String WORKFLOW_STATUS_COMPLETED = "COMPLETED";
    private static final String WORKFLOW_STATUS_REVOKED = "REVOKED";
    private static final String PENDING_OPERATION_NONE = "NONE";

    private final WorkflowBindingResolverService workflowBindingResolverService;
    private final WorkflowActionService workflowActionService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;

    public InventoryDocumentWorkflowService(WorkflowBindingResolverService workflowBindingResolverService,
                                            WorkflowActionService workflowActionService,
                                            RepositoryService repositoryService,
                                            RuntimeService runtimeService,
                                            TaskService taskService,
                                            InventoryDocumentRepository inventoryDocumentRepository,
                                            PurchaseInboundRepository purchaseInboundRepository) {
        this.workflowBindingResolverService = workflowBindingResolverService;
        this.workflowActionService = workflowActionService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.inventoryDocumentRepository = inventoryDocumentRepository;
        this.purchaseInboundRepository = purchaseInboundRepository;
    }

    public boolean hasBusinessOperationPermission(InventoryDocumentType type,
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

    public boolean hasBusinessReviewPermission(InventoryDocumentType type,
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

    public boolean shouldTriggerAction(InventoryDocumentType type,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       String action) {
        if (!type.isWorkflowEnabled()) {
            return false;
        }
        return workflowActionService.shouldTriggerAction(
                type.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                action
        );
    }

    public String resolveBusinessName(InventoryDocumentType type,
                                      String scopeType,
                                      Long scopeId,
                                      Long groupId) {
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding = resolveBinding(type, scopeType, scopeId, groupId, false);
        if (binding.isEmpty() || !StringUtils.hasText(binding.get().businessName())) {
            return type.getBusinessName();
        }
        return binding.get().businessName();
    }

    public boolean syncOnAction(InventoryDocumentType type,
                                String scopeType,
                                Long scopeId,
                                Long groupId,
                                InventoryDocumentHeader header,
                                Long operatorId,
                                String action) {
        return syncInternal(
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                scopeType,
                scopeId,
                groupId,
                header,
                operatorId,
                action,
                () -> inventoryDocumentRepository.updateHeader(type, header),
                "库存单据删除"
        );
    }

    public boolean syncPurchaseInboundOnAction(String scopeType,
                                               Long scopeId,
                                               Long groupId,
                                               PurchaseInboundDO header,
                                               Long operatorId,
                                               String action) {
        InventoryDocumentHeader workflowHeader = PurchaseInboundWorkflowBridge.toHeader(header);
        boolean applied = syncInternal(
                InventoryDocumentType.PURCHASE_INBOUND.getBusinessCode(),
                "采购入库流程",
                scopeType,
                scopeId,
                groupId,
                workflowHeader,
                operatorId,
                action,
                () -> persistPurchaseInboundHeader(header, workflowHeader),
                "采购入库单删除"
        );
        PurchaseInboundWorkflowBridge.applyHeader(header, workflowHeader);
        return applied;
    }

    public ApprovalResult completeCurrentTask(InventoryDocumentType type,
                                              InventoryDocumentHeader header,
                                              Long operatorId) {
        return completeCurrentTaskInternal(
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                header,
                operatorId,
                () -> inventoryDocumentRepository.updateHeader(type, header)
        );
    }

    public ApprovalResult completePurchaseInboundCurrentTask(PurchaseInboundDO header, Long operatorId) {
        InventoryDocumentHeader workflowHeader = PurchaseInboundWorkflowBridge.toHeader(header);
        ApprovalResult result = completeCurrentTaskInternal(
                InventoryDocumentType.PURCHASE_INBOUND.getBusinessCode(),
                "采购入库流程",
                workflowHeader,
                operatorId,
                () -> persistPurchaseInboundHeader(header, workflowHeader)
        );
        PurchaseInboundWorkflowBridge.applyHeader(header, workflowHeader);
        return result;
    }

    public void resetWorkflowState(InventoryDocumentType type, InventoryDocumentHeader header) {
        resetWorkflowStateInternal(
                header,
                () -> inventoryDocumentRepository.updateHeader(type, header),
                "库存单据删除"
        );
    }

    public void resetPurchaseInboundWorkflowState(PurchaseInboundDO header) {
        InventoryDocumentHeader workflowHeader = PurchaseInboundWorkflowBridge.toHeader(header);
        resetWorkflowStateInternal(
                workflowHeader,
                () -> persistPurchaseInboundHeader(header, workflowHeader),
                "采购入库单删除"
        );
        PurchaseInboundWorkflowBridge.applyHeader(header, workflowHeader);
    }

    public void cancelWorkflowInstanceIfRunning(InventoryDocumentHeader header) {
        cancelWorkflowInstanceIfRunning(header, "库存单据删除");
    }

    public void cancelPurchaseInboundWorkflowInstanceIfRunning(PurchaseInboundDO header) {
        cancelWorkflowInstanceIfRunning(PurchaseInboundWorkflowBridge.toHeader(header), "采购入库单删除");
    }

    public String resolveApprovalRoleLabel(InventoryDocumentType type,
                                           String scopeType,
                                           Long scopeId,
                                           Long groupId,
                                           Long operatorId,
                                           String taskName) {
        return workflowActionService.resolveApprovalRoleLabel(
                type.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                operatorId,
                taskName
        );
    }

    public Optional<WorkflowActionService.ApprovalTarget> resolveApprovalTarget(InventoryDocumentType type,
                                                                                String scopeType,
                                                                                Long scopeId,
                                                                                Long groupId,
                                                                                String taskName) {
        return workflowActionService.resolveApprovalTarget(
                type.getBusinessCode(),
                scopeType,
                scopeId,
                groupId,
                taskName
        );
    }

    private boolean syncInternal(String businessCode,
                                 String workflowLabel,
                                 String scopeType,
                                 Long scopeId,
                                 Long groupId,
                                 InventoryDocumentHeader header,
                                 Long operatorId,
                                 String action,
                                 Runnable persistAction,
                                 String deleteReason) {
        if (!workflowActionService.shouldTriggerAction(businessCode, scopeType, scopeId, groupId, action)) {
            cancelWorkflowInstanceIfRunning(header, deleteReason);
            clearWorkflowState(header);
            persistAction.run();
            return false;
        }
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding =
                resolveBinding(businessCode, workflowLabel, scopeType, scopeId, groupId, false);
        if (binding.isEmpty()) {
            return false;
        }
        String businessKey = businessKey(businessCode, header.getId());
        ProcessInstance activeInstance = findActiveInstance(businessKey);
        if (activeInstance == null) {
            startInstance(businessCode, workflowLabel, header, binding.get(), businessKey, operatorId, action, persistAction);
            return true;
        }
        header.setWorkflowProcessCode(binding.get().processCode());
        header.setWorkflowDefinitionKey(binding.get().processDefinitionKey());
        header.setWorkflowDefinitionId(binding.get().processDefinitionId());
        header.setWorkflowInstanceId(activeInstance.getId());
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setPendingOperation(StringUtils.hasText(action) ? action : PENDING_OPERATION_NONE);
        refreshCurrentTask(header, activeInstance.getId());
        persistAction.run();
        return true;
    }

    private ApprovalResult completeCurrentTaskInternal(String businessCode,
                                                       String workflowLabel,
                                                       InventoryDocumentHeader header,
                                                       Long operatorId,
                                                       Runnable persistAction) {
        String businessKey = businessKey(businessCode, header.getId());
        ProcessInstance activeInstance = findActiveInstance(businessKey);
        if (activeInstance == null) {
            if (hasWorkflowMetadata(header)) {
                throw new BusinessException("流程实例不存在，请重新保存单据后再审批");
            }
            return ApprovalResult.legacy();
        }
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(activeInstance.getId())
                .orderByTaskCreateTime()
                .asc()
                .list();
        if (tasks.isEmpty()) {
            throw new BusinessException("当前流程暂无待办任务");
        }
        if (tasks.size() > 1) {
            throw new BusinessException("当前流程存在多个待办任务，请联系管理员");
        }
        Task task = tasks.get(0);
        taskService.complete(task.getId(), Map.of(
                "operatorId", operatorId,
                "businessId", header.getId(),
                "businessCode", businessCode,
                "documentCode", header.getDocumentCode()
        ));
        ProcessInstance nextInstance = findActiveInstance(businessKey);
        if (nextInstance == null) {
            header.setWorkflowStatus(WORKFLOW_STATUS_COMPLETED);
            header.setWorkflowInstanceId(activeInstance.getId());
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            persistAction.run();
            return ApprovalResult.workflowCompleted();
        }
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding =
                resolveBinding(businessCode, workflowLabel, header.getScopeType(), header.getScopeId(), resolveGroupId(header), true);
        header.setWorkflowProcessCode(binding.map(WorkflowBindingResolverService.ResolvedWorkflowBinding::processCode).orElse(businessCode));
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setWorkflowInstanceId(nextInstance.getId());
        refreshCurrentTask(header, nextInstance.getId());
        persistAction.run();
        return ApprovalResult.workflowPending();
    }

    private void resetWorkflowStateInternal(InventoryDocumentHeader header,
                                           Runnable persistAction,
                                           String deleteReason) {
        cancelWorkflowInstanceIfRunning(header, deleteReason);
        header.setWorkflowDefinitionKey(null);
        header.setWorkflowDefinitionId(null);
        header.setWorkflowInstanceId(null);
        header.setWorkflowTaskId(null);
        header.setWorkflowTaskName(null);
        header.setWorkflowStatus(WORKFLOW_STATUS_REVOKED);
        header.setPendingOperation(PENDING_OPERATION_NONE);
        persistAction.run();
    }

    private void startInstance(String businessCode,
                               String workflowLabel,
                               InventoryDocumentHeader header,
                               WorkflowBindingResolverService.ResolvedWorkflowBinding binding,
                               String businessKey,
                               Long operatorId,
                               String action,
                               Runnable persistAction) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(binding.processDefinitionKey())
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new BusinessException(workflowLabel + "模板未发布，请先发布流程");
        }
        header.setWorkflowProcessCode(binding.processCode());
        header.setWorkflowDefinitionKey(binding.processDefinitionKey());
        header.setWorkflowDefinitionId(binding.processDefinitionId());
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setPendingOperation(StringUtils.hasText(action) ? action : PENDING_OPERATION_NONE);
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                processDefinition.getKey(),
                businessKey,
                Map.of(
                        "businessId", header.getId(),
                        "businessCode", businessCode,
                        "documentCode", header.getDocumentCode(),
                        "operatorId", operatorId
                )
        );
        header.setWorkflowInstanceId(instance.getId());
        refreshCurrentTask(header, instance.getId());
        if (!StringUtils.hasText(header.getWorkflowTaskId())) {
            runtimeService.deleteProcessInstance(instance.getId(), workflowLabel + "未配置审批节点");
            throw new BusinessException(workflowLabel + "未配置审批节点");
        }
        persistAction.run();
    }

    private void clearWorkflowState(InventoryDocumentHeader header) {
        header.setWorkflowProcessCode(null);
        header.setWorkflowDefinitionKey(null);
        header.setWorkflowDefinitionId(null);
        header.setWorkflowInstanceId(null);
        header.setWorkflowTaskId(null);
        header.setWorkflowTaskName(null);
        header.setWorkflowStatus(WORKFLOW_STATUS_NONE);
        header.setPendingOperation(PENDING_OPERATION_NONE);
    }

    private void refreshCurrentTask(InventoryDocumentHeader header, String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .orderByTaskCreateTime()
                .asc()
                .list();
        if (tasks.isEmpty()) {
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            return;
        }
        if (tasks.size() > 1) {
            throw new BusinessException("当前流程存在多个待办任务，请联系管理员");
        }
        Task task = tasks.get(0);
        header.setWorkflowTaskId(task.getId());
        header.setWorkflowTaskName(task.getName());
    }

    private ProcessInstance findActiveInstance(String businessKey) {
        return runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();
    }

    private Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> resolveBinding(InventoryDocumentType type,
                                                                                            String scopeType,
                                                                                            Long scopeId,
                                                                                            Long groupId,
                                                                                            boolean allowMissing) {
        return resolveBinding(type.getBusinessCode(), type.getBusinessName() + "流程", scopeType, scopeId, groupId, allowMissing);
    }

    private Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> resolveBinding(String businessCode,
                                                                                            String workflowLabel,
                                                                                            String scopeType,
                                                                                            Long scopeId,
                                                                                            Long groupId,
                                                                                            boolean allowMissing) {
        try {
            return workflowBindingResolverService.resolvePublishedBinding(scopeType, scopeId, groupId, businessCode, workflowLabel);
        } catch (BusinessException ex) {
            if (allowMissing) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    private Long resolveGroupId(InventoryDocumentHeader header) {
        return SCOPE_GROUP.equalsIgnoreCase(header.getScopeType()) ? header.getScopeId() : null;
    }

    private String businessKey(String businessCode, Long businessId) {
        return businessCode + ":" + businessId;
    }

    private void cancelWorkflowInstanceIfRunning(InventoryDocumentHeader header, String deleteReason) {
        if (header == null || !StringUtils.hasText(header.getWorkflowInstanceId())) {
            return;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(header.getWorkflowInstanceId())
                .singleResult();
        if (instance != null) {
            runtimeService.deleteProcessInstance(instance.getId(), deleteReason);
        }
    }

    private boolean hasWorkflowMetadata(InventoryDocumentHeader header) {
        return StringUtils.hasText(header.getWorkflowInstanceId())
                || StringUtils.hasText(header.getWorkflowProcessCode())
                || StringUtils.hasText(header.getWorkflowDefinitionKey())
                || StringUtils.hasText(header.getWorkflowDefinitionId())
                || StringUtils.hasText(header.getWorkflowTaskId())
                || StringUtils.hasText(header.getWorkflowTaskName())
                || (StringUtils.hasText(header.getWorkflowStatus()) && !WORKFLOW_STATUS_NONE.equals(header.getWorkflowStatus()));
    }

    private void persistPurchaseInboundHeader(PurchaseInboundDO target, InventoryDocumentHeader header) {
        PurchaseInboundWorkflowBridge.applyHeader(target, header);
        purchaseInboundRepository.update(target);
    }

    public record ApprovalResult(boolean workflowApplied, boolean completed) {
        public static ApprovalResult legacy() {
            return new ApprovalResult(false, true);
        }

        public static ApprovalResult workflowPending() {
            return new ApprovalResult(true, false);
        }

        public static ApprovalResult workflowCompleted() {
            return new ApprovalResult(true, true);
        }
    }
}
