package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserAccountMapper extends BaseMapper<UserAccountDO> {

    UserAccountDO selectLoginUserByAccount(@Param("account") String account);

    List<UserRoleView> selectUserRoles(@Param("userId") Long userId);

    List<UserRoleView> selectUserRolesByUserIds(@Param("userIds") List<Long> userIds);
}

