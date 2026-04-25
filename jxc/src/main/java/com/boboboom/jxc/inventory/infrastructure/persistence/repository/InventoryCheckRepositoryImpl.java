package com.boboboom.jxc.inventory.infrastructure.persistence.repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.inventory.application.service.InventoryCheckHeader;
import com.boboboom.jxc.inventory.application.service.InventoryCheckKind;
import com.boboboom.jxc.inventory.application.service.InventoryCheckLine;
import com.boboboom.jxc.inventory.domain.repository.InventoryCheckRepository;
import com.boboboom.jxc.inventory.infrastructure.persistence.mapper.InventoryCheckMapper;

/**
 * 盘点单仓储实现。
 */
@Repository
public class InventoryCheckRepositoryImpl implements InventoryCheckRepository {

    private final InventoryCheckMapper inventoryCheckMapper;

    /**
     * 创建盘点单仓储实现。
     *
     * @param inventoryCheckMapperValue 盘点单持久化映射
     */
    public InventoryCheckRepositoryImpl(InventoryCheckMapper inventoryCheckMapperValue) {
        this.inventoryCheckMapper = inventoryCheckMapperValue;
    }

    @Override
    public List<InventoryCheckHeader> findHeadersByScopeAndKindOrdered(InventoryCheckKind kind, String scopeType, Long scopeId) {
        return inventoryCheckMapper.selectHeadersByScopeOrdered(kind.getHeaderTable(), scopeType, scopeId);
    }

    @Override
    public List<InventoryCheckHeader> findHeadersByScopeAndKindAndIds(InventoryCheckKind kind,
                                                                      String scopeType,
                                                                      Long scopeId,
                                                                      Long createdBy,
                                                                      boolean viewAll,
                                                                      List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryCheckMapper.selectHeadersByScopeAndIds(kind.getHeaderTable(), scopeType, scopeId, createdBy, viewAll, ids);
    }

    @Override
    public Optional<InventoryCheckHeader> findHeaderByScopeAndKindAndId(InventoryCheckKind kind,
                                                                        String scopeType,
                                                                        Long scopeId,
                                                                        Long createdBy,
                                                                        boolean viewAll,
                                                                        Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return inventoryCheckMapper.selectHeaderByScopeAndId(kind.getHeaderTable(), scopeType, scopeId, createdBy, viewAll, id)
                .stream()
                .findFirst();
    }

    @Override
    public Long countByScopeAndKindAndDocumentCode(InventoryCheckKind kind, String scopeType, Long scopeId, String documentCode) {
        Long count = inventoryCheckMapper.countByScopeAndDocumentCode(kind.getHeaderTable(), scopeType, scopeId, documentCode);
        return count == null ? 0L : count;
    }

    @Override
    public void saveHeader(InventoryCheckKind kind, InventoryCheckHeader header) {
        inventoryCheckMapper.insertHeader(kind.getHeaderTable(), header);
    }

    @Override
    public void updateHeader(InventoryCheckKind kind, InventoryCheckHeader header) {
        inventoryCheckMapper.updateHeader(kind.getHeaderTable(), header);
    }

    @Override
    public void deleteHeaderById(InventoryCheckKind kind, Long id) {
        inventoryCheckMapper.deleteHeaderById(kind.getHeaderTable(), id);
    }

    @Override
    public List<InventoryCheckLine> findLinesByHeaderIds(InventoryCheckKind kind, List<Long> headerIds) {
        if (headerIds == null || headerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return inventoryCheckMapper.selectLinesByHeaderIds(kind.getLineTable(), headerIds);
    }

    @Override
    public List<InventoryCheckLine> findLinesByHeaderId(InventoryCheckKind kind, Long headerId) {
        if (headerId == null) {
            return Collections.emptyList();
        }
        return inventoryCheckMapper.selectLinesByHeaderId(kind.getLineTable(), headerId);
    }

    @Override
    public void deleteLinesByHeaderId(InventoryCheckKind kind, Long headerId) {
        inventoryCheckMapper.deleteLinesByHeaderId(kind.getLineTable(), headerId);
    }

    @Override
    public void saveLine(InventoryCheckKind kind, InventoryCheckLine line) {
        inventoryCheckMapper.insertLine(kind.getLineTable(), line);
    }
}
