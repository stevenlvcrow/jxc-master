package com.boboboom.jxc.identity.infrastructure.persistence.query;

/** 身份与权限类型，负责集团门店汇总相关处理。 */
public class GroupStoreSummary {

    private Long id;
    private String groupCode;
    private String groupName;
    private String status;
    private Long storeCount;

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 设置Id。 */
    public void setId(Long idValue) {
        this.id = idValue;
    }

    /** 获取GroupCode。 */
    public String getGroupCode() {
        return groupCode;
    }

    /** 设置GroupCode。 */
    public void setGroupCode(String groupCodeValue) {
        this.groupCode = groupCodeValue;
    }

    /** 获取GroupName。 */
    public String getGroupName() {
        return groupName;
    }

    /** 设置GroupName。 */
    public void setGroupName(String groupNameValue) {
        this.groupName = groupNameValue;
    }

    /** 获取Status。 */
    public String getStatus() {
        return status;
    }

    /** 设置Status。 */
    public void setStatus(String statusValue) {
        this.status = statusValue;
    }

    /** 获取StoreCount。 */
    public Long getStoreCount() {
        return storeCount;
    }

    /** 设置StoreCount。 */
    public void setStoreCount(Long storeCountValue) {
        this.storeCount = storeCountValue;
    }
}

