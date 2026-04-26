package com.boboboom.jxc.item.infrastructure.persistence.dataobject;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 物品与供应商数据对象，映射数据库表记录。 */
@TableName("supplier_qualification")
public class SupplierQualificationDO extends BaseAuditDO {

    private Long supplierId;
    private Integer sortNo;
    private String fileName;
    private String fileUrl;
    private String qualificationType;
    private LocalDate validTo;
    private String status;
    private String remark;

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

    /** 获取FileName。 */
    public String getFileName() {
        return fileName;
    }

    /** 设置FileName。 */
    public void setFileName(String fileNameValue) {
        this.fileName = fileNameValue;
    }

    /** 获取FileUrl。 */
    public String getFileUrl() {
        return fileUrl;
    }

    /** 设置FileUrl。 */
    public void setFileUrl(String fileUrlValue) {
        this.fileUrl = fileUrlValue;
    }

    /** 获取QualificationType。 */
    public String getQualificationType() {
        return qualificationType;
    }

    /** 设置QualificationType。 */
    public void setQualificationType(String qualificationTypeValue) {
        this.qualificationType = qualificationTypeValue;
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

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }
}

