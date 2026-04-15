package com.boboboom.jxc.workflow.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowProcessUpsertRequest(
        @NotBlank(message = "流程ID不能为空")
        @Size(max = 64, message = "流程ID长度不能超过64")
        String processCode,
        @NotBlank(message = "业务名称不能为空")
        @Size(max = 128, message = "业务名称长度不能超过128")
        String businessName,
        @Size(max = 64, message = "模板ID长度不能超过64")
        String templateId
) {
}
