package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boboboom.jxc.inventory.infrastructure.persistence.dataobject.InventoryPeriodOpeningLineDO;

/** 周期期初库存明细 MyBatis Mapper。 */
public interface InventoryPeriodOpeningLineMapper extends BaseMapper<InventoryPeriodOpeningLineDO> {

    List<InventoryPeriodOpeningLineDO> selectByHeaderId(@Param("headerId") Long headerId);

    List<InventoryPeriodOpeningLineDO> selectByHeaderIds(@Param("headerIds") List<Long> headerIds);

    void deleteByHeaderId(@Param("headerId") Long headerId);
}
