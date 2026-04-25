package com.boboboom.jxc.purchase.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Header;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Line;

/**
 * 采购单据仓储。
 */
public interface PurchaseDocumentRepository {

    List<Header> findHeaders(String scopeType, Long scopeId, String documentType, Long createdBy, boolean viewAll);

    Optional<Header> findHeader(String scopeType, Long scopeId, String documentType, Long id, Long createdBy, boolean viewAll);

    List<Header> findHeadersByIds(String scopeType,
                                  Long scopeId,
                                  String documentType,
                                  List<Long> ids,
                                  Long createdBy,
                                  boolean viewAll);

    Long countByCodePrefix(String scopeType, Long scopeId, String documentType, String prefix);

    Long saveHeader(Header header);

    void updateHeader(Header header);

    void deleteHeaderById(Long id);

    void updateLastOperator(List<Long> ids, String operatorName);

    void deleteLinesByDocumentId(Long documentId);

    void saveLine(Line line);

    List<Line> findLinesByDocumentIds(List<Long> documentIds);

    List<Line> findLinesByDocumentId(Long documentId);

    List<Line> findLinesByIds(List<Long> ids);

    void updateLineReview(Long lineId, String reviewStatus, java.math.BigDecimal reviewQty, String remark);

    void updateLinePatch(Long lineId, String supplier, java.time.LocalDate expectedArrivalDate, java.math.BigDecimal reviewQty, String remark);
}
