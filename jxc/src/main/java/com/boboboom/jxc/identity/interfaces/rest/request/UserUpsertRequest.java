package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public class UserUpsertRequest {

    @NotBlank
    private String realName;

    @NotBlank
    private String phone;

    private String status;

    public static class UserBatchDeleteRequest {

        @jakarta.validation.constraints.NotEmpty
        private java.util.List<Long> ids;

        public java.util.List<Long> getIds() {
            return ids;
        }

        public void setIds(java.util.List<Long> ids) {
            this.ids = ids;
        }
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
