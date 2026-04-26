package com.boboboom.jxc.workflow.application.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;

/**
 * 启动时发布已有集团的内置审批流程，避免只存在配置数据但缺失流程引擎定义。
 */
@Component
public class InventoryWorkflowBootstrapRunner implements ApplicationRunner {

    private final GroupRepository groupRepository;
    private final InventoryWorkflowBootstrapService inventoryWorkflowBootstrapService;

    /** 审批流程启动任务，负责应用启动后的初始化处理。 */
    public InventoryWorkflowBootstrapRunner(GroupRepository groupRepositoryValue,
                                            InventoryWorkflowBootstrapService inventoryWorkflowBootstrapServiceValue) {
        this.groupRepository = groupRepositoryValue;
        this.inventoryWorkflowBootstrapService = inventoryWorkflowBootstrapServiceValue;
    }

    /** 应用启动后执行初始化任务。 */
    @Override
    public void run(ApplicationArguments args) {
        for (GroupDO group : groupRepository.findAllOrdered()) {
            inventoryWorkflowBootstrapService.ensureDefaults(group.getId(), null);
        }
    }
}
