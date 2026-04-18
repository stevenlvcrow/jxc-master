package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.WarehouseItemRuleAdministrationService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.domain.repository.WarehouseItemRuleRepository;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseItemRuleCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.WarehouseItemRuleUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
/**
 * 仓库物料规则管理接口，负责规则列表、详情、新建、更新和删除。
 */
public class IdentityWarehouseRuleAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final WarehouseItemRuleAdministrationService warehouseItemRuleAdministrationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造仓库物料规则管理接口。
     *
     * @param identityAccessControlService 组织权限控制服务
     * @param warehouseItemRuleAdministrationService 规则管理服务
     * @param identityAdminLookupService 查询辅助服务
     * @param identityAdminSupport 当前登录管理员辅助服务
     */
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
    /**
     * 查询分组下的仓库物料规则列表。
     *
     * @param groupId 分组主键
     * @return 规则列表响应
     */
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
    /**
     * 查询规则详情。
     *
     * @param id 规则主键
     * @return 规则详情响应
     */
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
                        .map(category -> new CategoryDetailRow(
                                category.id(),
                                category.categoryCode(),
                                category.categoryName(),
                                category.parentCategory(),
                                category.childCategory()))
                        .toList(),
                detail.warehouses().stream()
                        .map(warehouse -> new WarehouseDetailRow(warehouse.id(), warehouse.warehouseId(), warehouse.warehouseName()))
                        .toList()
        ));
    }

    @PostMapping("/groups/{groupId}/item-rules")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建仓库物料规则。
     *
     * @param groupId 分组主键
     * @param request 规则新增请求
     * @return 新建结果
     */
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
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新仓库物料规则。
     *
     * @param id 规则主键
     * @param request 规则更新请求
     * @return 空响应
     */
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
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除仓库物料规则。
     *
     * @param id 规则主键
     * @return 空响应
     */
    public CodeDataResponse<Void> deleteItemRule(@PathVariable Long id) {
        WarehouseItemRuleRepository.RuleRecord rule = warehouseItemRuleAdministrationService.requireRule(id);
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), rule.groupId());
        warehouseItemRuleAdministrationService.deleteRule(id);
        return CodeDataResponse.ok(null);
    }
}
