package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.WarehouseItemRuleAdministrationService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.domain.repository.WarehouseItemRuleRepository;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseItemRuleCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseItemRuleUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin")
public class IdentityWarehouseRuleAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final WarehouseItemRuleAdministrationService warehouseItemRuleAdministrationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    public IdentityWarehouseRuleAdminController(IdentityAccessControlService identityAccessControlService,
                                                WarehouseItemRuleAdministrationService warehouseItemRuleAdministrationService,
                                                IdentityAdminLookupService identityAdminLookupService,
                                                IdentityAdminSupport identityAdminSupport) {
        this.identityAccessControlService = identityAccessControlService;
        this.warehouseItemRuleAdministrationService = warehouseItemRuleAdministrationService;
        this.identityAdminLookupService = identityAdminLookupService;
        this.identityAdminSupport = identityAdminSupport;
    }

    @GetMapping("/groups/{groupId}/item-rules")
    public CodeDataResponse<List<RuleListView>> listItemRules(@PathVariable Long groupId) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        List<RuleListView> result = warehouseItemRuleAdministrationService.listRules(groupId).stream()
                .map(rule -> new RuleListView(
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
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/item-rules/{id}")
    public CodeDataResponse<RuleDetailView> getRuleDetail(@PathVariable Long id) {
        WarehouseItemRuleRepository.RuleDetailData detail = warehouseItemRuleAdministrationService.getRuleDetail(id);
        WarehouseItemRuleRepository.RuleRecord rule = detail.rule();
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), rule.groupId());
        return CodeDataResponse.ok(new RuleDetailView(
                rule.id(),
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
                rule.updatedAt(),
                detail.items().stream()
                        .map(item -> new ItemDetailRow(item.id(), item.itemCode(), item.itemName(), item.specModel(), item.itemCategory()))
                        .toList(),
                detail.categories().stream()
                        .map(category -> new CategoryDetailRow(category.id(), category.categoryCode(), category.categoryName(), category.parentCategory(), category.childCategory()))
                        .toList(),
                detail.warehouses().stream()
                        .map(warehouse -> new WarehouseDetailRow(warehouse.id(), warehouse.warehouseId(), warehouse.warehouseName()))
                        .toList()
        ));
    }

    @PostMapping("/groups/{groupId}/item-rules")
    public CodeDataResponse<IdPayload> createItemRule(@PathVariable Long groupId,
                                                      @Valid @RequestBody WarehouseItemRuleCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        Long ruleId = warehouseItemRuleAdministrationService.createRule(
                new WarehouseItemRuleAdministrationService.CreateRuleCommand(
                        groupId,
                        identityAdminLookupService.trim(request.ruleName()),
                        request.businessControl() != null && request.businessControl(),
                        request.controlOrder() != null && request.controlOrder(),
                        request.controlPurchaseInbound() != null && request.controlPurchaseInbound(),
                        request.controlTransferInbound() != null && request.controlTransferInbound(),
                        identityAdminSupport.currentOperatorUsername(),
                        request.items() == null ? null : request.items().stream()
                                .map(i -> new WarehouseItemRuleRepository.RuleItemData(null, i.itemCode(), i.itemName(), i.specModel(), i.itemCategory()))
                                .toList(),
                        request.categories() == null ? null : request.categories().stream()
                                .map(c -> new WarehouseItemRuleRepository.RuleCategoryData(null, c.categoryCode(), c.categoryName(), c.parentCategory(), c.childCategory()))
                                .toList(),
                        request.warehouses() == null ? null : request.warehouses().stream()
                                .map(w -> new WarehouseItemRuleRepository.RuleWarehouseData(null, w.warehouseId(), null))
                                .toList()
                )
        );
        return CodeDataResponse.ok(new IdPayload(ruleId));
    }

    @PutMapping("/item-rules/{id}")
    public CodeDataResponse<Void> updateItemRule(@PathVariable Long id,
                                                 @Valid @RequestBody WarehouseItemRuleUpdateRequest request) {
        WarehouseItemRuleRepository.RuleRecord rule = warehouseItemRuleAdministrationService.requireRule(id);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), rule.groupId());
        warehouseItemRuleAdministrationService.updateRule(
                new WarehouseItemRuleAdministrationService.UpdateRuleCommand(
                        id,
                        identityAdminLookupService.trim(request.ruleName()),
                        request.businessControl(),
                        request.controlOrder(),
                        request.controlPurchaseInbound(),
                        request.controlTransferInbound(),
                        identityAdminSupport.currentOperatorUsername(),
                        request.items() == null ? null : request.items().stream()
                                .map(i -> new WarehouseItemRuleRepository.RuleItemData(null, i.itemCode(), i.itemName(), i.specModel(), i.itemCategory()))
                                .toList(),
                        request.categories() == null ? null : request.categories().stream()
                                .map(c -> new WarehouseItemRuleRepository.RuleCategoryData(null, c.categoryCode(), c.categoryName(), c.parentCategory(), c.childCategory()))
                                .toList(),
                        request.warehouses() == null ? null : request.warehouses().stream()
                                .map(w -> new WarehouseItemRuleRepository.RuleWarehouseData(null, w.warehouseId(), null))
                                .toList()
                )
        );
        return CodeDataResponse.ok(null);
    }

    @DeleteMapping("/item-rules/{id}")
    public CodeDataResponse<Void> deleteItemRule(@PathVariable Long id) {
        WarehouseItemRuleRepository.RuleRecord rule = warehouseItemRuleAdministrationService.requireRule(id);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), rule.groupId());
        warehouseItemRuleAdministrationService.deleteRule(id);
        return CodeDataResponse.ok(null);
    }
}
