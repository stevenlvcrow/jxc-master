package com.boboboom.jxc.identity.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** 身份与权限仓储接口，定义领域需要的数据访问能力。 */
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

    /**
     * 仓库规则摘要。
     */
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

    /**
     * 仓库规则主记录。
     */
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

    /**
     * 仓库规则详情。
     */
    record RuleDetailData(RuleRecord rule,
                          List<RuleItemData> items,
                          List<RuleCategoryData> categories,
                          List<RuleWarehouseData> warehouses) {
    }

    /**
     * 规则物品范围。
     */
    record RuleItemData(Long id,
                        String itemCode,
                        String itemName,
                        String specModel,
                        String itemCategory) {
    }

    /**
     * 规则类别范围。
     */
    record RuleCategoryData(Long id,
                            String categoryCode,
                            String categoryName,
                            String parentCategory,
                            String childCategory) {
    }

    /**
     * 规则仓库范围。
     */
    record RuleWarehouseData(Long id,
                             Long warehouseId,
                             String warehouseName) {
    }
}
