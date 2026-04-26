package com.boboboom.jxc.workflow.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 审批流程数据对象，映射数据库表记录。 */
@TableName("workflow_process_store_binding")
public class WorkflowProcessStoreBindingDO extends BaseAuditDO {

    private Long processRegistryId;
    private Long groupId;
    private Long storeId;
    private Long createdBy;
    private Long updatedBy;

    /** 获取ProcessRegistryId。 */
    public Long getProcessRegistryId() {
        return processRegistryId;
    }

    /** 设置ProcessRegistryId。 */
    public void setProcessRegistryId(Long processRegistryIdValue) {
        this.processRegistryId = processRegistryIdValue;
    }

    /** 获取GroupId。 */
    public Long getGroupId() {
        return groupId;
    }

    /** 设置GroupId。 */
    public void setGroupId(Long groupIdValue) {
        this.groupId = groupIdValue;
    }

    /** 获取StoreId。 */
    public Long getStoreId() {
        return storeId;
    }

    /** 设置StoreId。 */
    public void setStoreId(Long storeIdValue) {
        this.storeId = storeIdValue;
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

