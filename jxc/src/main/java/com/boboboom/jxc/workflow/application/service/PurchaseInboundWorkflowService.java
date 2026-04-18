package com.boboboom.jxc.workflow.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.PurchaseInboundDO;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.PurchaseInboundMapper;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowDefinitionConfigMapper;
import com.boboboom.jxc.workflow.infrastructure.persistence.mapper.WorkflowProcessRegistryMapper;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class PurchaseInboundWorkflowService {

    private static final String BUSINESS_CODE = "PURCHASE_INBOUND";
    private static final String WORKFLOW_PROCESS_CODE = "PURCHASE_INBOUND";
    private static final String WORKFLOW_STATUS_NONE = "NONE";
    private static final String WORKFLOW_STATUS_RUNNING = "RUNNING";
    private static final String WORKFLOW_STATUS_COMPLETED = "COMPLETED";
    private static final String WORKFLOW_STATUS_REVOKED = "REVOKED";
    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";
    private static final String PENDING_OPERATION_NONE = "NONE";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final WorkflowProcessRegistryMapper processRegistryMapper;
    private final WorkflowDefinitionConfigMapper definitionConfigMapper;
    private final WorkflowActionService workflowActionService;
    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final PurchaseInboundMapper purchaseInboundMapper;

    public PurchaseInboundWorkflowService(WorkflowProcessRegistryMapper processRegistryMapper,
                                          WorkflowDefinitionConfigMapper definitionConfigMapper,
                                          WorkflowActionService workflowActionService,
                                          RepositoryService repositoryService,
                                          RuntimeService runtimeService,
                                          TaskService taskService,
                                          PurchaseInboundMapper purchaseInboundMapper) {
        this.processRegistryMapper = processRegistryMapper;
        this.definitionConfigMapper = definitionConfigMapper;
        this.workflowActionService = workflowActionService;
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.purchaseInboundMapper = purchaseInboundMapper;
    }

    public boolean hasBusinessOperationPermission(String scopeType, Long scopeId, Long groupId, Long operatorId, String action) {
        return workflowActionService.hasActionPermission(BUSINESS_CODE, scopeType, scopeId, groupId, operatorId, action);
    }

    public boolean hasBusinessReviewPermission(String scopeType, Long scopeId, Long groupId, Long operatorId) {
        return workflowActionService.hasConditionNodePermission(BUSINESS_CODE, scopeType, scopeId, groupId, operatorId);
    }

    public boolean shouldTriggerAction(String scopeType, Long scopeId, Long groupId, String action) {
        return workflowActionService.shouldTriggerAction(BUSINESS_CODE, scopeType, scopeId, groupId, action);
    }

    public boolean syncOnAction(String scopeType, Long scopeId, Long groupId, PurchaseInboundDO header, Long operatorId, String action) {
        if (!shouldTriggerAction(scopeType, scopeId, groupId, action)) {
            cancelWorkflowInstanceIfRunning(header);
            header.setWorkflowProcessCode(null);
            header.setWorkflowDefinitionKey(null);
            header.setWorkflowDefinitionId(null);
            header.setWorkflowInstanceId(null);
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            header.setWorkflowStatus(WORKFLOW_STATUS_NONE);
            header.setPendingOperation(PENDING_OPERATION_NONE);
            purchaseInboundMapper.updateById(header);
            return false;
        }
        WorkflowBinding binding = resolveBinding(scopeType, scopeId, groupId).orElse(null);
        if (binding == null) {
            return false;
        }

        String businessKey = businessKey(header.getId());
        ProcessInstance activeInstance = findActiveInstance(businessKey);
        if (activeInstance == null) {
            startInstance(header, binding, businessKey, operatorId, action);
            return true;
        }

        header.setWorkflowProcessCode(binding.processCode());
        header.setWorkflowDefinitionKey(binding.processDefinitionKey());
        header.setWorkflowDefinitionId(binding.processDefinitionId());
        header.setWorkflowInstanceId(activeInstance.getId());
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setPendingOperation(action);
        refreshCurrentTask(header, activeInstance.getId());
        purchaseInboundMapper.updateById(header);
        return true;
    }

    public ApprovalResult completeCurrentTask(PurchaseInboundDO header, Long operatorId) {
        String businessKey = businessKey(header.getId());
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
                "businessCode", BUSINESS_CODE,
                "documentCode", header.getDocumentCode()
        ));

        ProcessInstance nextInstance = findActiveInstance(businessKey);
        if (nextInstance == null) {
            header.setWorkflowStatus(WORKFLOW_STATUS_COMPLETED);
            header.setWorkflowInstanceId(activeInstance.getId());
            header.setWorkflowTaskId(null);
            header.setWorkflowTaskName(null);
            purchaseInboundMapper.updateById(header);
            return ApprovalResult.workflowCompleted();
        }

        header.setWorkflowProcessCode(WORKFLOW_PROCESS_CODE);
        header.setWorkflowStatus(WORKFLOW_STATUS_RUNNING);
        header.setWorkflowInstanceId(nextInstance.getId());
        refreshCurrentTask(header, nextInstance.getId());
        purchaseInboundMapper.updateById(header);
        return ApprovalResult.workflowPending();
    }

    public void resetWorkflowState(PurchaseInboundDO header) {
        cancelWorkflowInstanceIfRunning(header);
        header.setWorkflowDefinitionKey(null);
        header.setWorkflowDefinitionId(null);
        header.setWorkflowInstanceId(null);
        header.setWorkflowTaskId(null);
        header.setWorkflowTaskName(null);
        header.setWorkflowStatus(WORKFLOW_STATUS_REVOKED);
        header.setPendingOperation(PENDING_OPERATION_NONE);
        purchaseInboundMapper.updateById(header);
    }

    public void cancelWorkflowInstanceIfRunning(PurchaseInboundDO header) {
        if (header == null || !StringUtils.hasText(header.getWorkflowInstanceId())) {
            return;
        }
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(header.getWorkflowInstanceId())
                .singleResult();
        if (instance != null) {
            runtimeService.deleteProcessInstance(instance.getId(), "采购入库单删除");
        }
    }

    private void startInstance(PurchaseInboundDO header, WorkflowBinding binding, String businessKey, Long operatorId, String action) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(binding.processDefinitionKey())
                .latestVersion()
                .singleResult();
        if (processDefinition == null) {
            throw new BusinessException("采购入库流程模板未发布，请先发布流程");
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
                        "businessCode", BUSINESS_CODE,
                        "documentCode", header.getDocumentCode(),
                        "operatorId", operatorId
                )
        );
        header.setWorkflowInstanceId(instance.getId());
        refreshCurrentTask(header, instance.getId());
        if (!StringUtils.hasText(header.getWorkflowTaskId())) {
            runtimeService.deleteProcessInstance(instance.getId(), "采购入库流程未配置审批节点");
            throw new BusinessException("采购入库流程未配置审批节点");
        }
        purchaseInboundMapper.updateById(header);
    }

    private void refreshCurrentTask(PurchaseInboundDO header, String processInstanceId) {
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

    private Optional<WorkflowBinding> resolveBinding(String scopeType, Long scopeId, Long groupId) {
        if (!SCOPE_GROUP.equalsIgnoreCase(scopeType) && !SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return Optional.empty();
        }
        if (groupId == null) {
            return Optional.empty();
        }

        WorkflowProcessRegistryDO registry = processRegistryMapper.selectOne(new LambdaQueryWrapper<WorkflowProcessRegistryDO>()
                .eq(WorkflowProcessRegistryDO::getScopeType, SCOPE_GROUP)
                .eq(WorkflowProcessRegistryDO::getScopeId, groupId)
                .eq(WorkflowProcessRegistryDO::getProcessCode, WORKFLOW_PROCESS_CODE)
                .last("limit 1"));
        if (registry == null) {
            return Optional.empty();
        }

        String workflowCode = trimToNull(registry.getTemplateId());
        if (!StringUtils.hasText(workflowCode)) {
            throw new BusinessException("采购入库流程尚未绑定模板，请先在流程管理中绑定模板");
        }

        WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, workflowCode);
        if (config == null) {
            throw new BusinessException("采购入库流程模板未发布，请先发布流程");
        }
        if (!"PUBLISHED".equals(config.getStatus())) {
            throw new BusinessException("采购入库流程模板未发布，请先发布流程");
        }
        if (!StringUtils.hasText(config.getProcessDefinitionKey()) || !StringUtils.hasText(config.getProcessDefinitionId())) {
            throw new BusinessException("采购入库流程定义缺失，请重新发布流程");
        }

        return Optional.of(new WorkflowBinding(
                registry.getProcessCode(),
                workflowCode,
                config.getProcessDefinitionKey(),
                config.getProcessDefinitionId()
        ));
    }

    private WorkflowDefinitionConfigDO findConfig(String scopeType, Long scopeId, Long groupId, String workflowCode) {
        WorkflowDefinitionConfigDO config = selectConfig(scopeType, scopeId, workflowCode);
        if (config != null) {
            return config;
        }
        if (SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return selectConfig(SCOPE_GROUP, groupId, workflowCode);
        }
        return null;
    }

    private WorkflowDefinitionConfigDO selectConfig(String scopeType, Long scopeId, String workflowCode) {
        if (!StringUtils.hasText(scopeType) || scopeId == null || !StringUtils.hasText(workflowCode)) {
            return null;
        }
        return definitionConfigMapper.selectOne(new LambdaQueryWrapper<WorkflowDefinitionConfigDO>()
                .eq(WorkflowDefinitionConfigDO::getScopeType, scopeType.toUpperCase(Locale.ROOT))
                .eq(WorkflowDefinitionConfigDO::getScopeId, scopeId)
                .eq(WorkflowDefinitionConfigDO::getBusinessCode, BUSINESS_CODE)
                .eq(WorkflowDefinitionConfigDO::getWorkflowCode, workflowCode)
                .last("limit 1"));
    }

    private String businessKey(Long purchaseInboundId) {
        return BUSINESS_CODE + ":" + purchaseInboundId;
    }

    private boolean hasWorkflowMetadata(PurchaseInboundDO header) {
        return StringUtils.hasText(header.getWorkflowInstanceId())
                || StringUtils.hasText(header.getWorkflowProcessCode())
                || StringUtils.hasText(header.getWorkflowDefinitionKey())
                || StringUtils.hasText(header.getWorkflowDefinitionId())
                || StringUtils.hasText(header.getWorkflowTaskId())
                || StringUtils.hasText(header.getWorkflowTaskName())
                || StringUtils.hasText(header.getWorkflowStatus()) && !WORKFLOW_STATUS_NONE.equals(header.getWorkflowStatus());
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

    private record WorkflowBinding(String processCode,
                                   String workflowCode,
                                   String processDefinitionKey,
                                   String processDefinitionId) {
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
