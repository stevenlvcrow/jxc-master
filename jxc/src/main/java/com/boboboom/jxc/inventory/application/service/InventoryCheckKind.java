package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;

import java.util.Arrays;

/**
 * 盘点单文档类型。
 */
public enum InventoryCheckKind {

    /** 盘点单。 */
    INVENTORY_CHECK("inventory-checks", "PD", "inventory_inventory_check", "inventory_inventory_check_line", false),
    /** 多人盘点单。 */
    MULTI_INVENTORY_CHECK("multi-inventory-checks", "MPD", "inventory_multi_inventory_check", "inventory_multi_inventory_check_line", true);

    private final String pathSegment;
    private final String documentPrefix;
    private final String headerTable;
    private final String lineTable;
    private final boolean multi;

    InventoryCheckKind(String pathSegment, String documentPrefix, String headerTable, String lineTable, boolean multi) {
        this.pathSegment = pathSegment;
        this.documentPrefix = documentPrefix;
        this.headerTable = headerTable;
        this.lineTable = lineTable;
        this.multi = multi;
    }

    /**
     * 按路径片段解析盘点单类型。
     *
     * @param pathSegment 路径片段
     * @return 盘点单类型
     */
    public static InventoryCheckKind fromPathSegment(String pathSegment) {
        return Arrays.stream(values())
                .filter(item -> item.pathSegment.equals(pathSegment))
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的盘点单类型"));
    }

    public String getDocumentPrefix() {
        return documentPrefix;
    }

    public String getHeaderTable() {
        return headerTable;
    }

    public String getLineTable() {
        return lineTable;
    }

    public boolean isMulti() {
        return multi;
    }
}
