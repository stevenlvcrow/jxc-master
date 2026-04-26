package com.boboboom.jxc.workflow.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;

/** 审批流程 MyBatis Mapper，承载数据库映射访问能力。 */
@Mapper
public interface WorkflowApprovalNotificationMapper extends BaseMapper<WorkflowApprovalNotificationDO> {
}
