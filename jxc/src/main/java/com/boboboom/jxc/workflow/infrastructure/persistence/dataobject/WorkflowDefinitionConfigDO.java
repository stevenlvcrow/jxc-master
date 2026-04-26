package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 审批流程数据对象，映射数据库表记录。 */
@TableName("workflow_definition_config")
public class WorkflowDefinitionConfigDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String businessCode;
    private String workflowCode;
    private String workflowName;
    private String status;
    private Integer versionNo;
    private String nodeConfigJson;
    private String processDefinitionKey;
    private String processDefinitionId;
    private LocalDateTime deployedAt;
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

    /** 获取BusinessCode。 */
    public String getBusinessCode() {
        return businessCode;
    }

    /** 设置BusinessCode。 */
    public void setBusinessCode(String businessCodeValue) {
        this.businessCode = businessCodeValue;
    }

    /** 获取WorkflowCode。 */
    public String getWorkflowCode() {
        return workflowCode;
    }

    /** 设置WorkflowCode。 */
    public void setWorkflowCode(String workflowCodeValue) {
        this.workflowCode = workflowCodeValue;
    }

    /** 获取WorkflowName。 */
    public String getWorkflowName() {
        return workflowName;
    }

    /** 设置WorkflowName。 */
    public void setWorkflowName(String workflowNameValue) {
        this.workflowName = workflowNameValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取VersionNo。 */
    public Integer getVersionNo() {
        return versionNo;
    }

    /** 设置VersionNo。 */
    public void setVersionNo(Integer versionNoValue) {
        this.versionNo = versionNoValue;
    }

    /** 获取NodeConfigJson。 */
    public String getNodeConfigJson() {
        return nodeConfigJson;
    }

    /** 设置NodeConfigJson。 */
    public void setNodeConfigJson(String nodeConfigJsonValue) {
        this.nodeConfigJson = nodeConfigJsonValue;
    }

    /** 获取ProcessDefinitionKey。 */
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    /** 设置ProcessDefinitionKey。 */
    public void setProcessDefinitionKey(String processDefinitionKeyValue) {
        this.processDefinitionKey = processDefinitionKeyValue;
    }

    /** 获取ProcessDefinitionId。 */
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    /** 设置ProcessDefinitionId。 */
    public void setProcessDefinitionId(String processDefinitionIdValue) {
        this.processDefinitionId = processDefinitionIdValue;
    }

    /** 获取DeployedAt。 */
    public LocalDateTime getDeployedAt() {
        return deployedAt;
    }

    /** 设置DeployedAt。 */
    public void setDeployedAt(LocalDateTime deployedAtValue) {
        this.deployedAt = deployedAtValue;
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
