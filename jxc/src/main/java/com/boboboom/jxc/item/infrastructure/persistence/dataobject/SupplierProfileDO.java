package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("supplier_profile")
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

    /** 获取ScopeType。 */
    public String getScopeType() {
        return scopeType;
    }

    /** 设置ScopeType。 */
    public void setScopeType(String scopeTypeValue) {
        this.scopeType = scopeTypeValue;
    }

    /** 获取ScopeId。 */
    public Long getScopeId() {
        return scopeId;
    }

    /** 设置ScopeId。 */
    public void setScopeId(Long scopeIdValue) {
        this.scopeId = scopeIdValue;
    }

    /** 获取SupplierCode。 */
    public String getSupplierCode() {
        return supplierCode;
    }

    /** 设置SupplierCode。 */
    public void setSupplierCode(String supplierCodeValue) {
        this.supplierCode = supplierCodeValue;
    }

    /** 获取SupplierName。 */
    public String getSupplierName() {
        return supplierName;
    }

    /** 设置SupplierName。 */
    public void setSupplierName(String supplierNameValue) {
        this.supplierName = supplierNameValue;
    }

    /** 获取SupplierShortName。 */
    public String getSupplierShortName() {
        return supplierShortName;
    }

    /** 设置SupplierShortName。 */
    public void setSupplierShortName(String supplierShortNameValue) {
        this.supplierShortName = supplierShortNameValue;
    }

    /** 获取SupplierMnemonic。 */
    public String getSupplierMnemonic() {
        return supplierMnemonic;
    }

    /** 设置SupplierMnemonic。 */
    public void setSupplierMnemonic(String supplierMnemonicValue) {
        this.supplierMnemonic = supplierMnemonicValue;
    }

    /** 获取SupplierCategory。 */
    public String getSupplierCategory() {
        return supplierCategory;
    }

    /** 设置SupplierCategory。 */
    public void setSupplierCategory(String supplierCategoryValue) {
        this.supplierCategory = supplierCategoryValue;
    }

    /** 获取TaxRate。 */
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /** 设置TaxRate。 */
    public void setTaxRate(BigDecimal taxRateValue) {
        this.taxRate = taxRateValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取ContactPerson。 */
    public String getContactPerson() {
        return contactPerson;
    }

    /** 设置ContactPerson。 */
    public void setContactPerson(String contactPersonValue) {
        this.contactPerson = contactPersonValue;
    }

    /** 获取ContactPhone。 */
    public String getContactPhone() {
        return contactPhone;
    }

    /** 设置ContactPhone。 */
    public void setContactPhone(String contactPhoneValue) {
        this.contactPhone = contactPhoneValue;
    }

    /** 获取Email。 */
    public String getEmail() {
        return email;
    }

    /** 设置Email。 */
    public void setEmail(String emailValue) {
        this.email = emailValue;
    }

    /** 获取ContactAddress。 */
    public String getContactAddress() {
        return contactAddress;
    }

    /** 设置ContactAddress。 */
    public void setContactAddress(String contactAddressValue) {
        this.contactAddress = contactAddressValue;
    }

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }

    /** 获取SettlementMethod。 */
    public String getSettlementMethod() {
        return settlementMethod;
    }

    /** 设置SettlementMethod。 */
    public void setSettlementMethod(String settlementMethodValue) {
        this.settlementMethod = settlementMethodValue;
    }

    /** 获取OrderSummaryRule。 */
    public String getOrderSummaryRule() {
        return orderSummaryRule;
    }

    /** 设置OrderSummaryRule。 */
    public void setOrderSummaryRule(String orderSummaryRuleValue) {
        this.orderSummaryRule = orderSummaryRuleValue;
    }

    /** 获取InputBatchWhenDelivery。 */
    public Boolean getInputBatchWhenDelivery() {
        return inputBatchWhenDelivery;
    }

    /** 设置InputBatchWhenDelivery。 */
    public void setInputBatchWhenDelivery(Boolean inputBatchWhenDeliveryValue) {
        this.inputBatchWhenDelivery = inputBatchWhenDeliveryValue;
    }

    /** 获取SyncReceiptData。 */
    public Boolean getSyncReceiptData() {
        return syncReceiptData;
    }

    /** 设置SyncReceiptData。 */
    public void setSyncReceiptData(Boolean syncReceiptDataValue) {
        this.syncReceiptData = syncReceiptDataValue;
    }

    /** 获取PurchaseReceiptDependShipping。 */
    public String getPurchaseReceiptDependShipping() {
        return purchaseReceiptDependShipping;
    }

    /** 设置PurchaseReceiptDependShipping。 */
    public void setPurchaseReceiptDependShipping(String purchaseReceiptDependShippingValue) {
        this.purchaseReceiptDependShipping = purchaseReceiptDependShippingValue;
    }

    /** 获取DeliveryDependShipping。 */
    public String getDeliveryDependShipping() {
        return deliveryDependShipping;
    }

    /** 设置DeliveryDependShipping。 */
    public void setDeliveryDependShipping(String deliveryDependShippingValue) {
        this.deliveryDependShipping = deliveryDependShippingValue;
    }

    /** 获取SupplierManageInventory。 */
    public Boolean getSupplierManageInventory() {
        return supplierManageInventory;
    }

    /** 设置SupplierManageInventory。 */
    public void setSupplierManageInventory(Boolean supplierManageInventoryValue) {
        this.supplierManageInventory = supplierManageInventoryValue;
    }

    /** 获取ControlOrderTime。 */
    public Boolean getControlOrderTime() {
        return controlOrderTime;
    }

    /** 设置ControlOrderTime。 */
    public void setControlOrderTime(Boolean controlOrderTimeValue) {
        this.controlOrderTime = controlOrderTimeValue;
    }

    /** 获取AllowCloseOrder。 */
    public Boolean getAllowCloseOrder() {
        return allowCloseOrder;
    }

    /** 设置AllowCloseOrder。 */
    public void setAllowCloseOrder(Boolean allowCloseOrderValue) {
        this.allowCloseOrder = allowCloseOrderValue;
    }

    /** 获取ReconciliationMode。 */
    public String getReconciliationMode() {
        return reconciliationMode;
    }

    /** 设置ReconciliationMode。 */
    public void setReconciliationMode(String reconciliationModeValue) {
        this.reconciliationMode = reconciliationModeValue;
    }

    /** 获取ScopeControl。 */
    public String getScopeControl() {
        return scopeControl;
    }

    /** 设置ScopeControl。 */
    public void setScopeControl(String scopeControlValue) {
        this.scopeControl = scopeControlValue;
    }

    /** 获取Source。 */
    public String getSource() {
        return source;
    }

    /** 设置Source。 */
    public void setSource(String sourceValue) {
        this.source = sourceValue;
    }

    /** 获取SupplyRelation。 */
    public String getSupplyRelation() {
        return supplyRelation;
    }

    /** 设置SupplyRelation。 */
    public void setSupplyRelation(String supplyRelationValue) {
        this.supplyRelation = supplyRelationValue;
    }

    /** 获取BindStatus。 */
    public String getBindStatus() {
        return bindStatus;
    }

    /** 设置BindStatus。 */
    public void setBindStatus(String bindStatusValue) {
        this.bindStatus = bindStatusValue;
    }

    /** 获取InvoiceCompanyName。 */
    public String getInvoiceCompanyName() {
        return invoiceCompanyName;
    }

    /** 设置InvoiceCompanyName。 */
    public void setInvoiceCompanyName(String invoiceCompanyNameValue) {
        this.invoiceCompanyName = invoiceCompanyNameValue;
    }

    /** 获取TaxpayerId。 */
    public String getTaxpayerId() {
        return taxpayerId;
    }

    /** 设置TaxpayerId。 */
    public void setTaxpayerId(String taxpayerIdValue) {
        this.taxpayerId = taxpayerIdValue;
    }

    /** 获取InvoicePhone。 */
    public String getInvoicePhone() {
        return invoicePhone;
    }

    /** 设置InvoicePhone。 */
    public void setInvoicePhone(String invoicePhoneValue) {
        this.invoicePhone = invoicePhoneValue;
    }

    /** 获取InvoiceAddress。 */
    public String getInvoiceAddress() {
        return invoiceAddress;
    }

    /** 设置InvoiceAddress。 */
    public void setInvoiceAddress(String invoiceAddressValue) {
        this.invoiceAddress = invoiceAddressValue;
    }
}

