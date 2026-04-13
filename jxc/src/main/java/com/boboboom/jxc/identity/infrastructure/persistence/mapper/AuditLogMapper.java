package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AuditLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.AuditLogView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper extends BaseMapper<AuditLogDO> {

    List<AuditLogView> selectAuditViews(@Param("operatorUserId") Long operatorUserId,
                                        @Param("actionType") String actionType,
                                        @Param("targetType") String targetType,
                                        @Param("limit") Integer limit);
}

