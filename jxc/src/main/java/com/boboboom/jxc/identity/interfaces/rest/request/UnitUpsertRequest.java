package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class UnitUpsertRequest {

    private String code;

    @NotBlank
    private String name;

    private String type;
    private String status;
    private String remark;

    /** 获取Code。 */
    public String getCode() {
        return code;
    }

    /** 设置Code。 */
    public void setCode(String codeValue) {
        this.code = codeValue;
    }

    /** 获取Name。 */
    public String getName() {
        return name;
    }

    /** 设置Name。 */
    public void setName(String nameValue) {
        this.name = nameValue;
    }

    /** 获取Type。 */
    public String getType() {
        return type;
    }

    /** 设置Type。 */
    public void setType(String typeValue) {
        this.type = typeValue;
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
}
