package com.boboboom.jxc.purchase.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Header;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Line;
import com.boboboom.jxc.purchase.domain.repository.PurchaseDocumentRepository;
import com.boboboom.jxc.purchase.infrastructure.persistence.mapper.PurchaseDocumentMapper;

/**
 * 采购单据仓储实现。
 */
@Repository
public class PurchaseDocumentRepositoryImpl implements PurchaseDocumentRepository {

    private final PurchaseDocumentMapper purchaseDocumentMapper;

    /**
     * 创建采购单据仓储实现。
     *
     * @param purchaseDocumentMapperValue 采购单据持久化映射
     */
    public PurchaseDocumentRepositoryImpl(PurchaseDocumentMapper purchaseDocumentMapperValue) {
        this.purchaseDocumentMapper = purchaseDocumentMapperValue;
    }

    @Override
    public List<Header> findHeaders(String scopeType, Long scopeId, String documentType, Long createdBy, boolean viewAll) {
        return purchaseDocumentMapper.selectHeaders(scopeType, scopeId, documentType, createdBy, viewAll);
    }

    @Override
    public Optional<Header> findHeader(String scopeType, Long scopeId, String documentType, Long id, Long createdBy, boolean viewAll) {
        return purchaseDocumentMapper.selectHeader(scopeType, scopeId, documentType, id, createdBy, viewAll)
                .stream()
                .findFirst();
    }

    @Override
    public List<Header> findHeadersByIds(String scopeType,
                                         Long scopeId,
                                         String documentType,
                                         List<Long> ids,
                                         Long createdBy,
                                         boolean viewAll) {
        return purchaseDocumentMapper.selectHeadersByIds(scopeType, scopeId, documentType, ids, createdBy, viewAll);
    }

    @Override
    public Long countByCodePrefix(String scopeType, Long scopeId, String documentType, String prefix) {
        return purchaseDocumentMapper.countByCodePrefix(scopeType, scopeId, documentType, prefix);
    }

    @Override
    public Long saveHeader(Header header) {
        return purchaseDocumentMapper.insertHeader(header);
    }

    @Override
    public void updateHeader(Header header) {
        purchaseDocumentMapper.updateHeader(header);
    }

    @Override
    public void deleteHeaderById(Long id) {
        purchaseDocumentMapper.deleteHeaderById(id);
    }

    @Override
    public void updateLastOperator(List<Long> ids, String operatorName) {
        purchaseDocumentMapper.updateLastOperator(ids, operatorName);
    }

    @Override
    public void deleteLinesByDocumentId(Long documentId) {
        purchaseDocumentMapper.deleteLinesByDocumentId(documentId);
    }

    @Override
    public void saveLine(Line line) {
        purchaseDocumentMapper.insertLine(line);
    }

    @Override
    public List<Line> findLinesByDocumentIds(List<Long> documentIds) {
        return purchaseDocumentMapper.selectLinesByDocumentIds(documentIds);
    }

    @Override
    public List<Line> findLinesByDocumentId(Long documentId) {
        return purchaseDocumentMapper.selectLinesByDocumentId(documentId);
    }

    @Override
    public List<Line> findLinesByIds(List<Long> ids) {
        return purchaseDocumentMapper.selectLinesByIds(ids);
    }

    @Override
    public void updateLineReview(Long lineId, String reviewStatus, BigDecimal reviewQty, String remark) {
        purchaseDocumentMapper.updateLineReview(lineId, reviewStatus, reviewQty, remark);
    }

    @Override
    public void updateLinePatch(Long lineId, String supplier, LocalDate expectedArrivalDate, BigDecimal reviewQty, String remark) {
        purchaseDocumentMapper.updateLinePatch(lineId, supplier, expectedArrivalDate, reviewQty, remark);
    }
}
