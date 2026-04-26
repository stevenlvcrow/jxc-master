package com.boboboom.jxc.purchase.domain.repository;

import java.util.List;

import com.boboboom.jxc.purchase.application.service.PurchaseReportLineSnapshot;

/** 采购报表仓储。 */
public interface PurchaseReportRepository {

    /** 查询采购报表单据行快照。 */
    List<PurchaseReportLineSnapshot> findLines(String scopeType, Long scopeId, String documentType, Long createdBy, boolean viewAll);
}
