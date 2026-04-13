package com.boboboom.jxc.item.interfaces.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemTagCreateRequest(
        @NotBlank(message = "标签编码不能为空")
        @Size(max = 64, message = "标签编码长度不能超过64")
        String tagCode,

        @NotBlank(message = "标签名称不能为空")
        @Size(max = 128, message = "标签名称长度不能超过128")
        String tagName,

        @Size(max = 500, message = "物品长度不能超过500")
        String itemName
) {
}
