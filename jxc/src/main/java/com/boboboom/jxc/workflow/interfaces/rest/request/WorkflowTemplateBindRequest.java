package com.boboboom.jxc.workflow.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WorkflowTemplateBindRequest(
        @NotBlank(message = "模板ID不能为空")
        @Size(max = 64, message = "模板ID长度不能超过64")
        String templateId
) {
}
