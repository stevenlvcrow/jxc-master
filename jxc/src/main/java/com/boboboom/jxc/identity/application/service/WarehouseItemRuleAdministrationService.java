package com.boboboom.jxc.identity.application.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.common.dictionary.DictionaryCodes;
import com.boboboom.jxc.identity.domain.repository.WarehouseItemRuleRepository;

/** 身份与权限服务，负责相关业务规则和流程协作。 */
@Service
public class WarehouseItemRuleAdministrationService {

    private static final String RULE_CODE_PREFIX = "CKWP";

    private final WarehouseItemRuleRepository warehouseItemRuleRepository;
    private final BusinessCodeGenerator businessCodeGenerator;
    private final DictionaryLookupService dictionaryLookupService;

    /** 身份与权限服务，负责相关业务规则和流程协作。 */
    public WarehouseItemRuleAdministrationService(WarehouseItemRuleRepository warehouseItemRuleRepositoryValue,
                                                  BusinessCodeGenerator businessCodeGeneratorValue,
                                                  DictionaryLookupService dictionaryLookupServiceValue) {
        this.warehouseItemRuleRepository = warehouseItemRuleRepositoryValue;
        this.businessCodeGenerator = businessCodeGeneratorValue;
        this.dictionaryLookupService = dictionaryLookupServiceValue;
    }

    /** 查询规则列表。 */
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

    /** 获取RuleDetail。 */
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

    /** 查询并校验规则存在。 */
    public RuleRecordData requireRule(Long ruleId) {
        WarehouseItemRuleRepository.RuleRecord rule = warehouseItemRuleRepository.findRuleById(ruleId)
                .orElseThrow(() -> new BusinessException("规则不存在"));
        return toRuleRecordData(rule);
    }

    /** 创建规则。 */
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
                enabledStatus(),
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

    /** 更新规则。 */
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

    /** 删除规则。 */
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

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
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

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
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

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public record RuleDetailData(RuleRecordData rule,
                                 List<RuleItemData> items,
                                 List<RuleCategoryData> categories,
                                 List<RuleWarehouseData> warehouses) {
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public record RuleItemData(Long id,
                               String itemCode,
                               String itemName,
                               String specModel,
                               String itemCategory) {
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public record RuleCategoryData(Long id,
                                   String categoryCode,
                                   String categoryName,
                                   String parentCategory,
                                   String childCategory) {
    }

    /** 身份与权限分页数据模型，承载列表数据和分页信息。 */
    public record RuleWarehouseData(Long id,
                                    Long warehouseId,
                                    String warehouseName) {
    }

    /** 身份与权限命令模型，承载Create规则命令写操作参数。 */
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

    /** 身份与权限命令模型，承载Update规则命令写操作参数。 */
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

    private String enabledStatus() {
        return dictionaryLookupService.codeOf(DictionaryCodes.COMMON_ENABLED_STATUS, DictionaryCodes.ENABLED);
    }
}
