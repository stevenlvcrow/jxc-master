package com.boboboom.jxc.inventory.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.boboboom.jxc.inventory.application.service.InventoryCheckHeader;
import com.boboboom.jxc.inventory.application.service.InventoryCheckLine;

/**
 * 盘点单持久化映射。
 */
public interface InventoryCheckMapper {

    List<InventoryCheckHeader> selectHeadersByScopeOrdered(@Param("headerTable") String headerTable,
                                                           @Param("scopeType") String scopeType,
                                                           @Param("scopeId") Long scopeId);

    List<InventoryCheckHeader> selectHeadersByScopeAndIds(@Param("headerTable") String headerTable,
                                                          @Param("scopeType") String scopeType,
                                                          @Param("scopeId") Long scopeId,
                                                          @Param("createdBy") Long createdBy,
                                                          @Param("viewAll") boolean viewAll,
                                                          @Param("ids") List<Long> ids);

    List<InventoryCheckHeader> selectHeaderByScopeAndId(@Param("headerTable") String headerTable,
                                                        @Param("scopeType") String scopeType,
                                                        @Param("scopeId") Long scopeId,
                                                        @Param("createdBy") Long createdBy,
                                                        @Param("viewAll") boolean viewAll,
                                                        @Param("id") Long id);

    Long countByScopeAndDocumentCode(@Param("headerTable") String headerTable,
                                     @Param("scopeType") String scopeType,
                                     @Param("scopeId") Long scopeId,
                                     @Param("documentCode") String documentCode);

    void insertHeader(@Param("headerTable") String headerTable, @Param("header") InventoryCheckHeader header);

    void updateHeader(@Param("headerTable") String headerTable, @Param("header") InventoryCheckHeader header);

    void deleteHeaderById(@Param("headerTable") String headerTable, @Param("id") Long id);

    List<InventoryCheckLine> selectLinesByHeaderIds(@Param("lineTable") String lineTable, @Param("headerIds") List<Long> headerIds);

    List<InventoryCheckLine> selectLinesByHeaderId(@Param("lineTable") String lineTable, @Param("headerId") Long headerId);

    void deleteLinesByHeaderId(@Param("lineTable") String lineTable, @Param("headerId") Long headerId);

    void insertLine(@Param("lineTable") String lineTable, @Param("line") InventoryCheckLine line);
}
