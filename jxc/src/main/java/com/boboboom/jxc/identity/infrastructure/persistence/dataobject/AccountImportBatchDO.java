package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("ext_account_import_batch")
public class AccountImportBatchDO extends BaseAuditDO {

    private String batchNo;
    private String sourceSystem;
    private String mqTopic;
    private String mqMessageKey;
    private Integer totalCount;
    private Integer successCount;
    private Integer ignoredCount;
    private Integer failedCount;
    private String processStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    /** 获取BatchNo。 */
    public String getBatchNo() {
        return batchNo;
    }

    /** 设置BatchNo。 */
    public void setBatchNo(String batchNoValue) {
        this.batchNo = batchNoValue;
    }

    /** 获取SourceSystem。 */
    public String getSourceSystem() {
        return sourceSystem;
    }

    /** 设置SourceSystem。 */
    public void setSourceSystem(String sourceSystemValue) {
        this.sourceSystem = sourceSystemValue;
    }

    /** 获取MqTopic。 */
    public String getMqTopic() {
        return mqTopic;
    }

    /** 设置MqTopic。 */
    public void setMqTopic(String mqTopicValue) {
        this.mqTopic = mqTopicValue;
    }

    /** 获取MqMessageKey。 */
    public String getMqMessageKey() {
        return mqMessageKey;
    }

    /** 设置MqMessageKey。 */
    public void setMqMessageKey(String mqMessageKeyValue) {
        this.mqMessageKey = mqMessageKeyValue;
    }

    /** 获取TotalCount。 */
    public Integer getTotalCount() {
        return totalCount;
    }

    /** 设置TotalCount。 */
    public void setTotalCount(Integer totalCountValue) {
        this.totalCount = totalCountValue;
    }

    /** 获取SuccessCount。 */
    public Integer getSuccessCount() {
        return successCount;
    }

    /** 设置SuccessCount。 */
    public void setSuccessCount(Integer successCountValue) {
        this.successCount = successCountValue;
    }

    /** 获取IgnoredCount。 */
    public Integer getIgnoredCount() {
        return ignoredCount;
    }

    /** 设置IgnoredCount。 */
    public void setIgnoredCount(Integer ignoredCountValue) {
        this.ignoredCount = ignoredCountValue;
    }

    /** 获取FailedCount。 */
    public Integer getFailedCount() {
        return failedCount;
    }

    /** 设置FailedCount。 */
    public void setFailedCount(Integer failedCountValue) {
        this.failedCount = failedCountValue;
    }

    /** 获取ProcessStatus。 */
    public String getProcessStatus() {
        return processStatus;
    }

    /** 设置ProcessStatus。 */
    public void setProcessStatus(String processStatusValue) {
        this.processStatus = processStatusValue;
    }

    /** 获取StartedAt。 */
    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    /** 设置StartedAt。 */
    public void setStartedAt(LocalDateTime startedAtValue) {
        this.startedAt = startedAtValue;
    }

    /** 获取FinishedAt。 */
    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    /** 设置FinishedAt。 */
    public void setFinishedAt(LocalDateTime finishedAtValue) {
        this.finishedAt = finishedAtValue;
    }
}

