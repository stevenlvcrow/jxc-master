package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.GroupStoreSummary;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupMapper extends BaseMapper<GroupDO> {

    List<GroupStoreSummary> selectGroupStoreSummary(@Param("status") String status);
}

