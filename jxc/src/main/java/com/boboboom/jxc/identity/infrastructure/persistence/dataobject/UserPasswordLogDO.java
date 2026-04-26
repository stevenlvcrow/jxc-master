package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_user_password_log")
public class UserPasswordLogDO extends BaseCreateDO {

    private Long userId;
    private String operationType;
    private Long operatorUserId;
    private String passwordHash;
    private String remark;

    /** 获取UserId。 */
    public Long getUserId() {
        return userId;
    }

    /** 设置UserId。 */
    public void setUserId(Long userIdValue) {
        this.userId = userIdValue;
    }

    /** 获取OperationType。 */
    public String getOperationType() {
        return operationType;
    }

    /** 设置OperationType。 */
    public void setOperationType(String operationTypeValue) {
        this.operationType = operationTypeValue;
    }

    /** 获取OperatorUserId。 */
    public Long getOperatorUserId() {
        return operatorUserId;
    }

    /** 设置OperatorUserId。 */
    public void setOperatorUserId(Long operatorUserIdValue) {
        this.operatorUserId = operatorUserIdValue;
    }

    /** 获取PasswordHash。 */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** 设置PasswordHash。 */
    public void setPasswordHash(String passwordHashValue) {
        this.passwordHash = passwordHashValue;
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

