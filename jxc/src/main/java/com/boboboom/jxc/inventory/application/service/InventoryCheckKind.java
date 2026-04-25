package com.boboboom.jxc.inventory.application.service;

import java.util.Arrays;

import com.boboboom.jxc.common.BusinessException;

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

    InventoryCheckKind(String pathSegmentValue, String documentPrefixValue, String headerTableValue, String lineTableValue, boolean multiValue) {
        this.pathSegment = pathSegmentValue;
        this.documentPrefix = documentPrefixValue;
        this.headerTable = headerTableValue;
        this.lineTable = lineTableValue;
        this.multi = multiValue;
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

    /** 获取DocumentPrefix。 */
    public String getDocumentPrefix() {
        return documentPrefix;
    }

    /** 获取HeaderTable。 */
    public String getHeaderTable() {
        return headerTable;
    }

    /** 获取LineTable。 */
    public String getLineTable() {
        return lineTable;
    }

    /** 判断Multi。 */
    public boolean isMulti() {
        return multi;
    }

    /** 获取BusinessCode。 */
    public String getBusinessCode() {
        return name();
    }
}
