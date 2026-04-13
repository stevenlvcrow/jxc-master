package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserRoleRelMapper extends BaseMapper<UserRoleRelDO> {

    List<UserRoleRelDO> selectActiveByUserId(@Param("userId") Long userId);
}

