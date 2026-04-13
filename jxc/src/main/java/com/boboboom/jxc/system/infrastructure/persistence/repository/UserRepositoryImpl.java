package com.boboboom.jxc.system.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.system.domain.model.User;
import com.boboboom.jxc.system.domain.model.UserStatus;
import com.boboboom.jxc.system.domain.repository.UserRepository;
import com.boboboom.jxc.system.infrastructure.persistence.dataobject.UserDO;
import com.boboboom.jxc.system.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    public UserRepositoryImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

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

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<User> findAll() {
        return userMapper.selectList(new LambdaQueryWrapper<UserDO>()
                        .orderByDesc(UserDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

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

