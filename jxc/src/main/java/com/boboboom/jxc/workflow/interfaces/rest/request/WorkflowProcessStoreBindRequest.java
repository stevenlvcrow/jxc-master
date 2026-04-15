package com.boboboom.jxc.workflow.interfaces.rest.request;

import jakarta.validation.constraints.Size;

import java.util.List;

public record WorkflowProcessStoreBindRequest(
        @Size(max = 500, message = "门店绑定数量不能超过500")
        List<Long> storeIds
) {
}

