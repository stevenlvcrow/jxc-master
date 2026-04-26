package com.boboboom.jxc.purchase.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Header;
import com.boboboom.jxc.purchase.application.service.PurchaseDocumentApplicationService.Line;

/**
 * 采购单据持久化映射。
 */
public interface PurchaseDocumentMapper {

    List<Header> selectHeaders(@Param("scopeType") String scopeType,
                               @Param("scopeId") Long scopeId,
                               @Param("documentType") String documentType,
                               @Param("createdBy") Long createdBy,
                               @Param("viewAll") boolean viewAll);

    List<Header> selectHeader(@Param("scopeType") String scopeType,
                              @Param("scopeId") Long scopeId,
                              @Param("documentType") String documentType,
                              @Param("id") Long id,
                              @Param("createdBy") Long createdBy,
                              @Param("viewAll") boolean viewAll);

    List<Header> selectHeadersByIds(@Param("scopeType") String scopeType,
                                    @Param("scopeId") Long scopeId,
                                    @Param("documentType") String documentType,
                                    @Param("ids") List<Long> ids,
                                    @Param("createdBy") Long createdBy,
                                    @Param("viewAll") boolean viewAll);

    Long countByCodePrefix(@Param("scopeType") String scopeType,
                           @Param("scopeId") Long scopeId,
                           @Param("documentType") String documentType,
                           @Param("prefix") String prefix);

    Long insertHeader(@Param("header") Header header);

    void updateHeader(@Param("header") Header header);

    void deleteHeaderById(@Param("id") Long id);

    void updateLastOperator(@Param("ids") List<Long> ids, @Param("operatorName") String operatorName);

    void deleteLinesByDocumentId(@Param("documentId") Long documentId);

    void insertLine(@Param("line") Line line);

    List<Line> selectLinesByDocumentIds(@Param("documentIds") List<Long> documentIds);

    List<Line> selectLinesByDocumentId(@Param("documentId") Long documentId);

    List<Line> selectLinesByIds(@Param("ids") List<Long> ids);

    void updateLineReview(@Param("lineId") Long lineId,
                          @Param("reviewStatus") String reviewStatus,
                          @Param("reviewQty") BigDecimal reviewQty,
                          @Param("remark") String remark);

    void updateLinePatch(@Param("lineId") Long lineId,
                         @Param("supplierName") String supplier,
                         @Param("expectedArrivalDate") LocalDate expectedArrivalDate,
                         @Param("reviewQty") BigDecimal reviewQty,
                         @Param("remark") String remark);
}
