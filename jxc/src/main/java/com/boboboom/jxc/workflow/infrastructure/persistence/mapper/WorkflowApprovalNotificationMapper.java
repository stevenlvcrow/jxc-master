package com.boboboom.jxc.workflow.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.workflow.infrastructure.persistence.dataobject.WorkflowApprovalNotificationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkflowApprovalNotificationMapper extends BaseMapper<WorkflowApprovalNotificationDO> {
}
