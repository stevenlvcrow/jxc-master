package com.boboboom.jxc.system.domain.model;

import com.boboboom.jxc.common.BusinessException;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    private Long id;
    private String username;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User() {
    }

    public static User create(String username, String phone) {
        if (username == null || username.isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (phone == null || phone.isBlank()) {
            throw new BusinessException("手机号不能为空");
        }
        User user = new User();
        user.username = username.trim();
        user.phone = phone.trim();
        user.status = UserStatus.ENABLED;
        return user;
    }

    public void disable() {
        if (status == UserStatus.DISABLED) {
            throw new BusinessException("用户已禁用");
        }
        status = UserStatus.DISABLED;
    }

    public void restorePersistence(Long id, String username, String phone, UserStatus status,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        }
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

