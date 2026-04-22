package com.boboboom.jxc.identity.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.domain.repository.DictionaryRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictFieldBindingDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictTypeDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictFieldBindingMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictItemMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.DictTypeMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

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
    private final JdbcTemplate jdbcTemplate;

    /**
     * 构造字典仓储实现。
     *
     * @param dictTypeMapper 字典类型 Mapper
     * @param dictItemMapper 字典项 Mapper
     * @param dictFieldBindingMapper 字段绑定 Mapper
     * @param jdbcTemplate JDBC 模板
     */
    public DictionaryRepositoryImpl(DictTypeMapper dictTypeMapper,
                                    DictItemMapper dictItemMapper,
                                    DictFieldBindingMapper dictFieldBindingMapper,
                                    JdbcTemplate jdbcTemplate) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
        this.dictFieldBindingMapper = dictFieldBindingMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DictTypeDO> findTypes() {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<DictTypeDO>()
                .orderByAsc(DictTypeDO::getSortNo)
                .orderByAsc(DictTypeDO::getId));
    }

    @Override
    public Optional<DictTypeDO> findTypeById(Long id) {
        return Optional.ofNullable(dictTypeMapper.selectById(id));
    }

    @Override
    public Optional<DictTypeDO> findTypeByCode(String dictCode) {
        return dictTypeMapper.selectList(new LambdaQueryWrapper<DictTypeDO>()
                        .eq(DictTypeDO::getDictCode, dictCode)
                        .orderByDesc(DictTypeDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void saveType(DictTypeDO dictType) {
        dictTypeMapper.insert(dictType);
    }

    @Override
    public void updateType(DictTypeDO dictType) {
        dictTypeMapper.updateById(dictType);
    }

    @Override
    public void deleteType(Long id) {
        dictTypeMapper.deleteById(id);
    }

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

    @Override
    public Optional<DictItemDO> findItemById(Long id) {
        return Optional.ofNullable(dictItemMapper.selectById(id));
    }

    @Override
    public Optional<DictItemDO> findItemByKey(Long dictTypeId, String itemKey) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemDO>()
                        .eq(DictItemDO::getDictTypeId, dictTypeId)
                        .eq(DictItemDO::getItemKey, itemKey)
                        .orderByDesc(DictItemDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public Optional<DictItemDO> findItemByCode(Long dictTypeId, String itemCode) {
        return dictItemMapper.selectList(new LambdaQueryWrapper<DictItemDO>()
                        .eq(DictItemDO::getDictTypeId, dictTypeId)
                        .eq(DictItemDO::getItemCode, itemCode)
                        .orderByDesc(DictItemDO::getId))
                .stream()
                .findFirst();
    }

    @Override
    public void saveItem(DictItemDO item) {
        dictItemMapper.insert(item);
    }

    @Override
    public void updateItem(DictItemDO item) {
        dictItemMapper.updateById(item);
    }

    @Override
    public void deleteItem(Long id) {
        dictItemMapper.deleteById(id);
    }

    @Override
    public List<DictFieldBindingDO> findBindingsByDictCode(String dictCode) {
        return dictFieldBindingMapper.selectList(new LambdaQueryWrapper<DictFieldBindingDO>()
                .eq(DictFieldBindingDO::getDictCode, dictCode)
                .orderByAsc(DictFieldBindingDO::getId));
    }

    @Override
    public long countBindingReferences(DictFieldBindingDO binding, String itemCode) {
        String tableName = checkedIdentifier(binding.getTableName());
        String columnName = checkedIdentifier(binding.getColumnName());
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM " + tableName + " WHERE " + columnName + " = ?",
                Long.class,
                itemCode
        );
        return count == null ? 0L : count;
    }

    @Override
    public void migrateBindingReferences(DictFieldBindingDO binding, String oldCode, String newCode) {
        String tableName = checkedIdentifier(binding.getTableName());
        String columnName = checkedIdentifier(binding.getColumnName());
        jdbcTemplate.update(
                "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + columnName + " = ?",
                newCode,
                oldCode
        );
    }

    private String checkedIdentifier(String identifier) {
        String value = identifier == null ? "" : identifier.trim();
        if (!SQL_IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException("字典字段绑定配置非法");
        }
        return value;
    }
}
