package com.boboboom.jxc.workflow.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.workflow.domain.repository.WorkflowApprovalNotificationRepository;
import com.boboboom.jxc.workflow.domain.repository.WorkflowDefinitionConfigRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowDefinitionConfigDO;

/** 审批通知业务服务，负责待办、已办和通知阅读状态维护。 */
@Service
public class WorkflowApprovalNotificationApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final WorkflowApprovalNotificationRepository notificationRepository;
    private final WorkflowDefinitionConfigRepository definitionConfigRepository;
    private final OrgScopeService orgScopeService;
    private final WorkflowActionService workflowActionService;

    /** 审批通知业务服务，负责待办、已办和通知阅读状态维护。 */
    public WorkflowApprovalNotificationApplicationService(WorkflowApprovalNotificationRepository notificationRepositoryValue,
                                                         WorkflowDefinitionConfigRepository definitionConfigRepositoryValue,
                                                         OrgScopeService orgScopeServiceValue,
                                                         WorkflowActionService workflowActionServiceValue) {
        this.notificationRepository = notificationRepositoryValue;
        this.definitionConfigRepository = definitionConfigRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.workflowActionService = workflowActionServiceValue;
    }

    /** 记录业务通知或审计数据。 */
    public void record(String scopeType,
                       Long scopeId,
                       String businessCode,
                       String businessName,
                       Long businessId,
                       String approvalNo,
                       Long approverUserId,
                       String approverName,
                       String approverRole,
                       Long targetApproverUserId,
                       String targetApproverRoleCode,
                       String targetApproverRoleName,
                       LocalDateTime auditedAt,
                       String result,
                       String remark,
                       String routePath) {
        if (isInvalidRecordRequest(scopeType, scopeId, businessId, approvalNo)) {
            return;
        }
        WorkflowApprovalNotificationDO notification = notificationRepository.findLatestByScopeAndBusiness(
                scopeType,
                scopeId,
                businessCode,
                businessId
        );
        if (notification == null) {
            notification = new WorkflowApprovalNotificationDO();
            notification.setScopeType(scopeType);
            notification.setScopeId(scopeId);
            notification.setBusinessCode(defaultText(businessCode, ""));
            notification.setBusinessId(businessId);
            notification.setApprovalNo(approvalNo);
        }
        notification.setScopeType(scopeType);
        notification.setScopeId(scopeId);
        notification.setBusinessCode(defaultText(businessCode, ""));
        notification.setBusinessName(defaultText(businessName, ""));
        notification.setBusinessId(businessId);
        notification.setApprovalNo(approvalNo);
        notification.setApproverUserId(approverUserId);
        notification.setApproverName(defaultText(approverName, "system"));
        notification.setApproverRole(defaultText(approverRole, "普通审核"));
        notification.setTargetApproverUserId(targetApproverUserId);
        notification.setTargetApproverRoleCode(nullableText(targetApproverRoleCode));
        notification.setTargetApproverRoleName(nullableText(targetApproverRoleName));
        notification.setAuditedAt(defaultAuditTime(auditedAt));
        notification.setResult(defaultText(result, "通过"));
        notification.setRemark(defaultText(remark, ""));
        notification.setRoutePath(defaultText(routePath, ""));
        if (notification.getId() == null) {
            notificationRepository.save(notification);
            return;
        }
        notificationRepository.update(notification);
    }

    private boolean isInvalidRecordRequest(String scopeType, Long scopeId, Long businessId, String approvalNo) {
        return !StringUtils.hasText(scopeType)
                || scopeId == null
                || businessId == null
                || !StringUtils.hasText(approvalNo);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String nullableText(String value) {
        return StringUtils.hasText(value) ? value : null;
    }

    private LocalDateTime defaultAuditTime(LocalDateTime auditedAt) {
        return auditedAt == null ? LocalDateTime.now() : auditedAt;
    }

    /** 统计当前用户待处理通知数量。 */
    public long pendingCount(String orgId) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        List<WorkflowApprovalNotificationDO> rows = loadAccessibleNotifications(scope);
        if (rows.isEmpty()) {
            return 0L;
        }
        return rows.stream()
                .filter(this::isPendingNotification)
                .filter(row -> workflowActionService.matchesApprovalTarget(
                        operatorId,
                        scope.scopeType(),
                        scope.scopeId(),
                        scope.groupId(),
                        toApprovalTarget(row)
                ))
                .count();
    }

    /** 分页查询业务数据。 */
    public NotificationPageData<WorkflowApprovalNotificationView> page(String orgId, String tab, Integer pageNum, Integer pageSize) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        List<NotificationTabView> tabs = resolveVisibleTabs(operatorId, scope);
        String activeTab = resolveActiveTab(tab, tabs);
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        List<WorkflowApprovalNotificationDO> rows = loadAccessibleNotifications(scope).stream()
                .filter(row -> isVisibleToOperator(row, operatorId, scope, activeTab))
                .toList();
        if (rows.isEmpty()) {
            return new NotificationPageData<>(List.of(), 0, safePageNum, safePageSize, tabs, activeTab);
        }
        int start = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int end = Math.min(start + safePageSize, rows.size());
        List<WorkflowApprovalNotificationView> list = rows.subList(start, end).stream()
                .map(this::toView)
                .toList();
        return new NotificationPageData<>(list, rows.size(), safePageNum, safePageSize, tabs, activeTab);
    }

    private WorkflowApprovalNotificationView toView(WorkflowApprovalNotificationDO row) {
        return new WorkflowApprovalNotificationView(
                row.getId(),
                row.getApprovalNo(),
                defaultIfBlank(row.getBusinessName(), row.getBusinessCode()),
                row.getApproverName(),
                row.getApproverRole(),
                formatDateTime(row.getAuditedAt()),
                defaultIfBlank(row.getResult(), "通过"),
                defaultIfBlank(row.getRemark(), ""),
                row.getRoutePath(),
                row.getBusinessCode(),
                row.getBusinessId()
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        return DATETIME_FORMATTER.format(value);
    }

    private boolean isPendingNotification(WorkflowApprovalNotificationDO row) {
        return row != null && "待审核".equals(row.getResult());
    }

    private boolean isVisibleToOperator(WorkflowApprovalNotificationDO row,
                                        Long operatorId,
                                        OrgScopeService.AccessibleScope scope,
                                        String tab) {
        if (row == null || !StringUtils.hasText(tab)) {
            return false;
        }
        if ("PENDING_REVIEW".equals(tab)) {
            return isPendingNotification(row)
                    && workflowActionService.matchesApprovalTarget(
                    operatorId,
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.groupId(),
                    toApprovalTarget(row)
            );
        }
        if ("APPROVED".equals(tab)) {
            return "通过".equals(row.getResult());
        }
        if ("REJECTED_BY_ME".equals(tab)) {
            return "拒绝".equals(row.getResult()) && operatorId.equals(row.getApproverUserId());
        }
        if ("SUBMITTED".equals(tab)) {
            return isPendingNotification(row)
                    && workflowActionService.hasAnyNormalNodePermission(
                    row.getBusinessCode(),
                    scope.scopeType(),
                    scope.scopeId(),
                    scope.groupId(),
                    operatorId
            );
        }
        return false;
    }

    private List<NotificationTabView> resolveVisibleTabs(Long operatorId, OrgScopeService.AccessibleScope scope) {
        if (scope == null || scope.scopeType() == null || scope.scopeId() == null) {
            return List.of();
        }
        List<String> businessCodes = loadAccessiblePublishedBusinessCodes(scope);
        boolean canReview = businessCodes.stream().anyMatch(businessCode -> workflowActionService.hasConditionNodePermission(
                businessCode,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        ));
        if (canReview) {
            return List.of(
                    new NotificationTabView("PENDING_REVIEW", "待审核记录"),
                    new NotificationTabView("APPROVED", "已审核记录"),
                    new NotificationTabView("REJECTED_BY_ME", "我驳回的记录")
            );
        }
        boolean canSubmit = businessCodes.stream().anyMatch(businessCode -> workflowActionService.hasAnyNormalNodePermission(
                businessCode,
                scope.scopeType(),
                scope.scopeId(),
                scope.groupId(),
                operatorId
        ));
        if (canSubmit) {
            return List.of(new NotificationTabView("SUBMITTED", "已提交记录"));
        }
        return List.of();
    }

    private List<String> loadAccessiblePublishedBusinessCodes(OrgScopeService.AccessibleScope scope) {
        if (scope == null || scope.scopeType() == null || scope.scopeId() == null) {
            return List.of();
        }
        List<WorkflowDefinitionConfigDO> configs = new ArrayList<>(
                definitionConfigRepository.findPublishedByScope(scope.scopeType(), scope.scopeId())
        );
        if ("STORE".equalsIgnoreCase(scope.scopeType()) && scope.groupId() != null) {
            configs.addAll(definitionConfigRepository.findPublishedByScope(OrgScopeService.SCOPE_GROUP, scope.groupId()));
        }
        return configs.stream()
                .map(WorkflowDefinitionConfigDO::getBusinessCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String resolveActiveTab(String tab, List<NotificationTabView> tabs) {
        if (tabs == null || tabs.isEmpty()) {
            return "";
        }
        String normalized = StringUtils.hasText(tab) ? tab.trim().toUpperCase(Locale.ROOT) : "";
        return tabs.stream()
                .map(NotificationTabView::key)
                .filter(key -> key.equals(normalized))
                .findFirst()
                .orElse(tabs.get(0).key());
    }

    private List<WorkflowApprovalNotificationDO> loadAccessibleNotifications(OrgScopeService.AccessibleScope scope) {
        if (scope == null || scope.scopeType() == null || scope.scopeId() == null) {
            return List.of();
        }
        List<WorkflowApprovalNotificationDO> rows = new ArrayList<>(
                notificationRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId())
        );
        if ("STORE".equalsIgnoreCase(scope.scopeType()) && scope.groupId() != null) {
            rows.addAll(notificationRepository.findByScopeOrdered(OrgScopeService.SCOPE_GROUP, scope.groupId()));
        }
        rows.sort(notificationComparator());
        return rows;
    }

    private Comparator<WorkflowApprovalNotificationDO> notificationComparator() {
        return (left, right) -> {
            int byAuditedAt = compareDesc(left == null ? null : left.getAuditedAt(), right == null ? null : right.getAuditedAt());
            if (byAuditedAt != 0) {
                return byAuditedAt;
            }
            return compareDesc(left == null ? null : left.getId(), right == null ? null : right.getId());
        };
    }

    private <T extends Comparable<T>> int compareDesc(T left, T right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private WorkflowActionService.ApprovalTarget toApprovalTarget(WorkflowApprovalNotificationDO row) {
        if (row == null) {
            return null;
        }
        return new WorkflowActionService.ApprovalTarget(
                row.getTargetApproverUserId(),
                row.getTargetApproverRoleCode(),
                row.getTargetApproverRoleName()
        );
    }

    /** 审批流程分页数据模型，承载列表数据和分页信息。 */
    public record PageData<T>(List<T> list, long total, int pageNum, int pageSize) {
    }

    /** 审批流程分页数据模型，承载列表数据和分页信息。 */
    public record NotificationPageData<T>(List<T> list,
                                          long total,
                                          int pageNum,
                                          int pageSize,
                                          List<NotificationTabView> tabs,
                                          String activeTab) {
    }

    /** 审批流程视图模型，承载页面展示数据。 */
    public record NotificationTabView(String key, String label) {
    }

    /** 审批流程视图模型，承载页面展示数据。 */
    public record WorkflowApprovalNotificationView(Long id,
                                                   String approvalNo,
                                                   String workflowName,
                                                   String approverName,
                                                   String approverRole,
                                                   String auditedAt,
                                                   String result,
                                                   String remark,
                                                   String routePath,
                                                   String businessCode,
                                                   Long businessId) {
    }
}
