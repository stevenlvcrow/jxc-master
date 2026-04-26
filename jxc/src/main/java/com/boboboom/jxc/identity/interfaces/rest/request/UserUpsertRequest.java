package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class UserUpsertRequest {

    @NotBlank
    private String realName;

    @NotBlank
    private String phone;

    private String status;

    /**
     * 用户批量删除请求。
     */
    public static class UserBatchDeleteRequest {

        @jakarta.validation.constraints.NotEmpty
        private java.util.List<Long> ids;

        /** 获取Ids。 */
        public java.util.List<Long> getIds() {
            return ids;
        }

        /** 设置Ids。 */
        public void setIds(java.util.List<Long> idsValue) {
            this.ids = idsValue;
        }
    }

    /** 获取RealName。 */
    public String getRealName() {
        return realName;
    }

    /** 设置RealName。 */
    public void setRealName(String realNameValue) {
        this.realName = realNameValue;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 设置Phone。 */
    public void setPhone(String phoneValue) {
        this.phone = phoneValue;
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
