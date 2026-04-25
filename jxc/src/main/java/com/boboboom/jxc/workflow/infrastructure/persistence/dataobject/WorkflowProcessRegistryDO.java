package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 审批流程数据对象，映射数据库表记录。 */
@TableName("workflow_process_registry")
public class WorkflowProcessRegistryDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String processCode;
    private String businessName;
    private String templateId;
    private Long createdBy;
    private Long updatedBy;

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

    /** 获取ProcessCode。 */
    public String getProcessCode() {
        return processCode;
    }

    /** 设置ProcessCode。 */
    public void setProcessCode(String processCodeValue) {
        this.processCode = processCodeValue;
    }

    /** 获取BusinessName。 */
    public String getBusinessName() {
        return businessName;
    }

    /** 设置BusinessName。 */
    public void setBusinessName(String businessNameValue) {
        this.businessName = businessNameValue;
    }

    /** 获取TemplateId。 */
    public String getTemplateId() {
        return templateId;
    }

    /** 设置TemplateId。 */
    public void setTemplateId(String templateIdValue) {
        this.templateId = templateIdValue;
    }

    /** 获取CreatedBy。 */
    public Long getCreatedBy() {
        return createdBy;
    }

    /** 设置CreatedBy。 */
    public void setCreatedBy(Long createdByValue) {
        this.createdBy = createdByValue;
    }

    /** 获取UpdatedBy。 */
    public Long getUpdatedBy() {
        return updatedBy;
    }

    /** 设置UpdatedBy。 */
    public void setUpdatedBy(Long updatedByValue) {
        this.updatedBy = updatedByValue;
    }
}
