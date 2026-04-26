package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;

/** 身份与权限 MyBatis Mapper，承载数据库映射访问能力。 */
public interface UserAccountMapper extends BaseMapper<UserAccountDO> {

    UserAccountDO selectLoginUserByAccount(@Param("account") String account);

    List<UserAccountDO> selectUsersByCreatedGroupScopes(@Param("groupIds") List<Long> groupIds);

    List<UserAccountDO> selectUsersByGroupScope(@Param("groupId") Long groupId);

    List<UserAccountDO> selectRolelessUsersByCreatedScopes(@Param("groupIds") List<Long> groupIds,
                                                           @Param("storeIds") List<Long> storeIds);

    List<UserRoleView> selectUserRoles(@Param("userId") Long userId);

    List<UserRoleView> selectUserRolesByUserIds(@Param("userIds") List<Long> userIds);

    List<UserRoleView> selectUsersByRoleAndScope(@Param("roleCode") String roleCode,
                                                 @Param("scopeType") String scopeType,
                                                 @Param("scopeId") Long scopeId);
}
