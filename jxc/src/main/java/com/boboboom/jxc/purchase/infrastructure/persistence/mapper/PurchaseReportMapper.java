package com.boboboom.jxc.purchase.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.boboboom.jxc.purchase.application.service.PurchaseReportLineSnapshot;

/** 采购报表持久化映射。 */
public interface PurchaseReportMapper {

    /** 查询采购报表单据行快照。 */
    List<PurchaseReportLineSnapshot> selectLines(@Param("scopeType") String scopeType,
                                                 @Param("scopeId") Long scopeId,
                                                 @Param("documentType") String documentType,
                                                 @Param("createdBy") Long createdBy,
                                                 @Param("viewAll") boolean viewAll);
}
