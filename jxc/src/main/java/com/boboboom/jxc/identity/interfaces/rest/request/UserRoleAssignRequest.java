package com.boboboom.jxc.identity.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

/** 身份与权限请求参数，承载接口入参。 */
public class UserRoleAssignRequest {

    @NotEmpty
    private List<UserRoleAssignment> assignments;

    /** 获取Assignments。 */
    public List<UserRoleAssignment> getAssignments() {
        return assignments;
    }

    /** 设置Assignments。 */
    public void setAssignments(List<UserRoleAssignment> assignmentsValue) {
        this.assignments = assignmentsValue;
    }

    /**
     * 用户角色授权项。
     */
    public static class UserRoleAssignment {

        private Long roleId;
        private String scopeType;
        private Long scopeId;

        /** 获取RoleId。 */
        public Long getRoleId() {
            return roleId;
        }

        /** 设置RoleId。 */
        public void setRoleId(Long roleIdValue) {
            this.roleId = roleIdValue;
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
    }
}
