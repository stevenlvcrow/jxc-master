package com.boboboom.jxc.inventory.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.inventory.application.service.InventoryCheckHeader;
import com.boboboom.jxc.inventory.application.service.InventoryCheckKind;
import com.boboboom.jxc.inventory.application.service.InventoryCheckLine;

/**
 * 盘点单仓储。
 */
public interface InventoryCheckRepository {

    /**
     * 按作用域和类型查询盘点单头数据。
     *
     * @param kind 盘点单类型
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @return 头数据
     */
    List<InventoryCheckHeader> findHeadersByScopeAndKindOrdered(InventoryCheckKind kind, String scopeType, Long scopeId);

    /**
     * 按作用域、类型和主键集合查询盘点单。
     *
     * @param kind 盘点单类型
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param createdBy 创建人
     * @param viewAll 是否可查看全部
     * @param ids 主键集合
     * @return 头数据
     */
    List<InventoryCheckHeader> findHeadersByScopeAndKindAndIds(InventoryCheckKind kind,
                                                               String scopeType,
                                                               Long scopeId,
                                                               Long createdBy,
                                                               boolean viewAll,
                                                               List<Long> ids);

    /**
     * 按作用域、类型和主键查询盘点单。
     *
     * @param kind 盘点单类型
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param createdBy 创建人
     * @param viewAll 是否可查看全部
     * @param id 主键
     * @return 盘点单
     */
    Optional<InventoryCheckHeader> findHeaderByScopeAndKindAndId(InventoryCheckKind kind,
                                                                 String scopeType,
                                                                 Long scopeId,
                                                                 Long createdBy,
                                                                 boolean viewAll,
                                                                 Long id);

    /**
     * 按作用域和单号统计盘点单数量。
     *
     * @param kind 盘点单类型
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param documentCode 单据编号
     * @return 数量
     */
    Long countByScopeAndKindAndDocumentCode(InventoryCheckKind kind, String scopeType, Long scopeId, String documentCode);

    void saveHeader(InventoryCheckKind kind, InventoryCheckHeader header);

    void updateHeader(InventoryCheckKind kind, InventoryCheckHeader header);

    void deleteHeaderById(InventoryCheckKind kind, Long id);

    List<InventoryCheckLine> findLinesByHeaderIds(InventoryCheckKind kind, List<Long> headerIds);

    List<InventoryCheckLine> findLinesByHeaderId(InventoryCheckKind kind, Long headerId);

    void deleteLinesByHeaderId(InventoryCheckKind kind, Long headerId);

    void saveLine(InventoryCheckKind kind, InventoryCheckLine line);
}
