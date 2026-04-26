package com.boboboom.jxc.identity.interfaces.rest.response;

import java.util.List;

/** 身份与权限结果模型，承载业务处理结果。 */
public class OrgNodeResult {

    private String id;
    private String name;
    private String merchantNo;
    private String code;
    private String city;
    private String type;
    private Boolean selectable;
    private List<OrgNodeResult> children;

    /** 获取Id。 */
    public String getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(String idValue) {
        this.id = idValue;
    }

    /** 获取Name。 */
    public String getName() {
        return name;
    }

    /** 设置Name。 */
    public void setName(String nameValue) {
        this.name = nameValue;
    }

    /** 获取MerchantNo。 */
    public String getMerchantNo() {
        return merchantNo;
    }

    /** 设置MerchantNo。 */
    public void setMerchantNo(String merchantNoValue) {
        this.merchantNo = merchantNoValue;
    }

    /** 获取Code。 */
    public String getCode() {
        return code;
    }

    /** 设置Code。 */
    public void setCode(String codeValue) {
        this.code = codeValue;
    }

    /** 获取City。 */
    public String getCity() {
        return city;
    }

    /** 设置City。 */
    public void setCity(String cityValue) {
        this.city = cityValue;
    }

    /** 获取Type。 */
    public String getType() {
        return type;
    }

    /** 设置Type。 */
    public void setType(String typeValue) {
        this.type = typeValue;
    }

    /** 获取Selectable。 */
    public Boolean getSelectable() {
        return selectable;
    }

    /** 设置Selectable。 */
    public void setSelectable(Boolean selectableValue) {
        this.selectable = selectableValue;
    }

    /** 获取Children。 */
    public List<OrgNodeResult> getChildren() {
        return children;
    }

    /** 设置Children。 */
    public void setChildren(List<OrgNodeResult> childrenValue) {
        this.children = childrenValue;
    }
}
