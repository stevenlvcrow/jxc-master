package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("supplier_contract")
public class SupplierContractDO extends BaseAuditDO {

    private Long supplierId;
    private Integer sortNo;
    private String attachmentName;
    private String attachmentUrl;
    private String contractName;
    private String contractCode;
    private LocalDate validTo;
    private String status;

    /** 获取SupplierId。 */
    public Long getSupplierId() {
        return supplierId;
    }

    /** 设置SupplierId。 */
    public void setSupplierId(Long supplierIdValue) {
        this.supplierId = supplierIdValue;
    }

    /** 获取SortNo。 */
    public Integer getSortNo() {
        return sortNo;
    }

    /** 设置SortNo。 */
    public void setSortNo(Integer sortNoValue) {
        this.sortNo = sortNoValue;
    }

    /** 获取AttachmentName。 */
    public String getAttachmentName() {
        return attachmentName;
    }

    /** 设置AttachmentName。 */
    public void setAttachmentName(String attachmentNameValue) {
        this.attachmentName = attachmentNameValue;
    }

    /** 获取AttachmentUrl。 */
    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    /** 设置AttachmentUrl。 */
    public void setAttachmentUrl(String attachmentUrlValue) {
        this.attachmentUrl = attachmentUrlValue;
    }

    /** 获取ContractName。 */
    public String getContractName() {
        return contractName;
    }

    /** 设置ContractName。 */
    public void setContractName(String contractNameValue) {
        this.contractName = contractNameValue;
    }

    /** 获取ContractCode。 */
    public String getContractCode() {
        return contractCode;
    }

    /** 设置ContractCode。 */
    public void setContractCode(String contractCodeValue) {
        this.contractCode = contractCodeValue;
    }

    /** 获取ValidTo。 */
    public LocalDate getValidTo() {
        return validTo;
    }

    /** 设置ValidTo。 */
    public void setValidTo(LocalDate validToValue) {
        this.validTo = validToValue;
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

