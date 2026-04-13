package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.LoginLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LoginLogMapper extends BaseMapper<LoginLogDO> {

    List<LoginLogDO> selectRecentFailedByPhone(@Param("phone") String phone,
                                               @Param("limit") Integer limit);
}

