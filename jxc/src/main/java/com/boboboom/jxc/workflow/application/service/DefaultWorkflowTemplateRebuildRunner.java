package com.boboboom.jxc.workflow.application.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.boboboom.jxc.identity.domain.repository.GroupRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;

/**
 * 默认审批流程模板手动重建入口。
 */
@Component
public class DefaultWorkflowTemplateRebuildRunner implements ApplicationRunner {

    private static final String REBUILD_OPTION = "jxc.workflow.rebuild-default-templates";

    private final GroupRepository groupRepository;
    private final InventoryWorkflowBootstrapService inventoryWorkflowBootstrapService;

    /** 默认审批流程模板手动重建入口。 */
    public DefaultWorkflowTemplateRebuildRunner(GroupRepository groupRepositoryValue,
                                                InventoryWorkflowBootstrapService inventoryWorkflowBootstrapServiceValue) {
        this.groupRepository = groupRepositoryValue;
        this.inventoryWorkflowBootstrapService = inventoryWorkflowBootstrapServiceValue;
    }

    /**
     * 按显式启动参数删除并重建所有集团默认审批流程模板。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        if (!isRebuildEnabled(args)) {
            return;
        }
        List<GroupDO> groups = groupRepository.findAllOrdered();
        for (GroupDO group : groups) {
            inventoryWorkflowBootstrapService.rebuildDefaults(group.getId(), null);
        }
    }

    private boolean isRebuildEnabled(ApplicationArguments args) {
        if (args == null || !args.containsOption(REBUILD_OPTION)) {
            return false;
        }
        List<String> values = args.getOptionValues(REBUILD_OPTION);
        if (values == null || values.isEmpty()) {
            return true;
        }
        return values.stream().anyMatch("true"::equalsIgnoreCase);
    }
}
