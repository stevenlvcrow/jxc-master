package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;

/** 身份与权限 MyBatis Mapper，承载数据库映射访问能力。 */
public interface GroupMapper extends BaseMapper<GroupDO> {

    List<GroupStoreSummary> selectGroupStoreSummary(@Param("status") String status);
}

