package com.boboboom.jxc.identity.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.WarehouseItemRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WarehouseItemRuleAdministrationService {

    private static final String RULE_CODE_PREFIX = "CKWP";
    private static final String STATUS_ENABLED = "ENABLED";

    private final WarehouseItemRuleRepository warehouseItemRuleRepository;
    private final BusinessCodeGenerator businessCodeGenerator;

    public WarehouseItemRuleAdministrationService(WarehouseItemRuleRepository warehouseItemRuleRepository,
                                                  BusinessCodeGenerator businessCodeGenerator) {
        this.warehouseItemRuleRepository = warehouseItemRuleRepository;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public List<RuleSummaryData> listRules(Long groupId) {
        return warehouseItemRuleRepository.findRuleSummariesByGroupId(groupId).stream()
                .map(rule -> new RuleSummaryData(
                        rule.id(),
                        rule.ruleCode(),
                        rule.ruleName(),
                        rule.businessControl(),
                        rule.status(),
                        rule.createdBy(),
                        rule.createdAt(),
                        rule.updatedBy(),
                        rule.updatedAt()
                ))
                .toList();
    }

    public RuleDetailData getRuleDetail(Long ruleId) {
        WarehouseItemRuleRepository.RuleDetailData detail = warehouseItemRuleRepository.findRuleDetailById(ruleId);
        if (detail == null || detail.rule() == null) {
            throw new BusinessException("规则不存在");
        }
        return new RuleDetailData(
                toRuleRecordData(detail.rule()),
                detail.items().stream()
                        .map(item -> new RuleItemData(item.id(), item.itemCode(), item.itemName(), item.specModel(), item.itemCategory()))
                        .toList(),
                detail.categories().stream()
                        .map(category -> new RuleCategoryData(
                                category.id(),
                                category.categoryCode(),
                                category.categoryName(),
                                category.parentCategory(),
                                category.childCategory()))
                        .toList(),
                detail.warehouses().stream()
                        .map(warehouse -> new RuleWarehouseData(warehouse.id(), warehouse.warehouseId(), warehouse.warehouseName()))
                        .toList()
        );
    }

    public RuleRecordData requireRule(Long ruleId) {
        WarehouseItemRuleRepository.RuleRecord rule = warehouseItemRuleRepository.findRuleById(ruleId)
                .orElseThrow(() -> new BusinessException("规则不存在"));
        return toRuleRecordData(rule);
    }

    @Transactional
    public Long createRule(CreateRuleCommand command) {
        WarehouseItemRuleRepository.RuleRecord rule = new WarehouseItemRuleRepository.RuleRecord(
                null,
                command.groupId(),
                generateRuleCode(),
                command.ruleName(),
                command.businessControl(),
                command.controlOrder(),
                command.controlPurchaseInbound(),
                command.controlTransferInbound(),
                STATUS_ENABLED,
                command.operatorUsername(),
                null,
                null,
                null
        );
        Long ruleId = warehouseItemRuleRepository.saveRule(rule);
        warehouseItemRuleRepository.replaceRuleDetails(
                ruleId,
                toRepositoryItems(command.items()),
                toRepositoryCategories(command.categories()),
                toRepositoryWarehouses(command.warehouses())
        );
        return ruleId;
    }

    @Transactional
    public void updateRule(UpdateRuleCommand command) {
        RuleRecordData current = requireRule(command.ruleId());
        WarehouseItemRuleRepository.RuleRecord updated = new WarehouseItemRuleRepository.RuleRecord(
                current.id(),
                current.groupId(),
                current.ruleCode(),
                command.ruleName(),
                command.businessControl() != null ? command.businessControl() : current.businessControl(),
                command.controlOrder() != null ? command.controlOrder() : current.controlOrder(),
                command.controlPurchaseInbound() != null ? command.controlPurchaseInbound() : current.controlPurchaseInbound(),
                command.controlTransferInbound() != null ? command.controlTransferInbound() : current.controlTransferInbound(),
                current.status(),
                current.createdBy(),
                current.createdAt(),
                command.operatorUsername(),
                LocalDateTime.now()
        );
        warehouseItemRuleRepository.saveRule(updated);
        warehouseItemRuleRepository.replaceRuleDetails(
                command.ruleId(),
                toRepositoryItems(command.items()),
                toRepositoryCategories(command.categories()),
                toRepositoryWarehouses(command.warehouses())
        );
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        requireRule(ruleId);
        warehouseItemRuleRepository.replaceRuleDetails(ruleId, null, null, null);
        warehouseItemRuleRepository.deleteRuleById(ruleId);
    }

    private String generateRuleCode() {
        return businessCodeGenerator.nextCode(RULE_CODE_PREFIX, warehouseItemRuleRepository.findAllRuleCodes());
    }

    private RuleRecordData toRuleRecordData(WarehouseItemRuleRepository.RuleRecord rule) {
        return new RuleRecordData(
                rule.id(),
                rule.groupId(),
                rule.ruleCode(),
                rule.ruleName(),
                rule.businessControl(),
                rule.controlOrder(),
                rule.controlPurchaseInbound(),
                rule.controlTransferInbound(),
                rule.status(),
                rule.createdBy(),
                rule.createdAt(),
                rule.updatedBy(),
                rule.updatedAt()
        );
    }

    private List<WarehouseItemRuleRepository.RuleItemData> toRepositoryItems(List<RuleItemData> items) {
        if (items == null) {
            return null;
        }
        return items.stream()
                .map(item -> new WarehouseItemRuleRepository.RuleItemData(
                        item.id(),
                        item.itemCode(),
                        item.itemName(),
                        item.specModel(),
                        item.itemCategory()))
                .toList();
    }

    private List<WarehouseItemRuleRepository.RuleCategoryData> toRepositoryCategories(List<RuleCategoryData> categories) {
        if (categories == null) {
            return null;
        }
        return categories.stream()
                .map(category -> new WarehouseItemRuleRepository.RuleCategoryData(
                        category.id(),
                        category.categoryCode(),
                        category.categoryName(),
                        category.parentCategory(),
                        category.childCategory()))
                .toList();
    }

    private List<WarehouseItemRuleRepository.RuleWarehouseData> toRepositoryWarehouses(List<RuleWarehouseData> warehouses) {
        if (warehouses == null) {
            return null;
        }
        return warehouses.stream()
                .map(warehouse -> new WarehouseItemRuleRepository.RuleWarehouseData(
                        warehouse.id(),
                        warehouse.warehouseId(),
                        warehouse.warehouseName()))
                .toList();
    }

    public record RuleSummaryData(Long id,
                                  String ruleCode,
                                  String ruleName,
                                  boolean businessControl,
                                  String status,
                                  String createdBy,
                                  LocalDateTime createdAt,
                                  String updatedBy,
                                  LocalDateTime updatedAt) {
    }

    public record RuleRecordData(Long id,
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

    public record RuleDetailData(RuleRecordData rule,
                                 List<RuleItemData> items,
                                 List<RuleCategoryData> categories,
                                 List<RuleWarehouseData> warehouses) {
    }

    public record RuleItemData(Long id,
                               String itemCode,
                               String itemName,
                               String specModel,
                               String itemCategory) {
    }

    public record RuleCategoryData(Long id,
                                   String categoryCode,
                                   String categoryName,
                                   String parentCategory,
                                   String childCategory) {
    }

    public record RuleWarehouseData(Long id,
                                    Long warehouseId,
                                    String warehouseName) {
    }

    public record CreateRuleCommand(Long groupId,
                                    String ruleName,
                                    boolean businessControl,
                                    boolean controlOrder,
                                    boolean controlPurchaseInbound,
                                    boolean controlTransferInbound,
                                    String operatorUsername,
                                    List<RuleItemData> items,
                                    List<RuleCategoryData> categories,
                                    List<RuleWarehouseData> warehouses) {
    }

    public record UpdateRuleCommand(Long ruleId,
                                    String ruleName,
                                    Boolean businessControl,
                                    Boolean controlOrder,
                                    Boolean controlPurchaseInbound,
                                    Boolean controlTransferInbound,
                                    String operatorUsername,
                                    List<RuleItemData> items,
                                    List<RuleCategoryData> categories,
                                    List<RuleWarehouseData> warehouses) {
    }
}
