package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

/** 身份与权限数据对象，映射数据库表记录。 */
public class BaseAuditDO extends BaseCreateDO {

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 设置UpdatedAt。 */
    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }
}

