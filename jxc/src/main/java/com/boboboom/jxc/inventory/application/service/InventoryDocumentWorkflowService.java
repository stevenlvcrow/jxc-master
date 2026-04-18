package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.workflow.application.service.WorkflowActionService;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
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

    private static final String WORKFLOW_STATUS_NONE = "NONE";
    private static final String WORKFLOW_STATUS_RUNNING = "RUNNING";
    private static final String WORKFLOW_STATUS_COMPLETED = "COMPLETED";
    private static final String WORKFLOW_STATUS_REVOKED = "REVOKED";
    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final WorkflowProcessRegistryRepository processRegistryRepository;
    private final WorkflowDefinitionConfigRepository definitionConfigRepository;
    private final WorkflowActionService workflowActionService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final InventoryDocumentRepository inventoryDocumentRepository;

    public InventoryDocumentWorkflowService(WorkflowProcessRegistryRepository processRegistryRepository,
                                            WorkflowDefinitionConfigRepository definitionConfigRepository,
                                            WorkflowActionService workflowActionService,
                                            RepositoryService repositoryService,
                                            RuntimeService runtimeService,
                                            TaskService taskService,
                                            InventoryDocumentRepository inventoryDocumentRepository) {
        this.processRegistryRepository = processRegistryRepository;
        this.definitionConfigRepository = definitionConfigRepository;
        this.workflowActionService = workflowActionService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.inventoryDocumentRepository = inventoryDocumentRepository;
    }

    /**
     * 判断业务动作权限。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @param action     动作
     * @return 是否允许
     */
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

    /**
     * 判断业务审核权限。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @return 是否允许
     */
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

    /**
     * 判断动作是否需要走流程。
     *
     * @param type      业务类型
     * @param scopeType 作用域类型
     * @param scopeId   作用域 ID
     * @param groupId   集团 ID
     * @param action    动作
     * @return 是否触发流程
     */
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

    /**
     * 同步流程状态。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param header     单据主表
     * @param operatorId 操作人 ID
     * @param action     动作
     * @return 是否应用流程
     */
    public boolean syncOnAction(InventoryDocumentType type,
                                String scopeType,
                                Long scopeId,
                                Long groupId,
                                InventoryDocumentHeader header,
                                Long operatorId,
                                String action) {
        if (!type.isWorkflowEnabled()) {
            return false;
        }
        if (!shouldTriggerAction(type, scopeType, scopeId, groupId, action)) {
            cancelWorkflowInstanceIfRunning(header);
            header.setWorkflowProcessCode(null);
            header.setWorkflowDefinitionKey(null);
            header.setWorkflowDefinitionId(null);
            header.setWorkflowInstanceId(null);
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            header.setWorkflowStatus(WORKFLOW_STATUS_NONE);
            header.setPendingOperation(PENDING_OPERATION_NONE);
            inventoryDocumentRepository.updateHeader(type, header);
            return false;
        }
        WorkflowBinding binding = resolveBinding(type, scopeType, scopeId, groupId).orElse(null);
        if (binding == null) {
            return false;
        }
        String businessKey = businessKey(type, header.getId());
        ProcessInstance activeInstance = findActiveInstance(businessKey);
        if (activeInstance == null) {
            startInstance(type, header, binding, businessKey, operatorId, action);
            return true;
        }
        header.setWorkflowProcessCode(binding.processCode());
        header.setWorkflowDefinitionKey(binding.processDefinitionKey());
        header.setWorkflowDefinitionId(binding.processDefinitionId());
        header.setWorkflowInstanceId(activeInstance.getId());
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setPendingOperation(StringUtils.hasText(action) ? action : PENDING_OPERATION_NONE);
        refreshCurrentTask(header, activeInstance.getId());
        inventoryDocumentRepository.updateHeader(type, header);
        return true;
    }

    /**
     * 完成当前待办。
     *
     * @param type       业务类型
     * @param header     单据主表
     * @param operatorId 操作人 ID
     * @return 审批结果
     */
    public ApprovalResult completeCurrentTask(InventoryDocumentType type,
                                              InventoryDocumentHeader header,
                                              Long operatorId) {
        String businessKey = businessKey(type, header.getId());
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
                "businessCode", type.getBusinessCode(),
                "documentCode", header.getDocumentCode()
        ));
        ProcessInstance nextInstance = findActiveInstance(businessKey);
        if (nextInstance == null) {
            header.setWorkflowStatus(WORKFLOW_STATUS_COMPLETED);
            header.setWorkflowInstanceId(activeInstance.getId());
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            inventoryDocumentRepository.updateHeader(type, header);
            return ApprovalResult.workflowCompleted();
        }
        header.setWorkflowProcessCode(type.getBusinessCode());
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setWorkflowInstanceId(nextInstance.getId());
        refreshCurrentTask(header, nextInstance.getId());
        inventoryDocumentRepository.updateHeader(type, header);
        return ApprovalResult.workflowPending();
    }

    /**
     * 重置流程状态。
     *
     * @param type   业务类型
     * @param header 单据主表
     */
    public void resetWorkflowState(InventoryDocumentType type, InventoryDocumentHeader header) {
        cancelWorkflowInstanceIfRunning(header);
        header.setWorkflowDefinitionKey(null);
        header.setWorkflowDefinitionId(null);
        header.setWorkflowInstanceId(null);
        header.setWorkflowTaskId(null);
        header.setWorkflowTaskName(null);
        header.setWorkflowStatus(WORKFLOW_STATUS_REVOKED);
        header.setPendingOperation(PENDING_OPERATION_NONE);
        inventoryDocumentRepository.updateHeader(type, header);
    }

    /**
     * 取消正在运行的流程实例。
     *
     * @param header 单据主表
     */
    public void cancelWorkflowInstanceIfRunning(InventoryDocumentHeader header) {
        if (header == null || !StringUtils.hasText(header.getWorkflowInstanceId())) {
            return;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(header.getWorkflowInstanceId())
                .singleResult();
        if (instance != null) {
            runtimeService.deleteProcessInstance(instance.getId(), "库存单据删除");
        }
    }

    /**
     * 解析审批角色标签。
     *
     * @param type       业务类型
     * @param scopeType  作用域类型
     * @param scopeId    作用域 ID
     * @param groupId    集团 ID
     * @param operatorId 操作人 ID
     * @param taskName   任务名称
     * @return 审批角色
     */
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

    /**
     * 解析审批目标。
     *
     * @param type      业务类型
     * @param scopeType 作用域类型
     * @param scopeId   作用域 ID
     * @param groupId   集团 ID
     * @param taskName  任务名称
     * @return 审批目标
     */
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

    private void startInstance(InventoryDocumentType type,
                               InventoryDocumentHeader header,
                               WorkflowBinding binding,
                               String businessKey,
                               Long operatorId,
                               String action) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(binding.processDefinitionKey())
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new BusinessException(type.getBusinessName() + "流程模板未发布，请先发布流程");
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
                        "businessCode", type.getBusinessCode(),
                        "documentCode", header.getDocumentCode(),
                        "operatorId", operatorId
                )
        );
        header.setWorkflowInstanceId(instance.getId());
        refreshCurrentTask(header, instance.getId());
        if (!StringUtils.hasText(header.getWorkflowTaskId())) {
            runtimeService.deleteProcessInstance(instance.getId(), type.getBusinessName() + "流程未配置审批节点");
            throw new BusinessException(type.getBusinessName() + "流程未配置审批节点");
        }
        inventoryDocumentRepository.updateHeader(type, header);
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

    private Optional<WorkflowBinding> resolveBinding(InventoryDocumentType type,
                                                     String scopeType,
                                                     Long scopeId,
                                                     Long groupId) {
        if (!SCOPE_GROUP.equalsIgnoreCase(scopeType) && !SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return Optional.empty();
        }
        if (groupId == null) {
            return Optional.empty();
        }
        WorkflowProcessRegistryDO registry = processRegistryRepository
                .findByScopeAndProcessCode(SCOPE_GROUP, groupId, type.getBusinessCode())
                .orElse(null);
        if (registry == null) {
            return Optional.empty();
        }
        String workflowCode = trimToNull(registry.getTemplateId());
        if (!StringUtils.hasText(workflowCode)) {
            return Optional.empty();
        }
        WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, type.getBusinessCode(), workflowCode);
        if (config == null || !"PUBLISHED".equals(config.getStatus())) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(config.getProcessDefinitionKey()) || !StringUtils.hasText(config.getProcessDefinitionId())) {
            throw new BusinessException(type.getBusinessName() + "流程定义缺失，请重新发布流程");
        }
        return Optional.of(new WorkflowBinding(
                registry.getProcessCode(),
                workflowCode,
                config.getProcessDefinitionKey(),
                config.getProcessDefinitionId()
        ));
    }

    private WorkflowDefinitionConfigDO findConfig(String scopeType,
                                                  Long scopeId,
                                                  Long groupId,
                                                  String businessCode,
                                                  String workflowCode) {
        WorkflowDefinitionConfigDO config = selectConfig(scopeType, scopeId, businessCode, workflowCode);
        if (config != null) {
            return config;
        }
        if (SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return selectConfig(SCOPE_GROUP, groupId, businessCode, workflowCode);
        }
        return null;
    }

    private WorkflowDefinitionConfigDO selectConfig(String scopeType,
                                                    Long scopeId,
                                                    String businessCode,
                                                    String workflowCode) {
        if (!StringUtils.hasText(scopeType) || scopeId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(workflowCode)) {
            return null;
        }
        return definitionConfigRepository.findByScopeBusinessAndWorkflow(
                scopeType.toUpperCase(),
                scopeId,
                businessCode,
                workflowCode
        ).orElse(null);
    }

    private String businessKey(InventoryDocumentType type, Long businessId) {
        return type.getBusinessCode() + ":" + businessId;
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 审批结果。
     *
     * @param workflowApplied 是否应用流程
     * @param completed 是否已完成
     */
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

    private record WorkflowBinding(String processCode,
                                   String workflowCode,
                                   String processDefinitionKey,
                                   String processDefinitionId) {
    }
}
