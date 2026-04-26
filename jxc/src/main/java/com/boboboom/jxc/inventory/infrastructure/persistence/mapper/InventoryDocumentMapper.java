package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentLine;

/**
 * 通用库存单据持久化映射。
 */
public interface InventoryDocumentMapper {

    List<InventoryDocumentHeader> selectHeadersByScopeOrdered(@Param("headerTable") String headerTable,
                                                              @Param("purchaseInbound") boolean purchaseInbound,
                                                              @Param("scopeType") String scopeType,
                                                              @Param("scopeId") Long scopeId);

    List<InventoryDocumentHeader> selectHeadersByScopeAndIds(@Param("headerTable") String headerTable,
                                                             @Param("purchaseInbound") boolean purchaseInbound,
                                                             @Param("scopeType") String scopeType,
                                                             @Param("scopeId") Long scopeId,
                                                             @Param("createdBy") Long createdBy,
                                                             @Param("viewAll") boolean viewAll,
                                                             @Param("ids") List<Long> ids);

    List<InventoryDocumentHeader> selectHeaderByScopeAndId(@Param("headerTable") String headerTable,
                                                           @Param("purchaseInbound") boolean purchaseInbound,
                                                           @Param("scopeType") String scopeType,
                                                           @Param("scopeId") Long scopeId,
                                                           @Param("createdBy") Long createdBy,
                                                           @Param("viewAll") boolean viewAll,
                                                           @Param("id") Long id);

    Long countByScopeAndDocumentCode(@Param("headerTable") String headerTable,
                                     @Param("scopeType") String scopeType,
                                     @Param("scopeId") Long scopeId,
                                     @Param("documentCode") String documentCode);

    void insertHeader(@Param("headerTable") String headerTable, @Param("header") InventoryDocumentHeader header);

    void updateHeader(@Param("headerTable") String headerTable, @Param("header") InventoryDocumentHeader header);

    void deleteHeaderById(@Param("headerTable") String headerTable, @Param("id") Long id);

    List<InventoryDocumentLine> selectLinesByHeaderIds(@Param("lineTable") String lineTable,
                                                       @Param("headerIdColumn") String headerIdColumn,
                                                       @Param("purchaseInbound") boolean purchaseInbound,
                                                       @Param("headerIds") List<Long> headerIds);

    List<InventoryDocumentLine> selectLinesByHeaderId(@Param("lineTable") String lineTable,
                                                      @Param("headerIdColumn") String headerIdColumn,
                                                      @Param("purchaseInbound") boolean purchaseInbound,
                                                      @Param("headerId") Long headerId);

    void deleteLinesByHeaderId(@Param("lineTable") String lineTable, @Param("headerId") Long headerId);

    void insertLine(@Param("lineTable") String lineTable, @Param("line") InventoryDocumentLine line);
}
