package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserPasswordLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserPasswordLogMapper extends BaseMapper<UserPasswordLogDO> {

    List<UserPasswordLogDO> selectRecentByUserId(@Param("userId") Long userId,
                                                 @Param("limit") Integer limit);
}

