package com.boboboom.jxc.identity.interfaces.rest.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserRoleAssignRequest {

    @NotEmpty
    private List<UserRoleAssignment> assignments;

    public List<UserRoleAssignment> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<UserRoleAssignment> assignments) {
        this.assignments = assignments;
    }

    public static class UserRoleAssignment {

        private Long roleId;
        private String scopeType;
        private Long scopeId;

        public Long getRoleId() {
            return roleId;
        }

        public void setRoleId(Long roleId) {
            this.roleId = roleId;
        }

        public String getScopeType() {
            return scopeType;
        }

        public void setScopeType(String scopeType) {
            this.scopeType = scopeType;
        }

        public Long getScopeId() {
            return scopeId;
        }

        public void setScopeId(Long scopeId) {
            this.scopeId = scopeId;
        }
    }
}
