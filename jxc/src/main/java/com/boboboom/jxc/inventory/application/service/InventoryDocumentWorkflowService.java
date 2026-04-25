package com.boboboom.jxc.inventory.application.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.application.service.DictionaryLookupService;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.domain.repository.PurchaseInboundRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.application.service.WorkflowBindingResolverService;

/**
 * 通用库存单据流程协作服务。
 */
@Service
public class InventoryDocumentWorkflowService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final String NODE_TYPE_SUCCESS = "SUCCESS";
    private static final String NODE_TYPE_FAIL = "FAIL";
    private static final String NODE_TYPE_END = "END";
    private static final int WORKFLOW_AUTO_ADVANCE_LIMIT = 5;

    private final WorkflowBindingResolverService workflowBindingResolverService;
    private final WorkflowActionService workflowActionService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final InventoryDocumentRepository inventoryDocumentRepository;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final DictionaryLookupService dictionaryLookupService;

    /** 库存单据流程协作服务，负责审批流程启动、推进、撤销和节点状态同步。 */
    public InventoryDocumentWorkflowService(WorkflowBindingResolverService workflowBindingResolverServiceValue,
                                            WorkflowActionService workflowActionServiceValue,
                                            RepositoryService repositoryServiceValue,
                                            RuntimeService runtimeServiceValue,
                                            TaskService taskServiceValue,
                                            InventoryDocumentRepository inventoryDocumentRepositoryValue,
                                            PurchaseInboundRepository purchaseInboundRepositoryValue,
                                            DictionaryLookupService dictionaryLookupServiceValue) {
        this.workflowBindingResolverService = workflowBindingResolverServiceValue;
        this.workflowActionService = workflowActionServiceValue;
        this.repositoryService = repositoryServiceValue;
        this.runtimeService = runtimeServiceValue;
        this.taskService = taskServiceValue;
        this.inventoryDocumentRepository = inventoryDocumentRepositoryValue;
        this.purchaseInboundRepository = purchaseInboundRepositoryValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 判断当前操作人是否具备业务动作权限。 */
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

    /** 判断当前操作人是否具备业务审核权限。 */
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

    /** 判断当前业务动作是否需要触发审批流程。 */
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

    /** 判断当前业务动作是否需要触发审批流程。 */
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

    /** 判断当前操作人是否具备业务动作权限。 */
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

    /** 判断当前操作人是否具备业务审核权限。 */
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

    /** 解析流程通知和审批记录使用的业务名称。 */
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

    /** 解析流程通知和审批记录使用的业务名称。 */
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

    /** 在库存单据执行动作后同步流程状态。 */
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

    /** 在采购入库单执行动作后同步流程状态。 */
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

    /** 按业务编码同步单据审批流程状态。 */
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

    /** 完成通用库存单据当前审批任务。 */
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

    /** 完成采购入库单当前审批任务。 */
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

    /** 按业务编码完成当前审批任务。 */
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

    /** 重置通用库存单据流程状态。 */
    public void resetWorkflowState(InventoryDocumentType type, InventoryDocumentHeader header) {
        resetWorkflowStateInternal(
                header,
                () -> inventoryDocumentRepository.updateHeader(type, header),
                "库存单据删除"
        );
    }

    /** 重置采购入库单流程状态。 */
    public void resetPurchaseInboundWorkflowState(PurchaseInboundDO header) {
        InventoryDocumentHeader workflowHeader = PurchaseInboundWorkflowBridge.toHeader(header);
        resetWorkflowStateInternal(
                workflowHeader,
                () -> persistPurchaseInboundHeader(header, workflowHeader),
                "采购入库单删除"
        );
        PurchaseInboundWorkflowBridge.applyHeader(header, workflowHeader);
    }

    /** 按业务编码重置流程状态。 */
    public void resetBusinessWorkflowState(InventoryDocumentHeader header,
                                           Runnable persistAction,
                                           String deleteReason) {
        resetWorkflowStateInternal(header, persistAction, deleteReason);
    }

    /** 取消运行中的通用库存单据流程实例。 */
    public void cancelWorkflowInstanceIfRunning(InventoryDocumentHeader header) {
        cancelWorkflowInstanceIfRunning(header, "库存单据删除");
    }

    /** 取消运行中的采购入库流程实例。 */
    public void cancelPurchaseInboundWorkflowInstanceIfRunning(PurchaseInboundDO header) {
        cancelWorkflowInstanceIfRunning(PurchaseInboundWorkflowBridge.toHeader(header), "采购入库单删除");
    }

    /** 解析审批节点对应的角色展示名称。 */
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

    /** 解析审批节点对应的角色展示名称。 */
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

    /** 解析审批节点对应的审批目标。 */
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

    /** 解析审批节点对应的审批目标。 */
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
        autoCompleteTerminalTasks(businessCode, header, nextInstance, operatorId, groupId);
        nextInstance = findActiveInstance(businessKey);
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
        autoCompleteTerminalTasks(businessCode, header, findActiveInstance(businessKey), operatorId, groupId);
        if (!StringUtils.hasText(header.getWorkflowTaskId())) {
            if (findActiveInstance(businessKey) == null) {
                header.setWorkflowStatus(workflowCompletedStatus());
            } else {
                runtimeService.deleteProcessInstance(instance.getId(), workflowLabel + "未配置审批节点");
                throw new BusinessException(workflowLabel + "未配置审批节点");
            }
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
        for (int i = 0; i < WORKFLOW_AUTO_ADVANCE_LIMIT; i++) {
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

    private void autoCompleteTerminalTasks(String businessCode,
                                           InventoryDocumentHeader header,
                                           ProcessInstance instance,
                                           Long operatorId,
                                           Long groupId) {
        if (instance == null) {
            return;
        }
        for (int i = 0; i < WORKFLOW_AUTO_ADVANCE_LIMIT; i++) {
            refreshCurrentTask(header, instance.getId());
            if (!StringUtils.hasText(header.getWorkflowTaskId())
                    || !isTerminalTask(businessCode, header, groupId)) {
                return;
            }
            taskService.complete(header.getWorkflowTaskId(), Map.of(
                    "operatorId", operatorId,
                    "businessId", header.getId(),
                    "businessCode", businessCode,
                    "documentCode", header.getDocumentCode()
            ));
            if (runtimeService.createProcessInstanceQuery()
                    .processInstanceId(instance.getId())
                    .singleResult() == null) {
                header.setWorkflowStatus(workflowCompletedStatus());
                header.setWorkflowTaskId(null);
                header.setWorkflowTaskName(null);
                return;
            }
        }
        throw new BusinessException("流程结束节点自动推进次数过多，请检查流程配置");
    }

    private boolean isTerminalTask(String businessCode, InventoryDocumentHeader header, Long groupId) {
        if (!StringUtils.hasText(header.getWorkflowTaskName())) {
            return false;
        }
        return workflowActionService.resolveNodeType(
                        businessCode,
                        header.getScopeType(),
                        header.getScopeId(),
                        groupId,
                        header.getWorkflowTaskName()
                )
                .map(this::isTerminalNodeType)
                .orElse(false);
    }

    private boolean isTerminalNodeType(String nodeType) {
        if (!StringUtils.hasText(nodeType)) {
            return false;
        }
        String normalized = nodeType.trim().toUpperCase(Locale.ROOT);
        return NODE_TYPE_SUCCESS.equals(normalized)
                || NODE_TYPE_FAIL.equals(normalized)
                || NODE_TYPE_END.equals(normalized);
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

    /** 库存结果模型，承载业务处理结果。 */
    public record ApprovalResult(boolean workflowApplied, boolean completed) {
        /** 构造不启用流程时的审核结果。 */
        public static ApprovalResult legacy() {
            return new ApprovalResult(false, true);
        }

        /** 构造流程待审批状态的审核结果。 */
        public static ApprovalResult workflowPending() {
            return new ApprovalResult(true, false);
        }

        /** 构造流程已完成状态的审核结果。 */
        public static ApprovalResult workflowCompleted() {
            return new ApprovalResult(true, true);
        }
    }
}
