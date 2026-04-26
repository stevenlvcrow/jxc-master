package com.boboboom.jxc.purchase.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.purchase.application.service.PurchaseReportLineSnapshot;
import com.boboboom.jxc.purchase.domain.repository.PurchaseReportRepository;
import com.boboboom.jxc.purchase.infrastructure.persistence.mapper.PurchaseReportMapper;

/** 采购报表仓储实现。 */
@Repository
public class PurchaseReportRepositoryImpl implements PurchaseReportRepository {

    private final PurchaseReportMapper purchaseReportMapper;

    /** 创建采购报表仓储实现。 */
    public PurchaseReportRepositoryImpl(PurchaseReportMapper purchaseReportMapperValue) {
        this.purchaseReportMapper = purchaseReportMapperValue;
    }

    @Override
    public List<PurchaseReportLineSnapshot> findLines(String scopeType, Long scopeId, String documentType, Long createdBy, boolean viewAll) {
        return purchaseReportMapper.selectLines(scopeType, scopeId, documentType, createdBy, viewAll);
    }
}
