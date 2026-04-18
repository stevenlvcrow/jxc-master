package com.boboboom.jxc.workflow.application.service;

import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.workflow.domain.repository.WorkflowApprovalNotificationRepository;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class WorkflowApprovalNotificationApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    private final WorkflowApprovalNotificationRepository notificationRepository;
    private final OrgScopeService orgScopeService;
    private final WorkflowActionService workflowActionService;

    public WorkflowApprovalNotificationApplicationService(WorkflowApprovalNotificationRepository notificationRepository,
                                                         OrgScopeService orgScopeService,
                                                         WorkflowActionService workflowActionService) {
        this.notificationRepository = notificationRepository;
        this.orgScopeService = orgScopeService;
        this.workflowActionService = workflowActionService;
    }

    public void record(String scopeType,
                       Long scopeId,
                       String businessCode,
                       String businessName,
                       Long businessId,
                       String approvalNo,
                       String approverName,
                       String approverRole,
                       Long targetApproverUserId,
                       String targetApproverRoleCode,
                       String targetApproverRoleName,
                       LocalDateTime auditedAt,
                       String result,
                       String remark,
                       String routePath) {
        if (!StringUtils.hasText(scopeType) || scopeId == null || businessId == null || !StringUtils.hasText(approvalNo)) {
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
            notification.setBusinessCode(StringUtils.hasText(businessCode) ? businessCode : "");
            notification.setBusinessId(businessId);
            notification.setApprovalNo(approvalNo);
        }
        notification.setScopeType(scopeType);
        notification.setScopeId(scopeId);
        notification.setBusinessCode(StringUtils.hasText(businessCode) ? businessCode : "");
        notification.setBusinessName(StringUtils.hasText(businessName) ? businessName : "");
        notification.setBusinessId(businessId);
        notification.setApprovalNo(approvalNo);
        notification.setApproverName(StringUtils.hasText(approverName) ? approverName : "system");
        notification.setApproverRole(StringUtils.hasText(approverRole) ? approverRole : "普通审核");
        if (targetApproverUserId != null) {
            notification.setTargetApproverUserId(targetApproverUserId);
        }
        if (StringUtils.hasText(targetApproverRoleCode)) {
            notification.setTargetApproverRoleCode(targetApproverRoleCode);
        }
        if (StringUtils.hasText(targetApproverRoleName)) {
            notification.setTargetApproverRoleName(targetApproverRoleName);
        }
        notification.setAuditedAt(auditedAt == null ? LocalDateTime.now() : auditedAt);
        notification.setResult(StringUtils.hasText(result) ? result : "通过");
        notification.setRemark(StringUtils.hasText(remark) ? remark : "");
        notification.setRoutePath(StringUtils.hasText(routePath) ? routePath : "");
        if (notification.getId() == null) {
            notificationRepository.save(notification);
            return;
        }
        notificationRepository.update(notification);
    }

    public long pendingCount(String orgId) {
        Long operatorId = AuthContextHolder.requireUserId("登录已失效，请重新登录");
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(operatorId, orgId);
        List<WorkflowApprovalNotificationDO> rows = notificationRepository.findByScopeOrdered(
                scope.scopeType(),
                scope.scopeId()
        );
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

    public PageData<WorkflowApprovalNotificationView> page(String orgId, Integer pageNum, Integer pageSize) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(
                AuthContextHolder.requireUserId("登录已失效，请重新登录"),
                orgId
        );
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        List<WorkflowApprovalNotificationDO> rows = notificationRepository.findByScopeOrdered(
                scope.scopeType(),
                scope.scopeId()
        );
        if (rows.isEmpty()) {
            return new PageData<>(List.of(), 0, safePageNum, safePageSize);
        }
        int start = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int end = Math.min(start + safePageSize, rows.size());
        List<WorkflowApprovalNotificationView> list = rows.subList(start, end).stream()
                .map(this::toView)
                .toList();
        return new PageData<>(list, rows.size(), safePageNum, safePageSize);
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

    public record PageData<T>(List<T> list, long total, int pageNum, int pageSize) {
    }

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
