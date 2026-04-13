package com.boboboom.jxc.system.application.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserCommand(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 32, message = "用户名长度不能超过32")
        String username,

        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
        String phone
) {
}

