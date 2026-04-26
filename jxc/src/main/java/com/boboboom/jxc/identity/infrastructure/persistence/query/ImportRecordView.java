package com.boboboom.jxc.identity.infrastructure.persistence.query;

import java.time.LocalDateTime;

/** 身份与权限视图模型，承载页面展示数据。 */
public class ImportRecordView {

    private Long id;
    private Long batchId;
    private String sourceRecordId;
    private String realName;
    private String phone;
    private String validateStatus;
    private String processStatus;
    private String failureReason;
    private Long userId;
    private String linkedUserStatus;
    private LocalDateTime createdAt;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取BatchId。 */
    public Long getBatchId() {
        return batchId;
    }

    /** 设置BatchId。 */
    public void setBatchId(Long batchIdValue) {
        this.batchId = batchIdValue;
    }

    /** 获取SourceRecordId。 */
    public String getSourceRecordId() {
        return sourceRecordId;
    }

    /** 设置SourceRecordId。 */
    public void setSourceRecordId(String sourceRecordIdValue) {
        this.sourceRecordId = sourceRecordIdValue;
    }

    /** 获取RealName。 */
    public String getRealName() {
        return realName;
    }

    /** 设置RealName。 */
    public void setRealName(String realNameValue) {
        this.realName = realNameValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }

    /** 获取ValidateStatus。 */
    public String getValidateStatus() {
        return validateStatus;
    }

    /** 设置ValidateStatus。 */
    public void setValidateStatus(String validateStatusValue) {
        this.validateStatus = validateStatusValue;
    }

    /** 获取ProcessStatus。 */
    public String getProcessStatus() {
        return processStatus;
    }

    /** 设置ProcessStatus。 */
    public void setProcessStatus(String processStatusValue) {
        this.processStatus = processStatusValue;
    }

    /** 获取FailureReason。 */
    public String getFailureReason() {
        return failureReason;
    }

    /** 设置FailureReason。 */
    public void setFailureReason(String failureReasonValue) {
        this.failureReason = failureReasonValue;
    }

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取LinkedUserStatus。 */
    public String getLinkedUserStatus() {
        return linkedUserStatus;
    }

    /** 设置LinkedUserStatus。 */
    public void setLinkedUserStatus(String linkedUserStatusValue) {
        this.linkedUserStatus = linkedUserStatusValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }
}

