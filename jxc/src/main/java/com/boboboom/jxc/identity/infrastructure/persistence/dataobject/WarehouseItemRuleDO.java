package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("warehouse_item_rule")
public class WarehouseItemRuleDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long groupId;

    private String ruleCode;

    private String ruleName;

    private Boolean businessControl;

    private Boolean controlOrder;

    private Boolean controlPurchaseInbound;

    private Boolean controlTransferInbound;

    @TableLogic
    private String status;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取GroupId。 */
    public Long getGroupId() {
        return groupId;
    }

    /** 设置GroupId。 */
    public void setGroupId(Long groupIdValue) {
        this.groupId = groupIdValue;
    }

    /** 获取RuleCode。 */
    public String getRuleCode() {
        return ruleCode;
    }

    /** 设置RuleCode。 */
    public void setRuleCode(String ruleCodeValue) {
        this.ruleCode = ruleCodeValue;
    }

    /** 获取RuleName。 */
    public String getRuleName() {
        return ruleName;
    }

    /** 设置RuleName。 */
    public void setRuleName(String ruleNameValue) {
        this.ruleName = ruleNameValue;
    }

    /** 获取BusinessControl。 */
    public Boolean getBusinessControl() {
        return businessControl;
    }

    /** 设置BusinessControl。 */
    public void setBusinessControl(Boolean businessControlValue) {
        this.businessControl = businessControlValue;
    }

    /** 获取ControlOrder。 */
    public Boolean getControlOrder() {
        return controlOrder;
    }

    /** 设置ControlOrder。 */
    public void setControlOrder(Boolean controlOrderValue) {
        this.controlOrder = controlOrderValue;
    }

    /** 获取ControlPurchaseInbound。 */
    public Boolean getControlPurchaseInbound() {
        return controlPurchaseInbound;
    }

    /** 设置ControlPurchaseInbound。 */
    public void setControlPurchaseInbound(Boolean controlPurchaseInboundValue) {
        this.controlPurchaseInbound = controlPurchaseInboundValue;
    }

    /** 获取ControlTransferInbound。 */
    public Boolean getControlTransferInbound() {
        return controlTransferInbound;
    }

    /** 设置ControlTransferInbound。 */
    public void setControlTransferInbound(Boolean controlTransferInboundValue) {
        this.controlTransferInbound = controlTransferInboundValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取CreatedBy。 */
    public String getCreatedBy() {
        return createdBy;
    }

    /** 设置CreatedBy。 */
    public void setCreatedBy(String createdByValue) {
        this.createdBy = createdByValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }

    /** 获取UpdatedBy。 */
    public String getUpdatedBy() {
        return updatedBy;
    }

    /** 设置UpdatedBy。 */
    public void setUpdatedBy(String updatedByValue) {
        this.updatedBy = updatedByValue;
    }

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置UpdatedAt。 */
    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }
}
