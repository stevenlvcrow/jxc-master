package com.boboboom.jxc.system.application.dto;

import java.time.LocalDateTime;

/** 系统用户数据模型，承载用户传输对象数据。 */
public record UserDTO(
        Long id,
        String username,
        String phone,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

