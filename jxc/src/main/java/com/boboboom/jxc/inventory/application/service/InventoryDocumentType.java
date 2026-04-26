package com.boboboom.jxc.inventory.application.service;

import java.util.Arrays;
import java.util.List;

import com.boboboom.jxc.common.BusinessException;

/**
 * 库存单据业务类型定义。
 */
public enum InventoryDocumentType {

    PURCHASE_INBOUND("purchase-inbound", "PURCHASE_INBOUND", "采购入库", "PURK", "/inventory/purchase-inbounds/view/", true, StockDirection.INBOUND),
    PURCHASE_RETURN_OUTBOUND("purchase-return-outbound", "PURCHASE_RETURN_OUTBOUND", "采购退货出库", "THCK", "/inventory/purchase-return-outbounds/view/", true, StockDirection.OUTBOUND),
    DEPARTMENT_PICKING("department-picking", "DEPARTMENT_PICKING", "部门领料", "BM", "/inventory/department-pickings/view/", true, StockDirection.OUTBOUND),
    DEPARTMENT_RETURN("department-return", "DEPARTMENT_RETURN", "部门退料", "BT", "/inventory/department-returns/view/", true, StockDirection.INBOUND),
    STOCK_TRANSFER("stock-transfer", "STOCK_TRANSFER", "移库单", "YK", "/inventory/stock-transfers/view/", true, StockDirection.OUTBOUND),
    STOCK_TRANSFER_INBOUND("stock-transfer-inbound", "STOCK_TRANSFER_INBOUND", "移库入库", "YKRK", "/inventory/stock-transfer-inbounds/view/", true, StockDirection.INBOUND),
    DEPARTMENT_TRANSFER("department-transfer", "DEPARTMENT_TRANSFER", "部门调拨", "BMDB", "/inventory/department-transfers/view/", true, StockDirection.OUTBOUND),
    DAMAGE_OUTBOUND("damage-outbound", "DAMAGE_OUTBOUND", "报损出库", "BSCK", "/inventory/damage-outbounds/view/", true, StockDirection.OUTBOUND),
    OTHER_INBOUND("other-inbound", "OTHER_INBOUND", "其他入库", "QTRK", "/inventory/other-inbounds/view/", true, StockDirection.INBOUND),
    OTHER_OUTBOUND("other-outbound", "OTHER_OUTBOUND", "其他出库", "QTCK", "/inventory/other-outbounds/view/", true, StockDirection.OUTBOUND),
    PROFIT_INBOUND("profit-inbound", "PROFIT_INBOUND", "盘盈单", "PY", "/inventory/profit-inbounds/view/", false, StockDirection.INBOUND),
    LOSS_OUTBOUND("loss-outbound", "LOSS_OUTBOUND", "盘亏单", "PK", "/inventory/loss-outbounds/view/", false, StockDirection.OUTBOUND),
    PRODUCTION_INBOUND("production-inbound", "PRODUCTION_INBOUND", "生产入库", "SCRK", "/inventory/production-inbounds/view/", true, StockDirection.INBOUND),
    CUSTOMER_SALES_OUTBOUND("customer-sales-outbound", "CUSTOMER_SALES_OUTBOUND", "客户销售出库", "XSCK", "/inventory/customer-sales-outbounds/view/", true, StockDirection.OUTBOUND),
    CUSTOMER_RETURN_INBOUND("customer-return-inbound", "CUSTOMER_RETURN_INBOUND", "客户退货入库", "KHTH", "/inventory/customer-return-inbounds/view/", true, StockDirection.INBOUND),
    DISH_CONSUMPTION_OUTBOUND("dish-consumption-outbound", "DISH_CONSUMPTION_OUTBOUND", "菜品消耗出库", "CPC", "/inventory/dish-consumption-outbounds/view/", true, StockDirection.OUTBOUND),
    WAREHOUSE_OPENING_BALANCE("warehouse-opening-balance", "WAREHOUSE_OPENING_BALANCE", "仓库期初", "CKQC", "/inventory/warehouse-opening-balances/view/", false, StockDirection.NONE),
    STORE_TRANSFER("store-transfer", "STORE_TRANSFER", "店间调拨", "DJDB", "/inventory/store-transfers/view/", true, StockDirection.OUTBOUND),
    STOCK_TRANSFER_OUTBOUND("stock-transfer-outbound", "STOCK_TRANSFER_OUTBOUND", "移库出库", "YKCK", "/inventory/stock-transfer-outbounds/view/", true, StockDirection.OUTBOUND);

    private final String pathSegment;
    private final String businessCode;
    private final String businessName;
    private final String documentPrefix;
    private final String routeViewPrefix;
    private final boolean workflowEnabled;
    private final StockDirection stockDirection;

    InventoryDocumentType(String pathSegmentValue,
                          String businessCodeValue,
                          String businessNameValue,
                          String documentPrefixValue,
                          String routeViewPrefixValue,
                          boolean workflowEnabledValue,
                          StockDirection stockDirectionValue) {
        this.pathSegment = pathSegmentValue;
        this.businessCode = businessCodeValue;
        this.businessName = businessNameValue;
        this.documentPrefix = documentPrefixValue;
        this.routeViewPrefix = routeViewPrefixValue;
        this.workflowEnabled = workflowEnabledValue;
        this.stockDirection = stockDirectionValue;
    }

    /**
     * 按路径片段解析业务类型。
     *
     * @param pathSegment 路径片段
     * @return 业务类型
     */
    public static InventoryDocumentType fromPathSegment(String pathSegment) {
        return Arrays.stream(values())
                .filter(item -> item != WAREHOUSE_OPENING_BALANCE)
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
                .filter(item -> item != PURCHASE_INBOUND && item != WAREHOUSE_OPENING_BALANCE)
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

    /** 获取PathSegment。 */
    public String getPathSegment() {
        return pathSegment;
    }

    /** 获取BusinessCode。 */
    public String getBusinessCode() {
        return businessCode;
    }

    /** 获取BusinessName。 */
    public String getBusinessName() {
        return businessName;
    }

    /** 获取DocumentPrefix。 */
    public String getDocumentPrefix() {
        return documentPrefix;
    }

    /** 获取RouteViewPrefix。 */
    public String getRouteViewPrefix() {
        return routeViewPrefix;
    }

    /** 判断WorkflowEnabled。 */
    public boolean isWorkflowEnabled() {
        return workflowEnabled;
    }

    /** 获取StockDirection。 */
    public StockDirection getStockDirection() {
        return stockDirection;
    }

    /** 返回单据单头表名。 */
    public String headerTable() {
        return "inventory_" + pathSegment.replace('-', '_');
    }

    /** 返回单据明细表名。 */
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
