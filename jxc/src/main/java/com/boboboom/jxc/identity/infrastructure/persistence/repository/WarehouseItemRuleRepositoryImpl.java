package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.domain.repository.WarehouseItemRuleRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseItemRuleCategoryDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseItemRuleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseItemRuleItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.WarehouseItemRuleWarehouseDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseItemRuleCategoryMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseItemRuleItemMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseItemRuleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseItemRuleWarehouseMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.WarehouseMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
public class WarehouseItemRuleRepositoryImpl implements WarehouseItemRuleRepository {

    private final WarehouseItemRuleMapper warehouseItemRuleMapper;
    private final WarehouseItemRuleItemMapper ruleItemMapper;
    private final WarehouseItemRuleCategoryMapper ruleCategoryMapper;
    private final WarehouseItemRuleWarehouseMapper ruleWarehouseMapper;
    private final WarehouseMapper warehouseMapper;

    public WarehouseItemRuleRepositoryImpl(WarehouseItemRuleMapper warehouseItemRuleMapper,
                                           WarehouseItemRuleItemMapper ruleItemMapper,
                                           WarehouseItemRuleCategoryMapper ruleCategoryMapper,
                                           WarehouseItemRuleWarehouseMapper ruleWarehouseMapper,
                                           WarehouseMapper warehouseMapper) {
        this.warehouseItemRuleMapper = warehouseItemRuleMapper;
        this.ruleItemMapper = ruleItemMapper;
        this.ruleCategoryMapper = ruleCategoryMapper;
        this.ruleWarehouseMapper = ruleWarehouseMapper;
        this.warehouseMapper = warehouseMapper;
    }

    @Override
    public List<RuleSummary> findRuleSummariesByGroupId(Long groupId) {
        return warehouseItemRuleMapper.selectList(new LambdaQueryWrapper<WarehouseItemRuleDO>()
                        .eq(WarehouseItemRuleDO::getGroupId, groupId)
                        .orderByDesc(WarehouseItemRuleDO::getCreatedAt))
                .stream()
                .map(this::toRuleSummary)
                .toList();
    }

    @Override
    public Optional<RuleRecord> findRuleById(Long ruleId) {
        return Optional.ofNullable(warehouseItemRuleMapper.selectById(ruleId)).map(this::toRuleRecord);
    }

    @Override
    public RuleDetailData findRuleDetailById(Long ruleId) {
        RuleRecord rule = findRuleById(ruleId).orElse(null);
        if (rule == null) {
            return null;
        }
        List<RuleItemData> items = ruleItemMapper.selectList(new LambdaQueryWrapper<WarehouseItemRuleItemDO>()
                        .eq(WarehouseItemRuleItemDO::getRuleId, ruleId)
                        .orderByAsc(WarehouseItemRuleItemDO::getSortOrder))
                .stream()
                .map(i -> new RuleItemData(
                        i.getId(),
                        i.getItemCode(),
                        i.getItemName(),
                        i.getSpecModel(),
                        i.getItemCategory()
                ))
                .toList();
        List<RuleCategoryData> categories = ruleCategoryMapper.selectList(new LambdaQueryWrapper<WarehouseItemRuleCategoryDO>()
                        .eq(WarehouseItemRuleCategoryDO::getRuleId, ruleId)
                        .orderByAsc(WarehouseItemRuleCategoryDO::getSortOrder))
                .stream()
                .map(c -> new RuleCategoryData(
                        c.getId(),
                        c.getCategoryCode(),
                        c.getCategoryName(),
                        c.getParentCategory(),
                        c.getChildCategory()
                ))
                .toList();
        List<RuleWarehouseData> warehouses = ruleWarehouseMapper.selectList(new LambdaQueryWrapper<WarehouseItemRuleWarehouseDO>()
                        .eq(WarehouseItemRuleWarehouseDO::getRuleId, ruleId)
                        .orderByAsc(WarehouseItemRuleWarehouseDO::getSortOrder))
                .stream()
                .map(w -> new RuleWarehouseData(
                        w.getId(),
                        w.getWarehouseId(),
                        w.getWarehouseName()
                ))
                .toList();
        return new RuleDetailData(rule, items, categories, warehouses);
    }

    @Override
    public Long saveRule(RuleRecord rule) {
        WarehouseItemRuleDO data = toDataObject(rule);
        if (data.getId() == null) {
            warehouseItemRuleMapper.insert(data);
        } else {
            warehouseItemRuleMapper.updateById(data);
        }
        return data.getId();
    }

    @Override
    public void deleteRuleById(Long ruleId) {
        warehouseItemRuleMapper.deleteById(ruleId);
    }

