package com.boboboom.jxc.workflow.application.service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.RoleRepository;
import com.boboboom.jxc.identity.domain.repository.UserAccountRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 流程动作服务，负责审批动作权限、任务推进和流程状态判断。 */
@Service
public class WorkflowActionService {

    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final WorkflowBindingResolverService workflowBindingResolverService;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    /** 流程动作服务，负责审批动作权限、任务推进和流程状态判断。 */
    public WorkflowActionService(WorkflowBindingResolverService workflowBindingResolverServiceValue,
                                 UserAccountRepository userAccountRepositoryValue,
                                 RoleRepository roleRepositoryValue,
                                 ObjectMapper objectMapperValue) {
        this.workflowBindingResolverService = workflowBindingResolverServiceValue;
        this.userAccountRepository = userAccountRepositoryValue;
        this.roleRepository = roleRepositoryValue;
        this.objectMapper = objectMapperValue;
    }

    /** 判断当前业务动作是否需要触发审批流程。 */
    public boolean shouldTriggerAction(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       String action) {
        if (!StringUtils.hasText(businessCode) || !StringUtils.hasText(action)) {
            return false;
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return false;
            }
            return hasConfiguredAction(root, action)
                    && !collectConfiguredRoleCodes(root, action).isEmpty();
        } catch (BusinessException ex) {
            return false;
        }
    }

    /** 判断是否具备动作权限。 */
    public boolean hasActionPermission(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       Long operatorId,
                                       String action) {
        if (isInvalidActionRequest(businessCode, operatorId, action)) {
            return false;
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return true;
            }
            Set<String> configuredRoleCodes = collectConfiguredRoleCodes(root, action);
            if (!hasConfiguredAction(root, action)) {
                return false;
            }
            if (configuredRoleCodes.isEmpty()) {
                return true;
            }
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            return hasAnyRole(userRoleCodes, configuredRoleCodes);
        } catch (BusinessException ex) {
            return false;
        }
    }

    /** 判断是否具备任意普通节点权限。 */
    public boolean hasAnyNormalNodePermission(String businessCode,
                                              String scopeType,
                                              Long scopeId,
                                              Long groupId,
                                              Long operatorId) {
        if (operatorId == null || !StringUtils.hasText(businessCode)) {
            return false;
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return false;
            }
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            for (JsonNode node : root) {
                if (matchesNormalNodePermission(node, operatorId, userRoleCodes)) {
                    return true;
                }
            }
            return false;
        } catch (BusinessException ex) {
            return false;
        }
    }

    /** 判断是否具备条件节点权限。 */
    public boolean hasConditionNodePermission(String businessCode,
                                              String scopeType,
                                              Long scopeId,
                                              Long groupId,
                                              Long operatorId) {
        if (operatorId == null || !StringUtils.hasText(businessCode)) {
            return false;
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return false;
            }
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            return hasConditionNodePermission(root, operatorId, userRoleCodes);
        } catch (BusinessException ex) {
            return false;
        }
    }

    /** 解析审批节点对应的角色展示名称。 */
    public String resolveApprovalRoleLabel(String businessCode,
                                           String scopeType,
                                           Long scopeId,
                                           Long groupId,
                                           Long operatorId,
                                           String taskName) {
        if (operatorId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(taskName)) {
            return "普通审核";
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return "普通审核";
            }
            String normalizedTaskName = trimToNull(taskName);
            for (JsonNode node : root) {
                if (matchesNodeName(node, normalizedTaskName)) {
                    return resolveApprovalRoleLabel(node, operatorId);
                }
            }
        } catch (Exception ex) {
            return "普通审核";
        }
        return "普通审核";
    }

    /** 解析审批节点对应的审批目标。 */
    public Optional<ApprovalTarget> resolveApprovalTarget(String businessCode,
                                                          String scopeType,
                                                          Long scopeId,
                                                          Long groupId,
                                                          String taskName) {
        return resolveApprovalTarget(businessCode, scopeType, scopeId, groupId, taskName, null);
    }

    /** 解析审批节点对应的审批目标。 */
    public Optional<ApprovalTarget> resolveApprovalTarget(String businessCode,
                                                          String scopeType,
                                                          Long scopeId,
                                                          Long groupId,
                                                          String taskName,
                                                          String taskDefinitionKey) {
        if (!StringUtils.hasText(businessCode) || !StringUtils.hasText(taskName)) {
            return Optional.empty();
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return Optional.empty();
            }
            String normalizedTaskName = trimToNull(taskName);
            String normalizedTaskDefinitionKey = normalizeTaskDefinitionKey(taskDefinitionKey);
            for (JsonNode node : root) {
                if (!matchesTaskNode(node, normalizedTaskName, normalizedTaskDefinitionKey)) {
                    continue;
                }
                Long approverUserId = node.path("approverUserId").isNumber()
                        ? node.path("approverUserId").asLong()
                        : null;
                String approverRoleCode = trimToNull(node.path("approverRoleCode").asText(null));
                String approverRoleName = resolveRoleName(approverRoleCode);
                return Optional.of(new ApprovalTarget(approverUserId, approverRoleCode, approverRoleName));
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** 解析节点类型。 */
    public Optional<String> resolveNodeType(String businessCode,
                                            String scopeType,
                                            Long scopeId,
                                            Long groupId,
                                            String taskName) {
        if (!StringUtils.hasText(businessCode) || !StringUtils.hasText(taskName)) {
            return Optional.empty();
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return Optional.empty();
            }
            String normalizedTaskName = trimToNull(taskName);
            for (JsonNode node : root) {
                String nodeName = trimToNull(node.path("nodeName").asText(null));
                if (StringUtils.hasText(nodeName) && nodeName.equals(normalizedTaskName)) {
                    return Optional.ofNullable(trimToNull(node.path("nodeType").asText(null)));
                }
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    /** 处理匹配审批目标。 */
    public boolean matchesApprovalTarget(Long operatorId,
                                         String scopeType,
                                         Long scopeId,
                                         Long groupId,
                                         ApprovalTarget target) {
        if (operatorId == null || target == null) {
            return false;
        }
        if (target.userId() != null && target.userId().equals(operatorId)) {
            return true;
        }
        if (!StringUtils.hasText(target.roleCode())) {
            return false;
        }
        Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
        return userRoleCodes.contains(target.roleCode());
    }

    /** 判断ActionTriggerTask。 */
    public boolean isActionTriggerTask(String businessCode,
                                       String scopeType,
                                       Long scopeId,
                                       Long groupId,
                                       Long operatorId,
                                       String taskName,
                                       String action) {
        if (isInvalidTaskActionRequest(businessCode, operatorId, taskName, action)) {
            return false;
        }
        try {
            JsonNode root = readNodeConfig(scopeType, scopeId, groupId, businessCode);
            if (root == null) {
                return false;
            }
            String normalizedTaskName = trimToNull(taskName);
            String normalizedAction = normalizeAction(action);
            Set<String> userRoleCodes = collectUserRoleCodes(operatorId, scopeType, scopeId, groupId);
            for (JsonNode node : root) {
                if (matchesActionTriggerTask(node, normalizedTaskName, normalizedAction, operatorId, userRoleCodes)) {
                    return true;
                }
            }
            return false;
        } catch (BusinessException ex) {
            return false;
        }
    }

    private JsonNode readNodeConfig(String scopeType,
                                    Long scopeId,
                                    Long groupId,
                                    String businessCode) {
        Optional<WorkflowBindingResolverService.ResolvedWorkflowBinding> binding =
                workflowBindingResolverService.resolvePublishedBinding(scopeType, scopeId, groupId, businessCode, "流程");
        if (binding.isEmpty()) {
            return null;
        }
        try {
            String nodeConfigJson = trimToNull(binding.get().nodeConfigJson());
            if (!StringUtils.hasText(nodeConfigJson)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(nodeConfigJson);
            return root != null && root.isArray() ? root : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private Set<String> collectConfiguredRoleCodes(JsonNode root, String action) {
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
    }

    private boolean hasConfiguredAction(JsonNode root, String action) {
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
        Set<String> allowedScopes = buildAllowedScopes(scopeType, scopeId, groupId);
        Set<String> roleCodes = new LinkedHashSet<>();
        for (UserRoleView roleView : roleViews) {
            addUserRoleCode(roleCodes, roleView, allowedScopes);
        }
        return roleCodes;
    }

    private boolean isInvalidActionRequest(String businessCode, Long operatorId, String action) {
        return operatorId == null || !StringUtils.hasText(businessCode) || !StringUtils.hasText(action);
    }

    private boolean isInvalidTaskActionRequest(String businessCode, Long operatorId, String taskName, String action) {
        return isInvalidActionRequest(businessCode, operatorId, action) || !StringUtils.hasText(taskName);
    }

    private boolean hasAnyRole(Set<String> userRoleCodes, Set<String> configuredRoleCodes) {
        for (String roleCode : userRoleCodes) {
            if (configuredRoleCodes.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesNormalNodePermission(JsonNode node, Long operatorId, Set<String> userRoleCodes) {
        String nodeType = trimToNull(node.path("nodeType").asText(null));
        if (!"NORMAL".equalsIgnoreCase(nodeType)) {
            return false;
        }
        Long approverUserId = node.path("approverUserId").isNumber()
                ? node.path("approverUserId").asLong()
                : null;
        if (approverUserId != null && approverUserId.equals(operatorId)) {
            return true;
        }
        String approverRoleCode = trimToNull(node.path("approverRoleCode").asText(null));
        return StringUtils.hasText(approverRoleCode) && userRoleCodes.contains(approverRoleCode);
    }

    private boolean matchesNodeName(JsonNode node, String normalizedTaskName) {
        String nodeName = trimToNull(node.path("nodeName").asText(null));
        return StringUtils.hasText(nodeName) && nodeName.equals(normalizedTaskName);
    }

    private boolean matchesTaskNode(JsonNode node, String normalizedTaskName, String normalizedTaskDefinitionKey) {
        if (matchesNodeName(node, normalizedTaskName)) {
            return true;
        }
        if (!StringUtils.hasText(normalizedTaskDefinitionKey)) {
            return false;
        }
        String nodeKey = trimToNull(node.path("nodeKey").asText(null));
        if (!StringUtils.hasText(nodeKey)) {
            return false;
        }
        return normalizedTaskDefinitionKey.equals(nodeKey);
    }

    private String resolveApprovalRoleLabel(JsonNode node, Long operatorId) {
        Long approverUserId = node.path("approverUserId").isNumber()
                ? node.path("approverUserId").asLong()
                : null;
        if (approverUserId != null && approverUserId.equals(operatorId)) {
            return "指定人员";
        }
        String roleCode = trimToNull(node.path("approverRoleCode").asText(null));
        if (!StringUtils.hasText(roleCode)) {
            return "普通审核";
        }
        return roleRepository.findByRoleCode(roleCode)
                .map(RoleDO::getRoleName)
                .orElse(roleCode);
    }

    private boolean matchesActionTriggerTask(JsonNode node,
                                             String normalizedTaskName,
                                             String normalizedAction,
                                             Long operatorId,
                                             Set<String> userRoleCodes) {
        String nodeType = trimToNull(node.path("nodeType").asText(null));
        if (!"NORMAL".equalsIgnoreCase(nodeType) || !matchesNodeName(node, normalizedTaskName)) {
            return false;
        }
        if (!collectTriggerActions(node.path("triggerActions")).contains(normalizedAction)) {
            return false;
        }
        Long approverUserId = node.path("approverUserId").isNumber()
                ? node.path("approverUserId").asLong()
                : null;
        if (approverUserId != null) {
            return approverUserId.equals(operatorId);
        }
        String roleCode = trimToNull(node.path("approverRoleCode").asText(null));
        return StringUtils.hasText(roleCode) && userRoleCodes.contains(roleCode);
    }

    private Set<String> buildAllowedScopes(String scopeType, Long scopeId, Long groupId) {
        Set<String> allowedScopes = new HashSet<>();
        if (!StringUtils.hasText(scopeType)) {
            return allowedScopes;
        }
        String normalizedScopeType = scopeType.toUpperCase(Locale.ROOT);
        if (SCOPE_STORE.equals(normalizedScopeType)) {
            addAllowedScope(allowedScopes, SCOPE_STORE, scopeId);
            addAllowedScope(allowedScopes, SCOPE_GROUP, groupId);
        } else if (SCOPE_GROUP.equals(normalizedScopeType)) {
            addAllowedScope(allowedScopes, SCOPE_GROUP, scopeId);
        }
        return allowedScopes;
    }

    private void addAllowedScope(Set<String> allowedScopes, String scopeType, Long scopeId) {
        if (scopeId != null) {
            allowedScopes.add(scopeType + ":" + scopeId);
        }
    }

    private void addUserRoleCode(Set<String> roleCodes, UserRoleView roleView, Set<String> allowedScopes) {
        if (roleView == null || !StringUtils.hasText(roleView.getRoleCode())) {
            return;
        }
        String roleScopeType = roleView.getScopeType();
        Long roleScopeId = roleView.getScopeId();
        if (roleScopeType == null || roleScopeId == null) {
            return;
        }
        String normalizedScope = roleScopeType.toUpperCase(Locale.ROOT) + ":" + roleScopeId;
        if (allowedScopes.contains(normalizedScope)) {
            roleCodes.add(roleView.getRoleCode());
        }
    }

    private boolean hasConditionNodePermission(JsonNode root, Long operatorId, Set<String> userRoleCodes) {
        for (JsonNode node : root) {
            String nodeType = trimToNull(node.path("nodeType").asText(null));
            if (!isReviewNode(nodeType, node.path("triggerActions"))) {
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
    }

    private boolean isReviewNode(String nodeType, JsonNode triggerActionsNode) {
        return "CONDITION".equalsIgnoreCase(nodeType);
    }

    private String normalizeAction(String action) {
        String value = trimToNull(action);
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private String normalizeTaskDefinitionKey(String taskDefinitionKey) {
        String value = trimToNull(taskDefinitionKey);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.startsWith("task_")) {
            return value.substring("task_".length());
        }
        return value;
    }

    private String resolveRoleName(String roleCode) {
        if (!StringUtils.hasText(roleCode)) {
            return "";
        }
        return roleRepository.findByRoleCode(roleCode)
                .map(RoleDO::getRoleName)
                .filter(StringUtils::hasText)
                .orElse(roleCode);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** 审批流程数据模型，承载审批目标数据。 */
    public record ApprovalTarget(Long userId, String roleCode, String roleName) {
    }
}
