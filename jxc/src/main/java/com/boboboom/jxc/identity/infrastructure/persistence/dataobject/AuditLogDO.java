package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_audit_log")
public class AuditLogDO extends BaseCreateDO {

    private Long operatorUserId;
    private String operatorPhone;
    private String actionType;
    private String targetType;
    private String targetId;
    private String scopeType;
    private Long scopeId;
    private String operationResult;
    private String detailJson;
    private String clientIp;

    /** 获取OperatorUserId。 */
    public Long getOperatorUserId() {
        return operatorUserId;
    }

    /** 设置OperatorUserId。 */
    public void setOperatorUserId(Long operatorUserIdValue) {
        this.operatorUserId = operatorUserIdValue;
    }

    /** 获取OperatorPhone。 */
    public String getOperatorPhone() {
        return operatorPhone;
    }

    /** 设置OperatorPhone。 */
    public void setOperatorPhone(String operatorPhoneValue) {
        this.operatorPhone = operatorPhoneValue;
    }

    /** 获取ActionType。 */
    public String getActionType() {
        return actionType;
    }

    /** 设置ActionType。 */
    public void setActionType(String actionTypeValue) {
        this.actionType = actionTypeValue;
    }

    /** 获取TargetType。 */
    public String getTargetType() {
        return targetType;
    }

    /** 设置TargetType。 */
    public void setTargetType(String targetTypeValue) {
        this.targetType = targetTypeValue;
    }

    /** 获取TargetId。 */
    public String getTargetId() {
        return targetId;
    }

    /** 设置TargetId。 */
    public void setTargetId(String targetIdValue) {
        this.targetId = targetIdValue;
    }

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

    /** 获取OperationResult。 */
    public String getOperationResult() {
        return operationResult;
    }

    /** 设置OperationResult。 */
    public void setOperationResult(String operationResultValue) {
        this.operationResult = operationResultValue;
    }

    /** 获取DetailJson。 */
    public String getDetailJson() {
        return detailJson;
    }

    /** 设置DetailJson。 */
    public void setDetailJson(String detailJsonValue) {
        this.detailJson = detailJsonValue;
    }

    /** 获取ClientIp。 */
    public String getClientIp() {
        return clientIp;
    }

    /** 设置ClientIp。 */
    public void setClientIp(String clientIpValue) {
        this.clientIp = clientIpValue;
    }
}

