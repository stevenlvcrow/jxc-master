package com.boboboom.jxc.identity.interfaces.rest;

import java.time.LocalDateTime;
import java.util.List;

record IdPayload(Long id) {
}

record RoleAssignmentView(Long roleId,
                          String roleCode,
                          String roleName,
                          String roleType,
                          String scopeType,
                          Long scopeId,
                          String scopeName,
                          boolean builtin) {
}

record GroupAdminView(Long id,
                      String groupCode,
                      String groupName,
                      String status,
                      String remark,
                      LocalDateTime createdAt) {
}

record BindGroupAdminResult(Long groupId,
                            String groupName,
                            Long userId,
                            String phone,
                            String realName) {
}

record GroupAdminCandidateView(Long userId,
                               String realName,
                               String phone,
                               Long storeId,
                               String storeCode,
                               String storeName) {
}

record StoreAdminView(Long id,
                      Long groupId,
                      String storeCode,
                      String storeName,
                      String status,
                      String contactName,
                      String contactPhone,
                      String address,
                      String remark,
                      LocalDateTime createdAt) {
}

record UserAdminView(Long id,
                     String username,
                     String realName,
                     String phone,
                     String status,
                     LocalDateTime createdAt,
                     List<RoleAssignmentView> roles) {
}

record SalesmanCandidateView(Long userId,
                             String realName,
                             String phone) {
}

record RoleAdminView(Long id,
                     String roleCode,
                     Long tenantGroupId,
                     String tenantGroupName,
                     String roleName,
                     String roleType,
                     String dataScopeType,
                     String description,
                     String status,
                     List<Long> menuIds,
                     Boolean builtin,
                     Boolean editable) {
}

record MenuAdminView(Long id,
                     String menuCode,
                     String menuName,
                     Long parentId,
                     String menuType,
                     String routePath,
                     String permissionCode,
                     String status,
                     Integer sortNo) {
}

record WarehouseAdminView(Long id,
                          String warehouseCode,
                          String warehouseName,
                          String department,
                          String status,
                          String warehouseType,
                          String contactName,
                          String contactPhone,
                          String address,
                          String targetGrossMargin,
                          String idealPurchaseSaleRatio,
                          boolean isDefault,
                          LocalDateTime updatedAt) {
}

record RuleItemPayload(String itemCode,
                       String itemName,
                       String specModel,
                       String itemCategory) {
}

record RuleCategoryPayload(String categoryCode,
                           String categoryName,
                           String parentCategory,
                           String childCategory) {
}

record RuleWarehousePayload(Long warehouseId) {
}

record RuleListView(Long id,
                    String ruleCode,
                    String ruleName,
                    boolean businessControl,
                    String status,
                    String createdBy,
                    LocalDateTime createdAt,
                    String updatedBy,
                    LocalDateTime updatedAt) {
}

record RuleDetailView(Long id,
                      String ruleCode,
                      String ruleName,
                      boolean businessControl,
                      boolean controlOrder,
                      boolean controlPurchaseInbound,
                      boolean controlTransferInbound,
                      String status,
                      String createdBy,
                      LocalDateTime createdAt,
                      String updatedBy,
                      LocalDateTime updatedAt,
                      List<ItemDetailRow> items,
                      List<CategoryDetailRow> categories,
                      List<WarehouseDetailRow> warehouses) {
}

record ItemDetailRow(Long id,
                     String itemCode,
                     String itemName,
                     String specModel,
                     String itemCategory) {
}

record CategoryDetailRow(Long id,
                         String categoryCode,
                         String categoryName,
                         String parentCategory,
                         String childCategory) {
}

record WarehouseDetailRow(Long id,
                          Long warehouseId,
                          String warehouseName) {
}
