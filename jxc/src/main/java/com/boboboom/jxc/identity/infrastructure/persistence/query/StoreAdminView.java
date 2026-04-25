package com.boboboom.jxc.identity.infrastructure.persistence.query;

/** 身份与权限视图模型，承载页面展示数据。 */
public class StoreAdminView {

    private Long storeId;
    private String storeCode;
    private String storeName;
    private Long groupId;
    private String groupName;
    private Long adminUserId;
    private String adminRealName;
    private String adminPhone;
    private String status;

    /** 获取StoreId。 */
    public Long getStoreId() {
        return storeId;
    }

    /** 设置StoreId。 */
    public void setStoreId(Long storeIdValue) {
        this.storeId = storeIdValue;
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

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }
}

