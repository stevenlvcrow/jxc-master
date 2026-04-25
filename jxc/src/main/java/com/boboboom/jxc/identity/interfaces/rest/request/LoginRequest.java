package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class LoginRequest {

    @NotBlank
    private String phone;

    @NotBlank
    private String password;

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }

    /** 获取Password。 */
    public String getPassword() {
        return password;
    }

    /** 设置Password。 */
    public void setPassword(String passwordValue) {
        this.password = passwordValue;
    }
}
