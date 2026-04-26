package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class GroupStoreCreateRequest {

    private String storeCode;

    @NotBlank
    private String storeName;

    private String status;
    private String contactName;
    private String contactPhone;
    private String address;
    private String remark;
    private Long adminUserId;

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
}
