package com.boboboom.jxc.identity.infrastructure.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseItemRuleWarehouseDO;

/** 身份与权限 MyBatis Mapper，承载数据库映射访问能力。 */
@Mapper
public interface WarehouseItemRuleWarehouseMapper extends BaseMapper<WarehouseItemRuleWarehouseDO> {
}
