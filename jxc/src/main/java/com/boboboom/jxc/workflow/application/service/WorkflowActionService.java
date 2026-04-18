package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowProcessRegistryRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowProcessRegistryDO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkflowActionService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final WorkflowProcessRegistryRepository processRegistryRepository;
    private final WorkflowDefinitionConfigRepository definitionConfigRepository;
    private final UserAccountRepository userAccountRepository;
    private final ObjectMapper objectMapper;

    public WorkflowActionService(WorkflowProcessRegistryRepository processRegistryRepository,
                                 WorkflowDefinitionConfigRepository definitionConfigRepository,
                                 UserAccountRepository userAccountRepository,
                                 ObjectMapper objectMapper) {
        this.processRegistryRepository = processRegistryRepository;
        this.definitionConfigRepository = definitionConfigRepository;
        this.userAccountRepository = userAccountRepository;
        this.objectMapper = objectMapper;
    }

    public boolean shouldTriggerAction(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       String action) {
        if (!StringUtils.hasText(businessCode) || !StringUtils.hasText(action)) {
            return false;
        }
        try {
            Optional<WorkflowBinding> binding = resolveBinding(scopeType, scopeId, groupId, businessCode);
            if (binding.isEmpty()) {
                return false;
            }
            WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, binding.get().processCode(), binding.get().workflowCode());
            if (config == null || !StringUtils.hasText(config.getNodeConfigJson())) {
                return false;
            }
            return hasConfiguredAction(config.getNodeConfigJson(), action)
                    && !collectConfiguredRoleCodes(config.getNodeConfigJson(), action).isEmpty();
        } catch (BusinessException ex) {
            return false;
        }
    }

    public boolean hasActionPermission(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       Long operatorId,
                                       String action) {
        if (operatorId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(action)) {
            return false;
        }
        try {
            Optional<WorkflowBinding> binding = resolveBinding(scopeType, scopeId, groupId, businessCode);
            if (binding.isEmpty()) {
                return true;
            }
            WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, binding.get().processCode(), binding.get().workflowCode());
            if (config == null || !StringUtils.hasText(config.getNodeConfigJson())) {
                return false;
            }
            Set<String> configuredRoleCodes = collectConfiguredRoleCodes(config.getNodeConfigJson(), action);
            if (!hasConfiguredAction(config.getNodeConfigJson(), action)) {
                return false;
            }
            if (configuredRoleCodes.isEmpty()) {
                return true;
            }
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            if (userRoleCodes.isEmpty()) {
                return false;
            }
            for (String roleCode : userRoleCodes) {
                if (configuredRoleCodes.contains(roleCode)) {
                    return true;
                }
            }
            return false;
        } catch (BusinessException ex) {
            return false;
        }
    }

    public boolean hasConditionNodePermission(String businessCode,
                                              String scopeType,
                                              Long scopeId,
                                              Long groupId,
                                              Long operatorId) {
        if (operatorId == null || !StringUtils.hasText(businessCode)) {
            return false;
        }
        try {
            Optional<WorkflowBinding> binding = resolveBinding(scopeType, scopeId, groupId, businessCode);
            if (binding.isEmpty()) {
                return false;
            }
            WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, binding.get().processCode(), binding.get().workflowCode());
            if (config == null || !StringUtils.hasText(config.getNodeConfigJson())) {
                return false;
            }
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            return hasConditionNodePermission(config.getNodeConfigJson(), operatorId, userRoleCodes);
        } catch (BusinessException ex) {
            return false;
        }
    }

    private Optional<WorkflowBinding> resolveBinding(String scopeType, Long scopeId, Long groupId, String businessCode) {
        if (!SCOPE_GROUP.equalsIgnoreCase(scopeType) && !SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return Optional.empty();
        }
        if (groupId == null) {
            return Optional.empty();
        }

        WorkflowProcessRegistryDO registry = processRegistryRepository
                .findByScopeAndProcessCode(SCOPE_GROUP, groupId, businessCode)
                .orElse(null);
        if (registry == null) {
            return Optional.empty();
        }

        String workflowCode = trimToNull(registry.getTemplateId());
        if (!StringUtils.hasText(workflowCode)) {
            throw new BusinessException("流程尚未绑定模板，请先在流程管理中绑定模板");
        }

        WorkflowDefinitionConfigDO config = findConfig(scopeType, scopeId, groupId, registry.getProcessCode(), workflowCode);
        if (config == null) {
            throw new BusinessException("流程模板未发布，请先发布流程");
        }
        if (!"PUBLISHED".equals(config.getStatus())) {
            throw new BusinessException("流程模板未发布，请先发布流程");
        }
        if (!StringUtils.hasText(config.getProcessDefinitionKey()) || !StringUtils.hasText(config.getProcessDefinitionId())) {
            throw new BusinessException("流程定义缺失，请重新发布流程");
        }

        return Optional.of(new WorkflowBinding(
                registry.getProcessCode(),
                workflowCode,
                config.getProcessDefinitionKey(),
                config.getProcessDefinitionId()
        ));
    }

    private WorkflowDefinitionConfigDO findConfig(String scopeType, Long scopeId, Long groupId, String businessCode, String workflowCode) {
        WorkflowDefinitionConfigDO config = selectConfig(scopeType, scopeId, businessCode, workflowCode);
        if (config != null) {
            return config;
        }
        if (SCOPE_STORE.equalsIgnoreCase(scopeType)) {
            return selectConfig(SCOPE_GROUP, groupId, businessCode, workflowCode);
        }
        return null;
    }

    private WorkflowDefinitionConfigDO selectConfig(String scopeType, Long scopeId, String businessCode, String workflowCode) {
        if (!StringUtils.hasText(scopeType) || scopeId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(workflowCode)) {
            return null;
        }
        return definitionConfigRepository.findByScopeBusinessAndWorkflow(
                scopeType.toUpperCase(Locale.ROOT),
                scopeId,
                businessCode,
                workflowCode
        ).orElse(null);
    }

    private Set<String> collectConfiguredRoleCodes(String nodeConfigJson, String action) {
        try {
            JsonNode root = objectMapper.readTree(nodeConfigJson);
            if (root == null || !root.isArray()) {
                return Set.of();
            }
            String normalizedAction = normalizeAction(action);
            Set<String> roleCodes = new LinkedHashSet<>();
            for (JsonNode node : root) {
                String nodeType = trimToNull(node.path("nodeType").asText(null));
                if (!"NORMAL".equalsIgnoreCase(nodeType)) {
                    continue;
                }
                Set<String> triggerActions = collectTriggerActions(node.path("triggerActions"));
                if (!triggerActions.contains(normalizedAction)) {
                    continue;
                }
                String roleCode = trimToNull(node.path("approverRoleCode").asText(null));
                if (StringUtils.hasText(roleCode)) {
                    roleCodes.add(roleCode);
                }
            }
            return roleCodes;
        } catch (Exception ex) {
            return Set.of();
        }
    }

    private boolean hasConfiguredAction(String nodeConfigJson, String action) {
        try {
            JsonNode root = objectMapper.readTree(nodeConfigJson);
            if (root == null || !root.isArray()) {
                return false;
            }
            String normalizedAction = normalizeAction(action);
            for (JsonNode node : root) {
                String nodeType = trimToNull(node.path("nodeType").asText(null));
                if (!"NORMAL".equalsIgnoreCase(nodeType)) {
                    continue;
                }
                if (collectTriggerActions(node.path("triggerActions")).contains(normalizedAction)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private Set<String> collectTriggerActions(JsonNode triggerActionsNode) {
        if (triggerActionsNode == null || !triggerActionsNode.isArray()) {
            return Set.of();
        }
        Set<String> actions = new LinkedHashSet<>();
        for (JsonNode item : triggerActionsNode) {
            String value = trimToNull(item.asText(null));
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String upper = value.toUpperCase(Locale.ROOT);
            if ("CREATE".equals(upper) || "UPDATE".equals(upper) || "DELETE".equals(upper)) {
                actions.add(upper);
            }
        }
        return actions;
    }

    private Set<String> collectUserRoleCodes(Long operatorId, String scopeType, Long scopeId, Long groupId) {
        List<UserRoleView> roleViews = userAccountRepository.findUserRoles(operatorId);
        if (roleViews == null || roleViews.isEmpty()) {
            return Set.of();
        }
        Set<String> allowedScopes = new HashSet<>();
        if (StringUtils.hasText(scopeType)) {
            String normalizedScopeType = scopeType.toUpperCase(Locale.ROOT);
            if (SCOPE_STORE.equals(normalizedScopeType)) {
                if (scopeId != null) {
                    allowedScopes.add(SCOPE_STORE + ":" + scopeId);
                }
                if (groupId != null) {
                    allowedScopes.add(SCOPE_GROUP + ":" + groupId);
                }
            } else if (SCOPE_GROUP.equals(normalizedScopeType)) {
                if (scopeId != null) {
                    allowedScopes.add(SCOPE_GROUP + ":" + scopeId);
                }
            }
        }
        Set<String> roleCodes = new LinkedHashSet<>();
        for (UserRoleView roleView : roleViews) {
            if (roleView == null || !StringUtils.hasText(roleView.getRoleCode())) {
                continue;
            }
            String roleScopeType = roleView.getScopeType();
            Long roleScopeId = roleView.getScopeId();
            if (roleScopeType == null || roleScopeId == null) {
                continue;
            }
            String normalizedScope = roleScopeType.toUpperCase(Locale.ROOT) + ":" + roleScopeId;
            if (!allowedScopes.contains(normalizedScope)) {
                continue;
            }
            roleCodes.add(roleView.getRoleCode());
        }
        return roleCodes;
    }

    private boolean hasConditionNodePermission(String nodeConfigJson, Long operatorId, Set<String> userRoleCodes) {
        try {
            JsonNode root = objectMapper.readTree(nodeConfigJson);
            if (root == null || !root.isArray()) {
                return false;
            }
            for (JsonNode node : root) {
                String nodeType = trimToNull(node.path("nodeType").asText(null));
                if (!"CONDITION".equalsIgnoreCase(nodeType)) {
                    continue;
                }
                Long approverUserId = node.path("approverUserId").isNumber()
                        ? node.path("approverUserId").asLong()
                        : null;
                if (approverUserId != null && approverUserId.equals(operatorId)) {
                    return true;
                }
                String approverRoleCode = trimToNull(node.path("approverRoleCode").asText(null));
                if (StringUtils.hasText(approverRoleCode) && userRoleCodes.contains(approverRoleCode)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private String normalizeAction(String action) {
        String value = trimToNull(action);
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record WorkflowBinding(String processCode,
                                   String workflowCode,
                                   String processDefinitionKey,
                                   String processDefinitionId) {
    }
}
