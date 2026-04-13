package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMapper extends BaseMapper<RoleDO> {

    List<UserRoleView> selectRolesByUserAndScope(@Param("userId") Long userId,
                                                 @Param("scopeType") String scopeType,
                                                 @Param("scopeId") Long scopeId);
}

