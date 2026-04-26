package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class GroupUpsertRequest {

    private String groupCode;

    @NotBlank
    private String groupName;

    private String status;
    private String remark;
    private String adminRealName;
    private String adminPhone;

    /** 获取GroupCode。 */
    public String getGroupCode() {
        return groupCode;
    }

    /** 设置GroupCode。 */
    public void setGroupCode(String groupCodeValue) {
        this.groupCode = groupCodeValue;
    }

    /** 获取GroupName。 */
    public String getGroupName() {
        return groupName;
    }

    /** 设置GroupName。 */
    public void setGroupName(String groupNameValue) {
        this.groupName = groupNameValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
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
}
