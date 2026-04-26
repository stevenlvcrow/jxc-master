package com.boboboom.jxc.identity.infrastructure.persistence.query;

import java.time.LocalDateTime;

/** 身份与权限视图模型，承载页面展示数据。 */
public class AuditLogView {

    private Long id;
    private Long operatorUserId;
    private String operatorPhone;
    private String operatorRealName;
    private String actionType;
    private String targetType;
    private String targetId;
    private String scopeType;
    private Long scopeId;
    private String operationResult;
    private String clientIp;
    private LocalDateTime createdAt;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

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

    /** 获取OperatorRealName。 */
    public String getOperatorRealName() {
        return operatorRealName;
    }

    /** 设置OperatorRealName。 */
    public void setOperatorRealName(String operatorRealNameValue) {
        this.operatorRealName = operatorRealNameValue;
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

    /** 获取ClientIp。 */
    public String getClientIp() {
        return clientIp;
    }

    /** 设置ClientIp。 */
    public void setClientIp(String clientIpValue) {
        this.clientIp = clientIpValue;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 设置CreatedAt。 */
    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }
}

