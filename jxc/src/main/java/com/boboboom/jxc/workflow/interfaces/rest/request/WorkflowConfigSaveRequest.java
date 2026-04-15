package com.boboboom.jxc.workflow.interfaces.rest.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkflowConfigSaveRequest(
        @NotBlank(message = "业务编码不能为空")
        @Size(max = 64, message = "业务编码长度不能超过64")
        String businessCode,
        @NotBlank(message = "流程编码不能为空")
        @Size(max = 64, message = "流程编码长度不能超过64")
        String workflowCode,
        @NotBlank(message = "流程名称不能为空")
        @Size(max = 128, message = "流程名称长度不能超过128")
        String workflowName,
        @NotEmpty(message = "流程节点不能为空")
        @Size(max = 20, message = "流程节点最多支持20个")
        List<@Valid NodeItem> nodes
) {
    public record NodeItem(
            String nodeKey,
            @NotBlank(message = "节点名称不能为空")
            @Size(max = 64, message = "节点名称长度不能超过64")
            String nodeName,
            Integer x,
            Integer y,
            @Size(max = 64, message = "审批角色编码长度不能超过64")
            String approverRoleCode,
            @Size(max = 8, message = "会签方式长度不能超过8")
            String roleSignMode,
            Long approverUserId,
            Boolean allowReject,
            Boolean allowUnapprove,
            @Size(max = 32, message = "节点类型长度不能超过32")
            String nodeType,
            @Size(max = 256, message = "条件表达式长度不能超过256")
            String conditionExpression
    ) {
    }
}
