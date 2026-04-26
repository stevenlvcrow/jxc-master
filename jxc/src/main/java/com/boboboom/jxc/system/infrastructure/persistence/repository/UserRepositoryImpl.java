package com.boboboom.jxc.system.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.system.domain.model.User;
import com.boboboom.jxc.system.domain.model.UserStatus;
import com.boboboom.jxc.system.domain.repository.UserRepository;
import com.boboboom.jxc.system.infrastructure.persistence.dataobject.UserDO;
import com.boboboom.jxc.system.infrastructure.persistence.mapper.UserMapper;

/** 系统用户仓储实现，负责通过持久层组件完成数据读写。 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    /** 系统用户仓储实现，负责通过持久层组件完成数据读写。 */
    public UserRepositoryImpl(UserMapper userMapperValue) {
        this.userMapper = userMapperValue;
    }

    /** 保存业务数据。 */
    @Override
    public User save(User user) {
        UserDO userDO = toDataObject(user);
        if (user.getId() == null) {
            userMapper.insert(userDO);
        } else {
            userMapper.updateById(userDO);
        }
        return toDomain(userMapper.selectById(userDO.getId()));
    }

    /** 按主键查询记录。 */
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    /** 查询全部记录。 */
    @Override
    public List<User> findAll() {
        return userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                        .orderByDesc(UserDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    /** 判断手机号是否已存在。 */
    @Override
    public boolean existsByPhone(String phone) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getPhone, phone));
        return count != null && count > 0;
    }

    private UserDO toDataObject(User user) {
        UserDO userDO = new UserDO();
        userDO.setId(user.getId());
        userDO.setUsername(user.getUsername());
        userDO.setPhone(user.getPhone());
        userDO.setStatus(user.getStatus().name());
        userDO.setCreatedAt(user.getCreatedAt());
        userDO.setUpdatedAt(user.getUpdatedAt());
        return userDO;
    }

    private User toDomain(UserDO userDO) {
        User user = new User();
        user.restorePersistence(
                userDO.getId(),
                userDO.getUsername(),
                userDO.getPhone(),
                UserStatus.valueOf(userDO.getStatus()),
                userDO.getCreatedAt(),
                userDO.getUpdatedAt()
        );
        return user;
    }
}

