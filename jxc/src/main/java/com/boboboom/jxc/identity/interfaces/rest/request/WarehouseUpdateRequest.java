package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

/** 身份与权限请求参数，承载接口入参。 */
public class WarehouseUpdateRequest {

    @NotBlank
    private String warehouseName;

    private String department;
    private String status;
    private String warehouseType;
    private String contactName;
    private String contactPhone;
    private String regionPath;
    private String address;
    private String targetGrossMargin;
    private String idealPurchaseSaleRatio;

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }
    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取Department。 */
    public String getDepartment() {
        return department;
    }
    /** 设置Department。 */
    public void setDepartment(String departmentValue) {
        this.department = departmentValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }
    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取WarehouseType。 */
    public String getWarehouseType() {
        return warehouseType;
    }
    /** 设置WarehouseType。 */
    public void setWarehouseType(String warehouseTypeValue) {
        this.warehouseType = warehouseTypeValue;
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

    /** 获取RegionPath。 */
    public String getRegionPath() {
        return regionPath;
    }
    /** 设置RegionPath。 */
    public void setRegionPath(String regionPathValue) {
        this.regionPath = regionPathValue;
    }

    /** 获取Address。 */
    public String getAddress() {
        return address;
    }
    /** 设置Address。 */
    public void setAddress(String addressValue) {
        this.address = addressValue;
    }

    /** 获取TargetGrossMargin。 */
    public String getTargetGrossMargin() {
        return targetGrossMargin;
    }
    /** 设置TargetGrossMargin。 */
    public void setTargetGrossMargin(String targetGrossMarginValue) {
        this.targetGrossMargin = targetGrossMarginValue;
    }

    /** 获取IdealPurchaseSaleRatio。 */
    public String getIdealPurchaseSaleRatio() {
        return idealPurchaseSaleRatio;
    }
    /** 设置IdealPurchaseSaleRatio。 */
    public void setIdealPurchaseSaleRatio(String idealPurchaseSaleRatioValue) {
        this.idealPurchaseSaleRatio = idealPurchaseSaleRatioValue;
    }
}
