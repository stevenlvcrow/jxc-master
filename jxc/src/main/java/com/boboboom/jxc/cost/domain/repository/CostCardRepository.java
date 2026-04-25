package com.boboboom.jxc.cost.domain.repository;

import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardDetail;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardHeader;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardLine;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardVersion;

import java.util.List;
import java.util.Optional;

/**
 * 成本卡仓储接口，定义成本卡档案、版本和明细的数据访问能力。
 */
public interface CostCardRepository {

    /**
     * 查询作用域内成本卡列表。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @return 成本卡列表
     */
    List<CostCardHeader> findCards(String scopeType, Long scopeId);

    /**
     * 查询成本卡详情。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param id 成本卡 ID
     * @return 成本卡详情
     */
    Optional<CostCardDetail> findDetail(String scopeType, Long scopeId, Long id);

    /**
     * 统计指定前缀的成本卡数量。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param prefix 编码前缀
     * @return 数量
     */
    long countByCodePrefix(String scopeType, Long scopeId, String prefix);

    /**
     * 保存成本卡主档。
     *
     * @param header 主档
     * @return 主键
     */
    Long saveCard(CostCardHeader header);

    /**
     * 更新成本卡主档。
     *
     * @param header 主档
     */
    void updateCard(CostCardHeader header);

    /**
     * 保存成本卡版本。
     *
     * @param version 版本
     * @return 主键
     */
    Long saveVersion(CostCardVersion version);

    /**
     * 保存成本卡明细。
     *
     * @param versionId 版本 ID
     * @param lines 明细
     */
    void saveLines(Long versionId, List<CostCardLine> lines);

    /**
     * 启用指定版本。
     *
     * @param cardId 成本卡 ID
     * @param versionId 版本 ID
     */
    void activateVersion(Long cardId, Long versionId);

    /**
     * 停用成本卡。
     *
     * @param cardId 成本卡 ID
     */
    void disableCard(Long cardId);

    /**
     * 绑定菜品默认成本卡和扣减仓。
     *
     * @param scopeType 作用域类型
     * @param scopeId 作用域 ID
     * @param dishId 菜品 ID
     * @param cardId 成本卡 ID
     * @param defaultWarehouse 默认扣减仓
     * @param enabled 是否启用
     */
    void bindDish(String scopeType, Long scopeId, String dishId, Long cardId, String defaultWarehouse, boolean enabled);
}
