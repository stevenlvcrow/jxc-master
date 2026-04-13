package com.boboboom.jxc.identity.interfaces.rest.response;

import java.util.List;

public class OrgNodeResult {

    private String id;
    private String name;
    private String merchantNo;
    private String code;
    private String city;
    private String type;
    private List<OrgNodeResult> children;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMerchantNo() {
        return merchantNo;
    }

    public void setMerchantNo(String merchantNo) {
        this.merchantNo = merchantNo;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<OrgNodeResult> getChildren() {
        return children;
    }

    public void setChildren(List<OrgNodeResult> children) {
        this.children = children;
    }
}
