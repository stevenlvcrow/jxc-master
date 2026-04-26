package com.boboboom.jxc.workflow.interfaces.rest.request;

import java.util.List;

import jakarta.validation.constraints.Size;

/** 审批流程请求参数，承载接口入参。 */
public record WorkflowProcessBatchStoreBindRequest(
        @Size(min = 1, max = 200, message = "批量绑定业务数量必须在1到200之间")
        List<Long> processIds,
        @Size(max = 500, message = "门店绑定数量不能超过500")
        List<Long> storeIds
) {
}
