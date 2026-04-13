package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

import java.math.BigDecimal;

@TableName("dev.supplier_profile")
public class SupplierProfileDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String supplierCode;
    private String supplierName;
    private String supplierShortName;
    private String supplierMnemonic;
    private String supplierCategory;
    private BigDecimal taxRate;
    private String status;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String contactAddress;
    private String remark;
    private String settlementMethod;
    private String orderSummaryRule;
    private Boolean inputBatchWhenDelivery;
    private Boolean syncReceiptData;
    private String purchaseReceiptDependShipping;
    private String deliveryDependShipping;
    private Boolean supplierManageInventory;
    private Boolean controlOrderTime;
    private Boolean allowCloseOrder;
    private String reconciliationMode;
    private String scopeControl;
    private String source;
    private String supplyRelation;
    private String bindStatus;
    private String invoiceCompanyName;
    private String taxpayerId;
    private String invoicePhone;
    private String invoiceAddress;

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierShortName() {
        return supplierShortName;
    }

    public void setSupplierShortName(String supplierShortName) {
        this.supplierShortName = supplierShortName;
    }

    public String getSupplierMnemonic() {
        return supplierMnemonic;
    }

    public void setSupplierMnemonic(String supplierMnemonic) {
        this.supplierMnemonic = supplierMnemonic;
    }

    public String getSupplierCategory() {
        return supplierCategory;
    }

    public void setSupplierCategory(String supplierCategory) {
        this.supplierCategory = supplierCategory;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getSettlementMethod() {
        return settlementMethod;
    }

    public void setSettlementMethod(String settlementMethod) {
        this.settlementMethod = settlementMethod;
    }

    public String getOrderSummaryRule() {
        return orderSummaryRule;
    }

    public void setOrderSummaryRule(String orderSummaryRule) {
        this.orderSummaryRule = orderSummaryRule;
    }

    public Boolean getInputBatchWhenDelivery() {
        return inputBatchWhenDelivery;
    }

    public void setInputBatchWhenDelivery(Boolean inputBatchWhenDelivery) {
        this.inputBatchWhenDelivery = inputBatchWhenDelivery;
    }

    public Boolean getSyncReceiptData() {
        return syncReceiptData;
    }

    public void setSyncReceiptData(Boolean syncReceiptData) {
        this.syncReceiptData = syncReceiptData;
    }

    public String getPurchaseReceiptDependShipping() {
        return purchaseReceiptDependShipping;
    }

    public void setPurchaseReceiptDependShipping(String purchaseReceiptDependShipping) {
        this.purchaseReceiptDependShipping = purchaseReceiptDependShipping;
    }

    public String getDeliveryDependShipping() {
        return deliveryDependShipping;
    }

    public void setDeliveryDependShipping(String deliveryDependShipping) {
        this.deliveryDependShipping = deliveryDependShipping;
    }

    public Boolean getSupplierManageInventory() {
        return supplierManageInventory;
    }

    public void setSupplierManageInventory(Boolean supplierManageInventory) {
        this.supplierManageInventory = supplierManageInventory;
    }

    public Boolean getControlOrderTime() {
        return controlOrderTime;
    }

    public void setControlOrderTime(Boolean controlOrderTime) {
        this.controlOrderTime = controlOrderTime;
    }

    public Boolean getAllowCloseOrder() {
        return allowCloseOrder;
    }

    public void setAllowCloseOrder(Boolean allowCloseOrder) {
        this.allowCloseOrder = allowCloseOrder;
    }

    public String getReconciliationMode() {
        return reconciliationMode;
    }

    public void setReconciliationMode(String reconciliationMode) {
        this.reconciliationMode = reconciliationMode;
    }

    public String getScopeControl() {
        return scopeControl;
    }

    public void setScopeControl(String scopeControl) {
        this.scopeControl = scopeControl;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSupplyRelation() {
        return supplyRelation;
    }

    public void setSupplyRelation(String supplyRelation) {
        this.supplyRelation = supplyRelation;
    }

    public String getBindStatus() {
        return bindStatus;
    }

    public void setBindStatus(String bindStatus) {
        this.bindStatus = bindStatus;
    }

    public String getInvoiceCompanyName() {
        return invoiceCompanyName;
    }

    public void setInvoiceCompanyName(String invoiceCompanyName) {
        this.invoiceCompanyName = invoiceCompanyName;
    }

    public String getTaxpayerId() {
        return taxpayerId;
    }

    public void setTaxpayerId(String taxpayerId) {
        this.taxpayerId = taxpayerId;
    }

    public String getInvoicePhone() {
        return invoicePhone;
    }

    public void setInvoicePhone(String invoicePhone) {
        this.invoicePhone = invoicePhone;
    }

    public String getInvoiceAddress() {
        return invoiceAddress;
    }

    public void setInvoiceAddress(String invoiceAddress) {
        this.invoiceAddress = invoiceAddress;
    }
}

