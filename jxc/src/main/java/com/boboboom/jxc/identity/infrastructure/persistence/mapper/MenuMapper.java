package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;

/** 身份与权限 MyBatis Mapper，承载数据库映射访问能力。 */
public interface MenuMapper extends BaseMapper<MenuDO> {

    List<MenuPermissionView> selectMenusByUserContext(@Param("userId") Long userId,
                                                      @Param("scopeType") String scopeType,
                                                      @Param("scopeId") Long scopeId);
}

