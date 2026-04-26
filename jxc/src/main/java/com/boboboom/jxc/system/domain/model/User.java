package com.boboboom.jxc.system.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

import com.boboboom.jxc.common.BusinessException;

/** 系统用户领域模型，封装用户基础属性和状态变更规则。 */
public class User {

    private Long id;
    private String username;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 创建空用户对象，供持久化框架组装数据。 */
    public User() {
    }

    /** 创建业务记录。 */
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

    /** 处理停用。 */
    public void disable() {
        if (status == UserStatus.DISABLED) {
            throw new BusinessException("用户已禁用");
        }
        status = UserStatus.DISABLED;
    }

    /** 处理恢复持久化。 */
    public void restorePersistence(Long idValue, String usernameValue, String phoneValue, UserStatus statusValue,
                                   LocalDateTime createdAtValue, LocalDateTime updatedAtValue) {
        this.id = idValue;
        this.username = usernameValue;
        this.phone = phoneValue;
        this.status = statusValue;
        this.createdAt = createdAtValue;
        this.updatedAt = updatedAtValue;
    }

    /** 获取Id。 */
    public Long getId() {
        return id;
    }

    /** 获取Username。 */
    public String getUsername() {
        return username;
    }

    /** 获取Phone。 */
    public String getPhone() {
        return phone;
    }

    /** 获取Status。 */
    public UserStatus getStatus() {
        return status;
    }

    /** 获取CreatedAt。 */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** 获取UpdatedAt。 */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /** 处理相等判断。 */
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

    /** 处理哈希编码。 */
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
