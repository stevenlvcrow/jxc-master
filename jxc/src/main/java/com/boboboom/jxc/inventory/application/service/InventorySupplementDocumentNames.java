package com.boboboom.jxc.inventory.application.service;

import java.util.EnumMap;
import java.util.Map;

final class InventorySupplementDocumentNames {

    private static final Map<InventoryDocumentType, String> STOCK_STATISTIC_MODES = new EnumMap<>(InventoryDocumentType.class);
    private static final Map<InventoryDocumentType, String> STOCK_INOUT_TYPES = new EnumMap<>(InventoryDocumentType.class);
    private static final Map<InventoryDocumentType, String> STOCK_DIRECTIONS = new EnumMap<>(InventoryDocumentType.class);

    static {
        registerInbound(InventoryDocumentType.PURCHASE_INBOUND, "采购入库");
        registerOutbound(InventoryDocumentType.PURCHASE_RETURN_OUTBOUND, "采购退货出库");
        registerOutbound(InventoryDocumentType.DEPARTMENT_PICKING, "部门领料");
        registerInbound(InventoryDocumentType.DEPARTMENT_RETURN, "部门退料");
        registerOutbound(InventoryDocumentType.STOCK_TRANSFER, "移库出库");
        registerInbound(InventoryDocumentType.STOCK_TRANSFER_INBOUND, "移库入库");
        registerOutbound(InventoryDocumentType.DEPARTMENT_TRANSFER, "部门调拨");
        registerOutbound(InventoryDocumentType.DAMAGE_OUTBOUND, "报损出库");
        registerInbound(InventoryDocumentType.OTHER_INBOUND, "其他入库");
        registerOutbound(InventoryDocumentType.OTHER_OUTBOUND, "其他出库");
        registerInbound(InventoryDocumentType.PRODUCTION_INBOUND, "生产入库");
        registerOutbound(InventoryDocumentType.CUSTOMER_SALES_OUTBOUND, "客户销售出库");
        registerInbound(InventoryDocumentType.CUSTOMER_RETURN_INBOUND, "客户退货入库");
        registerOutbound(InventoryDocumentType.DISH_CONSUMPTION_OUTBOUND, "菜品消耗出库");
        registerOutbound(InventoryDocumentType.STORE_TRANSFER, "店间调拨");
        registerOutbound(InventoryDocumentType.STOCK_TRANSFER_OUTBOUND, "移库出库");
    }

    private InventorySupplementDocumentNames() {
    }

    static String stockStatisticMode(InventoryDocumentType type) {
        return STOCK_STATISTIC_MODES.getOrDefault(type, "从本店调出");
    }

    static String stockInoutType(InventoryDocumentType type) {
        return STOCK_INOUT_TYPES.getOrDefault(type, type.getBusinessName());
    }

    static String stockDirection(InventoryDocumentType type) {
        return STOCK_DIRECTIONS.getOrDefault(type, "出库");
    }

    static boolean isInboundWarehouseDocument(InventoryDocumentType type) {
        return "入库".equals(stockDirection(type));
    }

    private static void registerInbound(InventoryDocumentType type, String inoutType) {
        STOCK_STATISTIC_MODES.put(type, "调入本店");
        STOCK_DIRECTIONS.put(type, "入库");
        STOCK_INOUT_TYPES.put(type, inoutType);
    }

    private static void registerOutbound(InventoryDocumentType type, String inoutType) {
        STOCK_STATISTIC_MODES.put(type, "从本店调出");
        STOCK_DIRECTIONS.put(type, "出库");
        STOCK_INOUT_TYPES.put(type, inoutType);
    }
}