    @Override
    public void replaceRuleDetails(Long ruleId,
                                   List<RuleItemData> items,
                                   List<RuleCategoryData> categories,
                                   List<RuleWarehouseData> warehouses) {
        ruleItemMapper.delete(new LambdaQueryWrapper<WarehouseItemRuleItemDO>().eq(WarehouseItemRuleItemDO::getRuleId, ruleId));
        ruleCategoryMapper.delete(new LambdaQueryWrapper<WarehouseItemRuleCategoryDO>().eq(WarehouseItemRuleCategoryDO::getRuleId, ruleId));
        ruleWarehouseMapper.delete(new LambdaQueryWrapper<WarehouseItemRuleWarehouseDO>().eq(WarehouseItemRuleWarehouseDO::getRuleId, ruleId));

        int idx = 0;
        if (items != null) {
            for (RuleItemData item : items) {
                WarehouseItemRuleItemDO row = new WarehouseItemRuleItemDO();
                row.setRuleId(ruleId);
                row.setItemCode(item.itemCode());
                row.setItemName(item.itemName());
                row.setSpecModel(item.specModel());
                row.setItemCategory(item.itemCategory());
                row.setSortOrder(idx++);
                ruleItemMapper.insert(row);
            }
        }

        idx = 0;
        if (categories != null) {
            for (RuleCategoryData category : categories) {
                WarehouseItemRuleCategoryDO row = new WarehouseItemRuleCategoryDO();
                row.setRuleId(ruleId);
                row.setCategoryCode(category.categoryCode());
                row.setCategoryName(category.categoryName());
                row.setParentCategory(category.parentCategory());
                row.setChildCategory(category.childCategory());
                row.setSortOrder(idx++);
                ruleCategoryMapper.insert(row);
            }
        }

        idx = 0;
        if (warehouses != null) {
            for (RuleWarehouseData warehouse : warehouses) {
                WarehouseItemRuleWarehouseDO row = new WarehouseItemRuleWarehouseDO();
                row.setRuleId(ruleId);
                row.setWarehouseId(warehouse.warehouseId());
                row.setWarehouseName(resolveWarehouseName(warehouse.warehouseId()));
                row.setSortOrder(idx++);
                ruleWarehouseMapper.insert(row);
            }
        }
    }

    @Override
    public List<String> findAllRuleCodes() {
        return warehouseItemRuleMapper.selectList(new LambdaQueryWrapper<WarehouseItemRuleDO>()
                        .select(WarehouseItemRuleDO::getRuleCode))
                .stream()
                .map(WarehouseItemRuleDO::getRuleCode)
                .filter(Objects::nonNull)
                .toList();
    }

    private RuleSummary toRuleSummary(WarehouseItemRuleDO rule) {
        return new RuleSummary(
                rule.getId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                bool(rule.getBusinessControl()),
                rule.getStatus(),
                rule.getCreatedBy(),
                rule.getCreatedAt(),
                rule.getUpdatedBy(),
                rule.getUpdatedAt()
        );
    }

    private RuleRecord toRuleRecord(WarehouseItemRuleDO rule) {
        return new RuleRecord(
                rule.getId(),
                rule.getGroupId(),
                rule.getRuleCode(),
                rule.getRuleName(),
                bool(rule.getBusinessControl()),
                bool(rule.getControlOrder()),
                bool(rule.getControlPurchaseInbound()),
                bool(rule.getControlTransferInbound()),
                rule.getStatus(),
                rule.getCreatedBy(),
                rule.getCreatedAt(),
                rule.getUpdatedBy(),
                rule.getUpdatedAt()
        );
    }

    private WarehouseItemRuleDO toDataObject(RuleRecord rule) {
        WarehouseItemRuleDO data = new WarehouseItemRuleDO();
        data.setId(rule.id());
        data.setGroupId(rule.groupId());
        data.setRuleCode(rule.ruleCode());
        data.setRuleName(rule.ruleName());
        data.setBusinessControl(rule.businessControl());
        data.setControlOrder(rule.controlOrder());
        data.setControlPurchaseInbound(rule.controlPurchaseInbound());
        data.setControlTransferInbound(rule.controlTransferInbound());
        data.setStatus(rule.status());
        data.setCreatedBy(rule.createdBy());
        data.setCreatedAt(rule.createdAt());
        data.setUpdatedBy(rule.updatedBy());
        data.setUpdatedAt(rule.updatedAt());
        return data;
    }

    private String resolveWarehouseName(Long warehouseId) {
        if (warehouseId == null) {
            return null;
        }
        WarehouseDO warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null ? warehouse.getWarehouseName() : null;
    }

    private boolean bool(Boolean value) {
        return value != null && value;
    }
}
