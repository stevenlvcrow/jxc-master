package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public Boolean getBusinessControl() {
        return businessControl;
    }

    public void setBusinessControl(Boolean businessControl) {
        this.businessControl = businessControl;
    }

    public Boolean getControlOrder() {
        return controlOrder;
    }

    public void setControlOrder(Boolean controlOrder) {
        this.controlOrder = controlOrder;
    }

    public Boolean getControlPurchaseInbound() {
        return controlPurchaseInbound;
    }

    public void setControlPurchaseInbound(Boolean controlPurchaseInbound) {
        this.controlPurchaseInbound = controlPurchaseInbound;
    }

    public Boolean getControlTransferInbound() {
        return controlTransferInbound;
    }

    public void setControlTransferInbound(Boolean controlTransferInbound) {
        this.controlTransferInbound = controlTransferInbound;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
