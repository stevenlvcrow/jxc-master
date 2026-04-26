package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 审批流程数据对象，映射数据库表记录。 */
@TableName("workflow_approval_notification")
public class WorkflowApprovalNotificationDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String businessCode;
    private String businessName;
    private Long businessId;
    private String approvalNo;
    private Long approverUserId;
    private String approverName;
    private String approverRole;
    private Long targetApproverUserId;
    private String targetApproverRoleCode;
    private String targetApproverRoleName;
    private LocalDateTime auditedAt;
    private String result;
    private String remark;
    private String routePath;

    /** 获取ScopeType。 */
    public String getScopeType() {
        return scopeType;
    }

    /** 设置ScopeType。 */
    public void setScopeType(String scopeTypeValue) {
        this.scopeType = scopeTypeValue;
    }

    /** 获取ScopeId。 */
    public Long getScopeId() {
        return scopeId;
    }

    /** 设置ScopeId。 */
    public void setScopeId(Long scopeIdValue) {
        this.scopeId = scopeIdValue;
    }

    /** 获取BusinessCode。 */
    public String getBusinessCode() {
        return businessCode;
    }

    /** 设置BusinessCode。 */
    public void setBusinessCode(String businessCodeValue) {
        this.businessCode = businessCodeValue;
    }

    /** 获取BusinessName。 */
    public String getBusinessName() {
        return businessName;
    }

    /** 设置BusinessName。 */
    public void setBusinessName(String businessNameValue) {
        this.businessName = businessNameValue;
    }

    /** 获取BusinessId。 */
    public Long getBusinessId() {
        return businessId;
    }

    /** 设置BusinessId。 */
    public void setBusinessId(Long businessIdValue) {
        this.businessId = businessIdValue;
    }

    /** 获取ApprovalNo。 */
    public String getApprovalNo() {
        return approvalNo;
    }

    /** 设置ApprovalNo。 */
    public void setApprovalNo(String approvalNoValue) {
        this.approvalNo = approvalNoValue;
    }

    /** 获取ApproverName。 */
    public String getApproverName() {
        return approverName;
    }

    /** 获取ApproverUserId。 */
    public Long getApproverUserId() {
        return approverUserId;
    }

    /** 设置ApproverUserId。 */
    public void setApproverUserId(Long approverUserIdValue) {
        this.approverUserId = approverUserIdValue;
    }

    /** 设置ApproverName。 */
    public void setApproverName(String approverNameValue) {
        this.approverName = approverNameValue;
    }

    /** 获取ApproverRole。 */
    public String getApproverRole() {
        return approverRole;
    }

    /** 设置ApproverRole。 */
    public void setApproverRole(String approverRoleValue) {
        this.approverRole = approverRoleValue;
    }

    /** 获取TargetApproverUserId。 */
    public Long getTargetApproverUserId() {
        return targetApproverUserId;
    }

    /** 设置TargetApproverUserId。 */
    public void setTargetApproverUserId(Long targetApproverUserIdValue) {
        this.targetApproverUserId = targetApproverUserIdValue;
    }

    /** 获取TargetApproverRoleCode。 */
    public String getTargetApproverRoleCode() {
        return targetApproverRoleCode;
    }

    /** 设置TargetApproverRoleCode。 */
    public void setTargetApproverRoleCode(String targetApproverRoleCodeValue) {
        this.targetApproverRoleCode = targetApproverRoleCodeValue;
    }

    /** 获取TargetApproverRoleName。 */
    public String getTargetApproverRoleName() {
        return targetApproverRoleName;
    }

    /** 设置TargetApproverRoleName。 */
    public void setTargetApproverRoleName(String targetApproverRoleNameValue) {
        this.targetApproverRoleName = targetApproverRoleNameValue;
    }

    /** 获取AuditedAt。 */
    public LocalDateTime getAuditedAt() {
        return auditedAt;
    }

    /** 设置AuditedAt。 */
    public void setAuditedAt(LocalDateTime auditedAtValue) {
        this.auditedAt = auditedAtValue;
    }

    /** 获取Result。 */
    public String getResult() {
        return result;
    }

    /** 设置Result。 */
    public void setResult(String resultValue) {
        this.result = resultValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }

    /** 获取RoutePath。 */
    public String getRoutePath() {
        return routePath;
    }

    /** 设置RoutePath。 */
    public void setRoutePath(String routePathValue) {
        this.routePath = routePathValue;
    }
}
