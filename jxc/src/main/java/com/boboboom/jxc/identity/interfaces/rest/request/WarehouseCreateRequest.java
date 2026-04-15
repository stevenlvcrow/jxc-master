package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;

public class WarehouseCreateRequest {

    @NotBlank
    private String warehouseCode;

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

    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getWarehouseType() { return warehouseType; }
    public void setWarehouseType(String warehouseType) { this.warehouseType = warehouseType; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getRegionPath() { return regionPath; }
    public void setRegionPath(String regionPath) { this.regionPath = regionPath; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getTargetGrossMargin() { return targetGrossMargin; }
    public void setTargetGrossMargin(String targetGrossMargin) { this.targetGrossMargin = targetGrossMargin; }

    public String getIdealPurchaseSaleRatio() { return idealPurchaseSaleRatio; }
    public void setIdealPurchaseSaleRatio(String idealPurchaseSaleRatio) { this.idealPurchaseSaleRatio = idealPurchaseSaleRatio; }
}
