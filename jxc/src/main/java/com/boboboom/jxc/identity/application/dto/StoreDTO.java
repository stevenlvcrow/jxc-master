package com.boboboom.jxc.identity.application.dto;

import java.time.LocalDateTime;

/** 身份与权限类型，负责门店传输对象相关处理。 */
public class StoreDTO {

    private Long id;
    private Long groupId;
    private String groupName;
    private String storeCode;
    private String storeName;
    private String status;
    private String contactName;
    private String contactPhone;
    private String address;
    private String remark;
    private Long adminUserId;
    private String adminRealName;
    private String adminPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取GroupId。 */
    public Long getGroupId() {
        return groupId;
    }

    /** 设置GroupId。 */
    public void setGroupId(Long groupIdValue) {
        this.groupId = groupIdValue;
    }

    /** 获取GroupName。 */
    public String getGroupName() {
        return groupName;
    }

    /** 设置GroupName。 */
    public void setGroupName(String groupNameValue) {
        this.groupName = groupNameValue;
    }

    /** 获取StoreCode。 */
    public String getStoreCode() {
        return storeCode;
    }

    /** 设置StoreCode。 */
    public void setStoreCode(String storeCodeValue) {
        this.storeCode = storeCodeValue;
    }

    /** 获取StoreName。 */
    public String getStoreName() {
        return storeName;
    }

    /** 设置StoreName。 */
    public void setStoreName(String storeNameValue) {
        this.storeName = storeNameValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取ContactName。 */
    public String getContactName() {
        return contactName;
    }

    /** 设置ContactName。 */
    public void setContactName(String contactNameValue) {
        this.contactName = contactNameValue;
    }

    /** 获取ContactPhone。 */
    public String getContactPhone() {
        return contactPhone;
    }

    /** 设置ContactPhone。 */
    public void setContactPhone(String contactPhoneValue) {
        this.contactPhone = contactPhoneValue;
    }

    /** 获取Address。 */
    public String getAddress() {
        return address;
    }

    /** 设置Address。 */
    public void setAddress(String addressValue) {
        this.address = addressValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }

    /** 获取AdminUserId。 */
    public Long getAdminUserId() {
        return adminUserId;
    }

    /** 设置AdminUserId。 */
    public void setAdminUserId(Long adminUserIdValue) {
        this.adminUserId = adminUserIdValue;
    }

    /** 获取AdminRealName。 */
    public String getAdminRealName() {
        return adminRealName;
    }

    /** 设置AdminRealName。 */
    public void setAdminRealName(String adminRealNameValue) {
        this.adminRealName = adminRealNameValue;
    }

    /** 获取AdminPhone。 */
    public String getAdminPhone() {
        return adminPhone;
    }

    /** 设置AdminPhone。 */
    public void setAdminPhone(String adminPhoneValue) {
        this.adminPhone = adminPhoneValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置UpdatedAt。 */
    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }
}
