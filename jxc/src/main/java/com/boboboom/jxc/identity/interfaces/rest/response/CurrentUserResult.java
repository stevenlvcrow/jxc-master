package com.boboboom.jxc.identity.interfaces.rest.response;

/** 身份与权限结果模型，承载业务处理结果。 */
public class CurrentUserResult {

    private Long userId;
    private String userName;
    private String account;
    private String phone;

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取UserName。 */
    public String getUserName() {
        return userName;
    }

    /** 设置UserName。 */
    public void setUserName(String userNameValue) {
        this.userName = userNameValue;
    }

    /** 获取Account。 */
    public String getAccount() {
        return account;
    }

    /** 设置Account。 */
    public void setAccount(String accountValue) {
        this.account = accountValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
    }
}
