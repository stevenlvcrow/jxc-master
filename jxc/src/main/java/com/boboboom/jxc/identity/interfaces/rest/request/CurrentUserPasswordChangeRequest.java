package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class CurrentUserPasswordChangeRequest {

    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;

    /** 获取OldPassword。 */
    public String getOldPassword() {
        return oldPassword;
    }

    /** 设置OldPassword。 */
    public void setOldPassword(String oldPasswordValue) {
        this.oldPassword = oldPasswordValue;
    }

    /** 获取NewPassword。 */
    public String getNewPassword() {
        return newPassword;
    }

    /** 设置NewPassword。 */
    public void setNewPassword(String newPasswordValue) {
        this.newPassword = newPasswordValue;
    }
}
