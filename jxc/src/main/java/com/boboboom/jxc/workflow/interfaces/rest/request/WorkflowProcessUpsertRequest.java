package com.boboboom.jxc.workflow.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowProcessUpsertRequest(
        @NotBlank(message = "业务编码不能为空")
        @Size(max = 64, message = "业务编码长度不能超过64")
        String process_code,
        @NotBlank(message = "业务名称不能为空")
        @Size(max = 128, message = "业务名称长度不能超过128")
        String businessName,
        @Size(max = 64, message = "模板ID长度不能超过64")
        String templateId
) {
}
