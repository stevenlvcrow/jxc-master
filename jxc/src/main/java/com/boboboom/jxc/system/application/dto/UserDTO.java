package com.boboboom.jxc.system.application.dto;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String username,
        String phone,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

