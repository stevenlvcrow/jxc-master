package com.boboboom.jxc.identity.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;

/** 身份与权限数据对象，映射数据库表记录。 */
@TableName("sys_group")
public class GroupDO extends BaseAuditDO {

    private String groupCode;
    private String groupName;
    private String status;
    private String remark;

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

    /** 获取Remark。 */
    public String getRemark() {
        return remark;
    }

    /** 设置Remark。 */
    public void setRemark(String remarkValue) {
        this.remark = remarkValue;
    }
}

