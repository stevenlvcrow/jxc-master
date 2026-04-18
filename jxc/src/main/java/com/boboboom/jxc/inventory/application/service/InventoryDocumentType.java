package com.boboboom.jxc.inventory.application.service;

import com.boboboom.jxc.common.BusinessException;

import java.util.Arrays;
import java.util.List;

/**
 * 库存单据业务类型定义。
 */
public enum InventoryDocumentType {

    PURCHASE_INBOUND("purchase-inbound", "PURCHASE_INBOUND", "采购入库", "PURK", "/inventory/1/2/view/", true, StockDirection.INBOUND),
    PURCHASE_RETURN_OUTBOUND("purchase-return-outbound", "PURCHASE_RETURN_OUTBOUND", "采购退货出库", "THCK", "/inventory/1/3/view/", true, StockDirection.OUTBOUND),
    DEPARTMENT_PICKING("department-picking", "DEPARTMENT_PICKING", "部门领料", "BM", "/inventory/1/4/view/", true, StockDirection.OUTBOUND),
    DEPARTMENT_RETURN("department-return", "DEPARTMENT_RETURN", "部门退料", "BT", "/inventory/1/5/view/", true, StockDirection.INBOUND),
    STOCK_TRANSFER("stock-transfer", "STOCK_TRANSFER", "移库单", "YK", "/inventory/1/6/view/", true, StockDirection.OUTBOUND),
    STOCK_TRANSFER_INBOUND("stock-transfer-inbound", "STOCK_TRANSFER_INBOUND", "移库入库", "YKRK", "/inventory/1/7/view/", true, StockDirection.INBOUND),
    DEPARTMENT_TRANSFER("department-transfer", "DEPARTMENT_TRANSFER", "部门调拨", "BMDB", "/inventory/1/8/view/", true, StockDirection.OUTBOUND),
    DAMAGE_OUTBOUND("damage-outbound", "DAMAGE_OUTBOUND", "报损出库", "BSCK", "/inventory/1/10/view/", true, StockDirection.OUTBOUND),
    OTHER_INBOUND("other-inbound", "OTHER_INBOUND", "其他入库", "QTRK", "/inventory/1/11/view/", true, StockDirection.INBOUND),
    OTHER_OUTBOUND("other-outbound", "OTHER_OUTBOUND", "其他出库", "QTCK", "/inventory/1/12/view/", true, StockDirection.OUTBOUND),
    PRODUCTION_INBOUND("production-inbound", "PRODUCTION_INBOUND", "生产入库", "SCRK", "/inventory/1/13/view/", true, StockDirection.INBOUND),
    CUSTOMER_SALES_OUTBOUND("customer-sales-outbound", "CUSTOMER_SALES_OUTBOUND", "客户销售出库", "XSCK", "/inventory/1/14/view/", true, StockDirection.OUTBOUND),
    CUSTOMER_RETURN_INBOUND("customer-return-inbound", "CUSTOMER_RETURN_INBOUND", "客户退货入库", "KHTH", "/inventory/1/15/view/", true, StockDirection.INBOUND),
    WAREHOUSE_OPENING_BALANCE("warehouse-opening-balance", "WAREHOUSE_OPENING_BALANCE", "仓库期初", "CKQC", "/inventory/1/1/view/", false, StockDirection.NONE),
    STORE_TRANSFER("store-transfer", "STORE_TRANSFER", "店间调拨", "DJDB", "/inventory/1/9/view/", false, StockDirection.NONE),
    STOCK_TRANSFER_OUTBOUND("stock-transfer-outbound", "STOCK_TRANSFER_OUTBOUND", "移库出库", "YKCK", "/inventory/1/16/view/", false, StockDirection.NONE);

    private final String pathSegment;
    private final String businessCode;
    private final String businessName;
    private final String documentPrefix;
    private final String routeViewPrefix;
    private final boolean workflowEnabled;
    private final StockDirection stockDirection;

    InventoryDocumentType(String pathSegment,
                          String businessCode,
                          String businessName,
                          String documentPrefix,
                          String routeViewPrefix,
                          boolean workflowEnabled,
                          StockDirection stockDirection) {
        this.pathSegment = pathSegment;
        this.businessCode = businessCode;
        this.businessName = businessName;
        this.documentPrefix = documentPrefix;
        this.routeViewPrefix = routeViewPrefix;
        this.workflowEnabled = workflowEnabled;
        this.stockDirection = stockDirection;
    }

    /**
     * 按路径片段解析业务类型。
     *
     * @param pathSegment 路径片段
     * @return 业务类型
     */
    public static InventoryDocumentType fromPathSegment(String pathSegment) {
        return Arrays.stream(values())
                .filter(item -> item.pathSegment.equals(pathSegment))
                .findFirst()
                .orElseThrow(() -> new BusinessException("不支持的库存单据业务"));
    }

    /**
     * 返回除采购入库外的库存单据列表。
     *
     * @return 业务列表
     */
    public static List<InventoryDocumentType> managedTypes() {
        return Arrays.stream(values())
                .filter(item -> item != PURCHASE_INBOUND)
                .toList();
    }

    /**
     * 返回需要初始化流程的业务列表。
     *
     * @return 流程业务列表
     */
    public static List<InventoryDocumentType> workflowTypes() {
        return Arrays.stream(values())
                .filter(InventoryDocumentType::isWorkflowEnabled)
                .toList();
    }

    public String getPathSegment() {
        return pathSegment;
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public String getDocumentPrefix() {
        return documentPrefix;
    }

    public String getRouteViewPrefix() {
        return routeViewPrefix;
    }

    public boolean isWorkflowEnabled() {
        return workflowEnabled;
    }

    public StockDirection getStockDirection() {
        return stockDirection;
    }

    public String headerTable() {
        return "inventory_" + pathSegment.replace('-', '_');
    }

    public String lineTable() {
        return headerTable() + "_line";
    }

    /**
     * 库存方向定义。
     */
    public enum StockDirection {
        INBOUND,
        OUTBOUND,
        NONE
    }
}
