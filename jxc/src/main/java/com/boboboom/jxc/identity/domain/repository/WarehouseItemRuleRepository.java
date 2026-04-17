package com.boboboom.jxc.identity.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WarehouseItemRuleRepository {

    List<RuleSummary> findRuleSummariesByGroupId(Long groupId);

    Optional<RuleRecord> findRuleById(Long ruleId);

    RuleDetailData findRuleDetailById(Long ruleId);

    Long saveRule(RuleRecord rule);

    void deleteRuleById(Long ruleId);

    void replaceRuleDetails(Long ruleId,
                            List<RuleItemData> items,
                            List<RuleCategoryData> categories,
                            List<RuleWarehouseData> warehouses);

    List<String> findAllRuleCodes();

    record RuleSummary(Long id,
                       String ruleCode,
                       String ruleName,
                       boolean businessControl,
                       String status,
                       String createdBy,
                       LocalDateTime createdAt,
                       String updatedBy,
                       LocalDateTime updatedAt) {
    }

    record RuleRecord(Long id,
                      Long groupId,
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
                      LocalDateTime updatedAt) {
    }

    record RuleDetailData(RuleRecord rule,
                          List<RuleItemData> items,
                          List<RuleCategoryData> categories,
                          List<RuleWarehouseData> warehouses) {
    }

    record RuleItemData(Long id,
                        String itemCode,
                        String itemName,
                        String specModel,
                        String itemCategory) {
    }

    record RuleCategoryData(Long id,
                            String categoryCode,
                            String categoryName,
                            String parentCategory,
                            String childCategory) {
    }

    record RuleWarehouseData(Long id,
                             Long warehouseId,
                             String warehouseName) {
    }
}
