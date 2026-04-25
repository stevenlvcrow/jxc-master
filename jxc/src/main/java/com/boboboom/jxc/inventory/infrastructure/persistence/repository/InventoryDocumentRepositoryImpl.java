package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.inventory.application.service.InventoryDocumentHeader;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentLine;
import com.boboboom.jxc.inventory.application.service.InventoryDocumentType;
import com.boboboom.jxc.inventory.domain.repository.InventoryDocumentRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryDocumentMapper;

/**
 * 通用库存单据仓储实现。
 */
@Repository
public class InventoryDocumentRepositoryImpl implements InventoryDocumentRepository {

    private static final String PURCHASE_INBOUND_LINE_HEADER_COLUMN = "inbound_id";
    private static final String DEFAULT_LINE_HEADER_COLUMN = "header_id";

    private final InventoryDocumentMapper inventoryDocumentMapper;

    /**
     * 创建通用库存单据仓储实现。
     *
     * @param inventoryDocumentMapperValue 通用库存单据持久化映射
     */
    public InventoryDocumentRepositoryImpl(InventoryDocumentMapper inventoryDocumentMapperValue) {
        this.inventoryDocumentMapper = inventoryDocumentMapperValue;
    }

    @Override
    public List<InventoryDocumentHeader> findHeadersByScopeOrdered(InventoryDocumentType type, String scopeType, Long scopeId) {
        return inventoryDocumentMapper.selectHeadersByScopeOrdered(type.headerTable(), isPurchaseInbound(type), scopeType, scopeId);
    }

    @Override
    public List<InventoryDocumentHeader> findHeadersByScopeAndIds(InventoryDocumentType type,
                                                                  String scopeType,
                                                                  Long scopeId,
                                                                  Long createdBy,
                                                                  boolean viewAll,
                                                                  List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryDocumentMapper.selectHeadersByScopeAndIds(type.headerTable(), isPurchaseInbound(type), scopeType, scopeId,
                createdBy, viewAll, ids);
    }

    @Override
    public Optional<InventoryDocumentHeader> findHeaderByScopeAndId(InventoryDocumentType type,
                                                                    String scopeType,
                                                                    Long scopeId,
                                                                    Long createdBy,
                                                                    boolean viewAll,
                                                                    Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return inventoryDocumentMapper.selectHeaderByScopeAndId(type.headerTable(), isPurchaseInbound(type), scopeType, scopeId,
                createdBy, viewAll, id)
                .stream()
                .findFirst();
    }

    @Override
    public Long countByScopeAndDocumentCode(InventoryDocumentType type, String scopeType, Long scopeId, String documentCode) {
        Long count = inventoryDocumentMapper.countByScopeAndDocumentCode(type.headerTable(), scopeType, scopeId, documentCode);
        return count == null ? 0L : count;
    }

    @Override
    public void saveHeader(InventoryDocumentType type, InventoryDocumentHeader header) {
        inventoryDocumentMapper.insertHeader(type.headerTable(), header);
    }

    @Override
    public void updateHeader(InventoryDocumentType type, InventoryDocumentHeader header) {
        inventoryDocumentMapper.updateHeader(type.headerTable(), header);
    }

    @Override
    public void deleteHeaderById(InventoryDocumentType type, Long id) {
        inventoryDocumentMapper.deleteHeaderById(type.headerTable(), id);
    }

    @Override
    public List<InventoryDocumentLine> findLinesByHeaderIds(InventoryDocumentType type, List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryDocumentMapper.selectLinesByHeaderIds(type.lineTable(), lineHeaderIdColumn(type), isPurchaseInbound(type), headerIds);
    }

    @Override
    public List<InventoryDocumentLine> findLinesByHeaderId(InventoryDocumentType type, Long headerId) {
        if (headerId == null) {
            return Collections.emptyList();
        }
        return inventoryDocumentMapper.selectLinesByHeaderId(type.lineTable(), lineHeaderIdColumn(type), isPurchaseInbound(type), headerId);
    }

    @Override
    public void deleteLinesByHeaderId(InventoryDocumentType type, Long headerId) {
        inventoryDocumentMapper.deleteLinesByHeaderId(type.lineTable(), headerId);
    }

    @Override
    public void saveLine(InventoryDocumentType type, InventoryDocumentLine line) {
        inventoryDocumentMapper.insertLine(type.lineTable(), line);
    }

    private boolean isPurchaseInbound(InventoryDocumentType type) {
        return type == InventoryDocumentType.PURCHASE_INBOUND;
    }

    private String lineHeaderIdColumn(InventoryDocumentType type) {
        return isPurchaseInbound(type) ? PURCHASE_INBOUND_LINE_HEADER_COLUMN : DEFAULT_LINE_HEADER_COLUMN;
    }
}
