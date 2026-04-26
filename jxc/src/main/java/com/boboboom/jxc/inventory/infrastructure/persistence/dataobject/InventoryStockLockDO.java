package com.boboboom.jxc.inventory.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.BaseAuditDO;

/** 库存锁库数据对象，记录盘点期间被锁定的仓库物品。 */
@TableName("inventory_stock_lock")
public class InventoryStockLockDO extends BaseAuditDO {

    private String scopeType;
    private Long scopeId;
    private String warehouseName;
    private String itemCode;
    private String itemName;
    private String sourceType;
    private Long sourceId;
    private String sourceCode;
    private String lockReason;
    private Boolean active;
    private Long lockedBy;
    private LocalDateTime lockedAt;
    private Long releasedBy;
    private LocalDateTime releasedAt;
    private String remark;

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

    /** 获取WarehouseName。 */
    public String getWarehouseName() {
        return warehouseName;
    }

    /** 设置WarehouseName。 */
    public void setWarehouseName(String warehouseNameValue) {
        this.warehouseName = warehouseNameValue;
    }

    /** 获取ItemCode。 */
    public String getItemCode() {
        return itemCode;
    }

    /** 设置ItemCode。 */
    public void setItemCode(String itemCodeValue) {
        this.itemCode = itemCodeValue;
    }

    /** 获取ItemName。 */
    public String getItemName() {
        return itemName;
    }

    /** 设置ItemName。 */
    public void setItemName(String itemNameValue) {
        this.itemName = itemNameValue;
    }

    /** 获取SourceType。 */
    public String getSourceType() {
        return sourceType;
    }

    /** 设置SourceType。 */
    public void setSourceType(String sourceTypeValue) {
        this.sourceType = sourceTypeValue;
    }

    /** 获取SourceId。 */
    public Long getSourceId() {
        return sourceId;
    }

    /** 设置SourceId。 */
    public void setSourceId(Long sourceIdValue) {
        this.sourceId = sourceIdValue;
    }

    /** 获取SourceCode。 */
    public String getSourceCode() {
        return sourceCode;
    }

    /** 设置SourceCode。 */
    public void setSourceCode(String sourceCodeValue) {
        this.sourceCode = sourceCodeValue;
    }

    /** 获取LockReason。 */
    public String getLockReason() {
        return lockReason;
    }

    /** 设置LockReason。 */
    public void setLockReason(String lockReasonValue) {
        this.lockReason = lockReasonValue;
    }

    /** 获取Active。 */
    public Boolean getActive() {
        return active;
    }

    /** 设置Active。 */
    public void setActive(Boolean activeValue) {
        this.active = activeValue;
    }

    /** 获取LockedBy。 */
    public Long getLockedBy() {
        return lockedBy;
    }

    /** 设置LockedBy。 */
    public void setLockedBy(Long lockedByValue) {
        this.lockedBy = lockedByValue;
    }

    /** 获取LockedAt。 */
    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    /** 设置LockedAt。 */
    public void setLockedAt(LocalDateTime lockedAtValue) {
        this.lockedAt = lockedAtValue;
    }

    /** 获取ReleasedBy。 */
    public Long getReleasedBy() {
        return releasedBy;
    }

    /** 设置ReleasedBy。 */
    public void setReleasedBy(Long releasedByValue) {
        this.releasedBy = releasedByValue;
    }

    /** 获取ReleasedAt。 */
    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    /** 设置ReleasedAt。 */
    public void setReleasedAt(LocalDateTime releasedAtValue) {
        this.releasedAt = releasedAtValue;
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
