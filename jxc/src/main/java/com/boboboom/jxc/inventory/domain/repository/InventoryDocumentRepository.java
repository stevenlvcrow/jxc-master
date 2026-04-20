package com.boboboom.jxc.inventory.domain.repository;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentLine;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;

import java.util.List;
import java.util.Optional;

/**
 * 通用库存单据仓储。
 */
public interface InventoryDocumentRepository {

    List<InventoryDocumentHeader> findHeadersByScopeOrdered(InventoryDocumentType type, String scopeType, Long scopeId);

    List<InventoryDocumentHeader> findHeadersByScopeAndIds(InventoryDocumentType type,
                                                           String scopeType,
                                                           Long scopeId,
                                                           Long createdBy,
                                                           boolean viewAll,
                                                           List<Long> ids);

    Optional<InventoryDocumentHeader> findHeaderByScopeAndId(InventoryDocumentType type,
                                                             String scopeType,
                                                             Long scopeId,
                                                             Long createdBy,
                                                             boolean viewAll,
                                                             Long id);

    Long countByScopeAndDocumentCode(InventoryDocumentType type, String scopeType, Long scopeId, String documentCode);

    void saveHeader(InventoryDocumentType type, InventoryDocumentHeader header);

    void updateHeader(InventoryDocumentType type, InventoryDocumentHeader header);

    void deleteHeaderById(InventoryDocumentType type, Long id);

    List<InventoryDocumentLine> findLinesByHeaderIds(InventoryDocumentType type, List<Long> headerIds);

    List<InventoryDocumentLine> findLinesByHeaderId(InventoryDocumentType type, Long headerId);

    void deleteLinesByHeaderId(InventoryDocumentType type, Long headerId);

    void saveLine(InventoryDocumentType type, InventoryDocumentLine line);
}
