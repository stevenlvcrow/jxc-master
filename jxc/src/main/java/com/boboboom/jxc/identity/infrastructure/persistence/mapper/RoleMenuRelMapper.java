package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleMenuRelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RoleMenuRelMapper extends BaseMapper<RoleMenuRelDO> {

    List<RoleMenuRelDO> selectByRoleId(@Param("roleId") Long roleId);
}

