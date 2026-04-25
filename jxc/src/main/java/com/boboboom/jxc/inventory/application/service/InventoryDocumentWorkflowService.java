package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
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
    private static final String PENDING_OPERATION_NONE = "NONE";

    private final WorkflowBindingResolverService workflowBindingResolverService;
    private final WorkflowActionService workflowActionService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final DictionaryLookupService dictionaryLookupService;

    public InventoryDocumentWorkflowService(WorkflowBindingResolverService workflowBindingResolverService,
                                            WorkflowActionService workflowActionService,
                                            RepositoryService repositoryService,
                                            RuntimeService runtimeService,
                                            TaskService taskService,
                                            InventoryDocumentRepository inventoryDocumentRepository,
                                            PurchaseInboundRepository purchaseInboundRepository,
                                            DictionaryLookupService dictionaryLookupService) {
        this.workflowBindingResolverService = workflowBindingResolverService;
        this.workflowActionService = workflowActionService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.inventoryDocumentRepository = inventoryDocumentRepository;
        this.purchaseInboundRepository = purchaseInboundRepository;
        this.dictionaryLookupService = dictionaryLookupService;
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

    public boolean shouldTriggerAction(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       String action) {
        return workflowActionService.shouldTriggerAction(
                businessCode,
                scopeType,
                scopeId,
                groupId,
                action
        );
    }

    public boolean hasBusinessOperationPermission(String businessCode,
                                                  String scopeType,
                                                  Long scopeId,
                                                  Long groupId,
                                                  Long operatorId,
                                                  String action) {
        return workflowActionService.hasActionPermission(
                businessCode,
                scopeType,
                scopeId,
                groupId,
                operatorId,
                action
        );
    }

    public boolean hasBusinessReviewPermission(String businessCode,
                                               String scopeType,
                                               Long scopeId,
                                               Long groupId,
                                               Long operatorId) {
        return workflowActionService.hasConditionNodePermission(
                businessCode,
                scopeType,
                scopeId,
                groupId,
                operatorId
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

    public String resolveBusinessName(String businessCode,
                                      String defaultBusinessName,
                                      String scopeType,
                                      Long scopeId,
                                      Long groupId) {
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding =
                resolveBinding(businessCode, defaultBusinessName + "流程", scopeType, scopeId, groupId, false);
        if (binding.isEmpty() || !StringUtils.hasText(binding.get().businessName())) {
            return defaultBusinessName;
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

    public boolean syncBusinessOnAction(String businessCode,
                                        String workflowLabel,
                                        String scopeType,
                                        Long scopeId,
                                        Long groupId,
                                        InventoryDocumentHeader header,
                                        Long operatorId,
                                        String action,
                                        Runnable persistAction,
                                        String deleteReason,
                                        boolean requireWorkflow) {
        if (requireWorkflow && !workflowActionService.shouldTriggerAction(businessCode, scopeType, scopeId, groupId, action)) {
            throw new BusinessException(workflowLabel + "未配置" + actionLabel(action) + "审批节点，请先在流程模板中配置触发动作");
        }
        return syncInternal(
                businessCode,
                workflowLabel,
                scopeType,
                scopeId,
                groupId,
                header,
                operatorId,
                action,
                persistAction,
                deleteReason
        );
    }

    public ApprovalResult completeCurrentTask(InventoryDocumentType type,
                                              InventoryDocumentHeader header,
                                              Long operatorId) {
        return completeCurrentTaskInternal(
                type.getBusinessCode(),
                type.getBusinessName() + "流程",
                header,
                operatorId,
                resolveGroupId(header),
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
                resolveGroupId(workflowHeader),
                () -> persistPurchaseInboundHeader(header, workflowHeader)
        );
        PurchaseInboundWorkflowBridge.applyHeader(header, workflowHeader);
        return result;
    }

    public ApprovalResult completeBusinessCurrentTask(String businessCode,
                                                      String workflowLabel,
                                                      InventoryDocumentHeader header,
                                                      Long operatorId,
                                                      Long groupId,
                                                      Runnable persistAction) {
        return completeCurrentTaskInternal(
                businessCode,
                workflowLabel,
                header,
                operatorId,
                groupId,
                persistAction
        );
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

    public void resetBusinessWorkflowState(InventoryDocumentHeader header,
                                           Runnable persistAction,
                                           String deleteReason) {
        resetWorkflowStateInternal(header, persistAction, deleteReason);
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

    public String resolveApprovalRoleLabel(String businessCode,
                                           String scopeType,
                                           Long scopeId,
                                           Long groupId,
                                           Long operatorId,
                                           String taskName) {
        return workflowActionService.resolveApprovalRoleLabel(
                businessCode,
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

    public Optional<WorkflowActionService.ApprovalTarget> resolveApprovalTarget(String businessCode,
                                                                                String scopeType,
                                                                                Long scopeId,
                                                                                Long groupId,
                                                                                String taskName) {
        return workflowActionService.resolveApprovalTarget(
                businessCode,
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
            startInstance(businessCode, workflowLabel, header, binding.get(), businessKey, operatorId, action, groupId, persistAction);
            return true;
        }
        header.setWorkflowProcessCode(binding.get().processCode());
        header.setWorkflowDefinitionKey(binding.get().processDefinitionKey());
        header.setWorkflowDefinitionId(binding.get().processDefinitionId());
        header.setWorkflowInstanceId(activeInstance.getId());
        header.setWorkflowStatus(workflowRunningStatus());
        header.setPendingOperation(StringUtils.hasText(action) ? action : PENDING_OPERATION_NONE);
        refreshCurrentTask(header, activeInstance.getId());
        autoCompleteActionTriggerTasks(businessCode, header, activeInstance.getId(), operatorId, action, groupId);
        persistAction.run();
        return true;
    }

    private ApprovalResult completeCurrentTaskInternal(String businessCode,
                                                       String workflowLabel,
                                                       InventoryDocumentHeader header,
                                                       Long operatorId,
                                                       Long groupId,
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
            header.setWorkflowStatus(workflowCompletedStatus());
            header.setWorkflowInstanceId(activeInstance.getId());
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            persistAction.run();
            return ApprovalResult.workflowCompleted();
        }
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding =
                resolveBinding(businessCode, workflowLabel, header.getScopeType(), header.getScopeId(), groupId, true);
        header.setWorkflowProcessCode(binding.map(WorkflowBindingResolverService.ResolvedWorkflowBinding::processCode).orElse(businessCode));
        header.setWorkflowStatus(workflowRunningStatus());
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
        header.setWorkflowStatus(workflowRevokedStatus());
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
                               Long groupId,
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
        header.setWorkflowStatus(workflowRunningStatus());
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
        autoCompleteActionTriggerTasks(businessCode, header, instance.getId(), operatorId, action, groupId);
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
        header.setWorkflowStatus(workflowNoneStatus());
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

    private void autoCompleteActionTriggerTasks(String businessCode,
                                                InventoryDocumentHeader header,
                                                String processInstanceId,
                                                Long operatorId,
                                                String action,
                                                Long groupId) {
        if (!StringUtils.hasText(processInstanceId) || !StringUtils.hasText(action)) {
            return;
        }
        for (int i = 0; i < 5; i++) {
            if (!StringUtils.hasText(header.getWorkflowTaskId())
                    || !workflowActionService.isActionTriggerTask(
                            businessCode,
                            header.getScopeType(),
                            header.getScopeId(),
                            groupId,
                            operatorId,
                            header.getWorkflowTaskName(),
                            action
            )) {
                return;
            }
            taskService.complete(header.getWorkflowTaskId(), Map.of(
                    "operatorId", operatorId,
                    "businessId", header.getId(),
                    "businessCode", businessCode,
                    "documentCode", header.getDocumentCode()
            ));
            if (runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId)
                    .singleResult() == null) {
                header.setWorkflowStatus(workflowCompletedStatus());
                header.setWorkflowTaskId(null);
                header.setWorkflowTaskName(null);
                return;
            }
            refreshCurrentTask(header, processInstanceId);
        }
        throw new BusinessException("流程动作节点自动推进次数过多，请检查流程配置");
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

    private String actionLabel(String action) {
        if ("CREATE".equalsIgnoreCase(action)) {
            return "新增";
        }
        if ("UPDATE".equalsIgnoreCase(action)) {
            return "修改";
        }
        if ("DELETE".equalsIgnoreCase(action)) {
            return "删除";
        }
        return StringUtils.hasText(action) ? action : "业务";
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
                || (StringUtils.hasText(header.getWorkflowStatus()) && !workflowNoneStatus().equals(header.getWorkflowStatus()));
    }

    private void persistPurchaseInboundHeader(PurchaseInboundDO target, InventoryDocumentHeader header) {
        PurchaseInboundWorkflowBridge.applyHeader(target, header);
        purchaseInboundRepository.update(target);
    }

    private String workflowNoneStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.NONE);
    }

    private String workflowRunningStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.RUNNING);
    }

    private String workflowCompletedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.COMPLETED);
    }

    private String workflowRevokedStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.INVENTORY_WORKFLOW_STATUS, DictionaryCodes.REVOKED);
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
