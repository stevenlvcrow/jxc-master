package com.boboboom.jxc.identity.application.event;

import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.OrgAdminProvisioningService;
import com.boboboom.jxc.identity.application.service.StoreSampleDataInitializationService;
import com.boboboom.jxc.workflow.application.service.InventoryWorkflowBootstrapService;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 组织创建事件订阅者，集中编排集团和门店创建后的初始化动作。
 */
@Component
public class OrganizationCreatedEventListener {

    private final IdentityAdminLookupService identityAdminLookupService;
    private final OrgAdminProvisioningService orgAdminProvisioningService;
    private final InventoryWorkflowBootstrapService inventoryWorkflowBootstrapService;
    private final StoreSampleDataInitializationService storeSampleDataInitializationService;

    public OrganizationCreatedEventListener(IdentityAdminLookupService identityAdminLookupService,
                                            OrgAdminProvisioningService orgAdminProvisioningService,
                                            InventoryWorkflowBootstrapService inventoryWorkflowBootstrapService,
                                            StoreSampleDataInitializationService storeSampleDataInitializationService) {
        this.identityAdminLookupService = identityAdminLookupService;
        this.orgAdminProvisioningService = orgAdminProvisioningService;
        this.inventoryWorkflowBootstrapService = inventoryWorkflowBootstrapService;
        this.storeSampleDataInitializationService = storeSampleDataInitializationService;
    }

    /**
     * 初始化集团内置角色。
     *
     * @param event 集团创建事件
     */
    @Order(10)
    @EventListener
    public void initializeGroupRoles(GroupCreatedEvent event) {
        identityAdminLookupService.ensureGroupBuiltinRoles(event.groupId(), event.operatorId());
    }

    /**
     * 创建集团管理员账号并绑定集团管理员角色。
     *
     * @param event 集团创建事件
     */
    @Order(20)
    @EventListener
    public void createGroupAdmin(GroupCreatedEvent event) {
        orgAdminProvisioningService.createGroupAdmin(
                event.groupId(),
                event.operatorId(),
                event.adminRealName(),
                event.adminPhone()
        );
    }

    /**
     * 初始化集团库存业务流程。
     *
     * @param event 集团创建事件
     */
    @Order(30)
    @EventListener
    public void initializeGroupWorkflow(GroupCreatedEvent event) {
        inventoryWorkflowBootstrapService.ensureDefaults(event.groupId(), event.operatorId());
    }

    /**
     * 初始化门店物品基础档案模板数据。
     *
     * @param event 门店创建事件
     */
    @Order(10)
    @EventListener
    public void initializeStoreSampleData(StoreCreatedEvent event) {
        storeSampleDataInitializationService.initializeStoreSampleData(event.storeId());
    }

    /**
     * 创建门店管理员账号并绑定门店管理员角色。
     *
     * @param event 门店创建事件
     */
    @Order(20)
    @EventListener
    public void createStoreAdmin(StoreCreatedEvent event) {
        orgAdminProvisioningService.createStoreAdmin(
                event.groupId(),
                event.storeId(),
                event.operatorId(),
                event.adminRealName(),
                event.adminPhone()
        );
    }
}
