package com.boboboom.jxc.identity.domain.repository;

import java.util.List;
import java.util.Optional;

import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictFieldBindingDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictItemDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.DictTypeDO;

/**
 * 字典仓储接口。
 */
public interface DictionaryRepository {

    /**
     * 查询全部字典类型。
     *
     * @return 字典类型列表
     */
    List<DictTypeDO> findTypes();

    /**
     * 按主键查询字典类型。
     *
     * @param id 字典类型主键
     * @return 字典类型
     */
    Optional<DictTypeDO> findTypeById(Long id);

    /**
     * 按编码查询字典类型。
     *
     * @param dictCode 字典编码
     * @return 字典类型
     */
    Optional<DictTypeDO> findTypeByCode(String dictCode);

    /**
     * 保存字典类型。
     *
     * @param dictType 字典类型
     */
    void saveType(DictTypeDO dictType);

    /**
     * 更新字典类型。
     *
     * @param dictType 字典类型
     */
    void updateType(DictTypeDO dictType);

    /**
     * 删除字典类型。
     *
     * @param id 字典类型主键
     */
    void deleteType(Long id);

    /**
     * 查询指定字典类型下的字典项。
     *
     * @param dictTypeId 字典类型主键
     * @param enabledOnly 是否只查启用项
     * @return 字典项列表
     */
    List<DictItemDO> findItemsByTypeId(Long dictTypeId, boolean enabledOnly);

    /**
     * 按主键查询字典项。
     *
     * @param id 字典项主键
     * @return 字典项
     */
    Optional<DictItemDO> findItemById(Long id);

    /**
     * 按稳定键查询字典项。
     *
     * @param dictTypeId 字典类型主键
     * @param itemKey 稳定项键
     * @return 字典项
     */
    Optional<DictItemDO> findItemByKey(Long dictTypeId, String itemKey);

    /**
     * 按存储值查询字典项。
     *
     * @param dictTypeId 字典类型主键
     * @param itemCode 存储值
     * @return 字典项
     */
    Optional<DictItemDO> findItemByCode(Long dictTypeId, String itemCode);

    /**
     * 保存字典项。
     *
     * @param item 字典项
     */
    void saveItem(DictItemDO item);

    /**
     * 更新字典项。
     *
     * @param item 字典项
     */
    void updateItem(DictItemDO item);

    /**
     * 删除字典项。
     *
     * @param id 字典项主键
     */
    void deleteItem(Long id);

    /**
     * 查询字典字段绑定。
     *
     * @param dictCode 字典编码
     * @return 字段绑定列表
     */
    List<DictFieldBindingDO> findBindingsByDictCode(String dictCode);

    /**
     * 统计绑定字段中使用某个字典存储值的数据量。
     *
     * @param binding 字段绑定
     * @param itemCode 字典存储值
     * @return 引用数量
     */
    long countBindingReferences(DictFieldBindingDO binding, String itemCode);

    /**
     * 将绑定字段中的旧字典存储值迁移为新值。
     *
     * @param binding 字段绑定
     * @param oldCode 原存储值
     * @param newCode 新存储值
     */
    void migrateBindingReferences(DictFieldBindingDO binding, String oldCode, String newCode);
}
