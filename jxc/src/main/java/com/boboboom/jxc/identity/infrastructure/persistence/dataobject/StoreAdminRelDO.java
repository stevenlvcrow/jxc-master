package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_store_admin_rel")
public class StoreAdminRelDO extends BaseIdDO {

    private Long storeId;
    private Long userId;
    private Long assignedBy;
    private LocalDateTime assignedAt;
    private String status;

    /** 获取StoreId。 */
    public Long getStoreId() {
        return storeId;
    }

    /** 设置StoreId。 */
    public void setStoreId(Long storeIdValue) {
        this.storeId = storeIdValue;
    }

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取AssignedBy。 */
    public Long getAssignedBy() {
        return assignedBy;
    }

    /** 设置AssignedBy。 */
    public void setAssignedBy(Long assignedByValue) {
        this.assignedBy = assignedByValue;
    }

    /** 获取AssignedAt。 */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    /** 设置AssignedAt。 */
    public void setAssignedAt(LocalDateTime assignedAtValue) {
        this.assignedAt = assignedAtValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }
}

