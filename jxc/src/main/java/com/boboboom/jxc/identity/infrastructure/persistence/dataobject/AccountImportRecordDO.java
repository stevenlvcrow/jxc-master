package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("ext_account_import_record")
public class AccountImportRecordDO extends BaseAuditDO {

    private Long batchId;
    private String sourceSystem;
    private String sourceRecordId;
    private String realName;
    private String phone;
    private String validateStatus;
    private String processStatus;
    private String failureReason;
    private Long userId;
    private String payloadJson;

    /** 获取BatchId。 */
    public Long getBatchId() {
        return batchId;
    }

    /** 设置BatchId。 */
    public void setBatchId(Long batchIdValue) {
        this.batchId = batchIdValue;
    }

    /** 获取SourceSystem。 */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /** 设置SourceSystem。 */
    public void setSourceSystem(String sourceSystemValue) {
        this.sourceSystem = sourceSystemValue;
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

    /** 获取PayloadJson。 */
    public String getPayloadJson() {
        return payloadJson;
    }

    /** 设置PayloadJson。 */
    public void setPayloadJson(String payloadJsonValue) {
        this.payloadJson = payloadJsonValue;
    }
}

