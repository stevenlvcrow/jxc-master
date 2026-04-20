package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

import java.time.LocalDateTime;

@TableName("dev.workflow_approval_notification")
public class WorkflowApprovalNotificationDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String businessCode;
    private String businessName;
    private Long businessId;
    private String approvalNo;
    private String approverName;
    private String approverRole;
    private Long targetApproverUserId;
    private String targetApproverRoleCode;
    private String targetApproverRoleName;
    private LocalDateTime auditedAt;
    private String result;
    private String remark;
    private String routePath;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(String approvalNo) {
        this.approvalNo = approvalNo;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverRole() {
        return approverRole;
    }

    public void setApproverRole(String approverRole) {
        this.approverRole = approverRole;
    }

    public Long getTargetApproverUserId() {
        return targetApproverUserId;
    }

    public void setTargetApproverUserId(Long targetApproverUserId) {
        this.targetApproverUserId = targetApproverUserId;
    }

    public String getTargetApproverRoleCode() {
        return targetApproverRoleCode;
    }

    public void setTargetApproverRoleCode(String targetApproverRoleCode) {
        this.targetApproverRoleCode = targetApproverRoleCode;
    }

    public String getTargetApproverRoleName() {
        return targetApproverRoleName;
    }

    public void setTargetApproverRoleName(String targetApproverRoleName) {
        this.targetApproverRoleName = targetApproverRoleName;
    }

    public LocalDateTime getAuditedAt() {
        return auditedAt;
    }

    public void setAuditedAt(LocalDateTime auditedAt) {
        this.auditedAt = auditedAt;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }
}
