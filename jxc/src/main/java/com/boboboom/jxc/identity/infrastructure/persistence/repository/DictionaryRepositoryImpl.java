package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.DictionaryRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictFieldBindingDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictTypeDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictFieldBindingMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictItemMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictTypeMapper;

/**
 * 字典仓储实现。
 */
@Repository
public class DictionaryRepositoryImpl implements DictionaryRepository {

    private static final String STATUS_ENABLED = "ENABLED";
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]*");

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;
    private final DictFieldBindingMapper dictFieldBindingMapper;

    /**
     * 构造字典仓储实现。
     *
     * @param dictTypeMapperValue 字典类型 Mapper
     * @param dictItemMapperValue 字典项 Mapper
     * @param dictFieldBindingMapperValue 字段绑定 Mapper
     */
    public DictionaryRepositoryImpl(DictTypeMapper dictTypeMapperValue,
                                    DictItemMapper dictItemMapperValue,
                                    DictFieldBindingMapper dictFieldBindingMapperValue) {
        this.dictTypeMapper = dictTypeMapperValue;
        this.dictItemMapper = dictItemMapperValue;
        this.dictFieldBindingMapper = dictFieldBindingMapperValue;
    }

    /** 查询Types。 */
    @Override
    public List<DictTypeDO> findTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<DictTypeDO>()
                .orderByAsc(DictTypeDO::getSortNo)
                .orderByAsc(DictTypeDO::getId));
    }

    /** 查询类型By标识。 */
    @Override
    public Optional<DictTypeDO> findTypeById(Long id) {
        return Optional.ofNullable(dictTypeMapper.selectById(id));
    }

    /** 查询类型By编码。 */
    @Override
    public Optional<DictTypeDO> findTypeByCode(String dictCode) {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<DictTypeDO>()
                        .eq(DictTypeDO::getDictCode, dictCode)
                        .orderByDesc(DictTypeDO::getId))
                .stream()
                .findFirst();
    }

    /** 处理save类型。 */
    @Override
    public void saveType(DictTypeDO dictType) {
        dictTypeMapper.insert(dictType);
    }

    /** 更新类型。 */
    @Override
    public void updateType(DictTypeDO dictType) {
        dictTypeMapper.updateById(dictType);
    }

    /** 删除类型。 */
    @Override
    public void deleteType(Long id) {
        dictTypeMapper.deleteById(id);
    }

    /** 查询ItemsBy类型标识。 */
    @Override
    public List<DictItemDO> findItemsByTypeId(Long dictTypeId, boolean enabledOnly) {
        LambdaQueryWrapper<DictItemDO> wrapper = new LambdaQueryWrapper<DictItemDO>()
                .eq(DictItemDO::getDictTypeId, dictTypeId)
                .orderByAsc(DictItemDO::getSortNo)
                .orderByAsc(DictItemDO::getId);
        if (enabledOnly) {
            wrapper.eq(DictItemDO::getStatus, STATUS_ENABLED);
        }
        return dictItemMapper.selectList(wrapper);
    }

    /** 查询物品By标识。 */
    @Override
    public Optional<DictItemDO> findItemById(Long id) {
        return Optional.ofNullable(dictItemMapper.selectById(id));
    }

    /** 查询物品ByKey。 */
    @Override
    public Optional<DictItemDO> findItemByKey(Long dictTypeId, String itemKey) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemDO>()
                        .eq(DictItemDO::getDictTypeId, dictTypeId)
                        .eq(DictItemDO::getItemKey, itemKey)
                        .orderByDesc(DictItemDO::getId))
                .stream()
                .findFirst();
    }

    /** 查询物品By编码。 */
    @Override
    public Optional<DictItemDO> findItemByCode(Long dictTypeId, String itemCode) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemDO>()
                        .eq(DictItemDO::getDictTypeId, dictTypeId)
                        .eq(DictItemDO::getItemCode, itemCode)
                        .orderByDesc(DictItemDO::getId))
                .stream()
                .findFirst();
    }

    /** 身份与权限明细项模型，承载子表或批量操作明细。 */
    @Override
    public void saveItem(DictItemDO item) {
        dictItemMapper.insert(item);
    }

    /** 身份与权限明细项模型，承载子表或批量操作明细。 */
    @Override
    public void updateItem(DictItemDO item) {
        dictItemMapper.updateById(item);
    }

    /** 身份与权限明细项模型，承载子表或批量操作明细。 */
    @Override
    public void deleteItem(Long id) {
        dictItemMapper.deleteById(id);
    }

    /** 查询绑定关系ByDict编码。 */
    @Override
    public List<DictFieldBindingDO> findBindingsByDictCode(String dictCode) {
        return dictFieldBindingMapper.selectList(new LambdaQueryWrapper<DictFieldBindingDO>()
                .eq(DictFieldBindingDO::getDictCode, dictCode)
                .orderByAsc(DictFieldBindingDO::getId));
    }

    /** 统计绑定关系References数量。 */
    @Override
    public long countBindingReferences(DictFieldBindingDO binding, String itemCode) {
        String tableName = checkedIdentifier(binding.getTableName());
        String columnName = checkedIdentifier(binding.getColumnName());
        Long count = dictFieldBindingMapper.countReferences(tableName, columnName, itemCode);
        return count == null ? 0L : count;
    }

    /** 处理migrate绑定关系References。 */
    @Override
    public void migrateBindingReferences(DictFieldBindingDO binding, String oldCode, String newCode) {
        String tableName = checkedIdentifier(binding.getTableName());
        String columnName = checkedIdentifier(binding.getColumnName());
        dictFieldBindingMapper.migrateReferences(tableName, columnName, oldCode, newCode);
    }

    private String checkedIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (!SQL_IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException("字典字段绑定配置非法");
        }
        return value;
    }
}
