package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

@TableName("dev.workflow_process_store_binding")
public class WorkflowProcessStoreBindingDO extends BaseAuditDO {

    private Long processRegistryId;
    private Long groupId;
    private Long storeId;
    private Long createdBy;
    private Long updatedBy;

    public Long getProcessRegistryId() {
        return processRegistryId;
    }

    public void setProcessRegistryId(Long processRegistryId) {
        this.processRegistryId = processRegistryId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }
}

