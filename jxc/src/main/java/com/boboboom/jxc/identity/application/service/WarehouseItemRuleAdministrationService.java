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

    public List<WarehouseItemRuleRepository.RuleSummary> listRules(Long groupId) {
        return warehouseItemRuleRepository.findRuleSummariesByGroupId(groupId);
    }

    public WarehouseItemRuleRepository.RuleDetailData getRuleDetail(Long ruleId) {
        WarehouseItemRuleRepository.RuleDetailData detail = warehouseItemRuleRepository.findRuleDetailById(ruleId);
        if (detail == null || detail.rule() == null) {
            throw new BusinessException("规则不存在");
        }
        return detail;
    }

    public WarehouseItemRuleRepository.RuleRecord requireRule(Long ruleId) {
        return warehouseItemRuleRepository.findRuleById(ruleId)
                .orElseThrow(() -> new BusinessException("规则不存在"));
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
        warehouseItemRuleRepository.replaceRuleDetails(ruleId, command.items(), command.categories(), command.warehouses());
        return ruleId;
    }

    @Transactional
    public void updateRule(UpdateRuleCommand command) {
        WarehouseItemRuleRepository.RuleRecord current = requireRule(command.ruleId());
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
        warehouseItemRuleRepository.replaceRuleDetails(command.ruleId(), command.items(), command.categories(), command.warehouses());
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

    public record CreateRuleCommand(Long groupId,
                                    String ruleName,
                                    boolean businessControl,
                                    boolean controlOrder,
                                    boolean controlPurchaseInbound,
                                    boolean controlTransferInbound,
                                    String operatorUsername,
                                    List<WarehouseItemRuleRepository.RuleItemData> items,
                                    List<WarehouseItemRuleRepository.RuleCategoryData> categories,
                                    List<WarehouseItemRuleRepository.RuleWarehouseData> warehouses) {
    }

    public record UpdateRuleCommand(Long ruleId,
                                    String ruleName,
                                    Boolean businessControl,
                                    Boolean controlOrder,
                                    Boolean controlPurchaseInbound,
                                    Boolean controlTransferInbound,
                                    String operatorUsername,
                                    List<WarehouseItemRuleRepository.RuleItemData> items,
                                    List<WarehouseItemRuleRepository.RuleCategoryData> categories,
                                    List<WarehouseItemRuleRepository.RuleWarehouseData> warehouses) {
    }
}
