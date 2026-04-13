package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.MenuDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.MenuPermissionView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MenuMapper extends BaseMapper<MenuDO> {

    List<MenuPermissionView> selectMenusByUserContext(@Param("userId") Long userId,
                                                      @Param("scopeType") String scopeType,
                                                      @Param("scopeId") Long scopeId);
}

