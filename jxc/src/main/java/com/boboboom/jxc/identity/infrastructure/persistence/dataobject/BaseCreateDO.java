package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;

/** 身份与权限数据对象，映射数据库表记录。 */
public class BaseCreateDO extends BaseIdDO {

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }
}

