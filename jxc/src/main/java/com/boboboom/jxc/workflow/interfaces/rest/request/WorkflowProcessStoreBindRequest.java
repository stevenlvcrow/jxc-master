package com.boboboom.jxc.workflow.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.Size;

/** 审批流程请求参数，承载接口入参。 */
public record WorkflowProcessStoreBindRequest(
        @Size(max = 500, message = "门店绑定数量不能超过500")
        List<Long> storeIds
) {
}

