package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.AuditLogDO;
import com.boboboom.jxc.identity.infrastructure.persistence.query.AuditLogView;

/** 身份与权限 MyBatis Mapper，承载数据库映射访问能力。 */
public interface AuditLogMapper extends BaseMapper<AuditLogDO> {

    List<AuditLogView> selectAuditViews(@Param("operatorUserId") Long operatorUserId,
                                        @Param("actionType") String actionType,
                                        @Param("targetType") String targetType,
                                        @Param("limit") Integer limit);
}

