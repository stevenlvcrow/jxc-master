package com.boboboom.jxc.cost.infrastructure.persistence.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardHeader;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardLine;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardVersion;

/**
 * 成本卡持久化映射。
 */
public interface CostCardMapper {

    List<CostCardHeader> selectCards(@Param("scopeType") String scopeType, @Param("scopeId") Long scopeId);

    List<CostCardHeader> selectCardByScopeAndId(@Param("scopeType") String scopeType,
                                                @Param("scopeId") Long scopeId,
                                                @Param("id") Long id);

    List<CostCardVersion> selectVersionsByCardId(@Param("cardId") Long cardId);

    Long countByCodePrefix(@Param("scopeType") String scopeType, @Param("scopeId") Long scopeId, @Param("prefix") String prefix);

    Long insertCard(@Param("header") CostCardHeader header);

    void updateCard(@Param("header") CostCardHeader header);

    Long insertVersion(@Param("version") CostCardVersion version);

    void insertLine(@Param("versionId") Long versionId, @Param("line") CostCardLine line);

    void activateVersionStatus(@Param("cardId") Long cardId, @Param("versionId") Long versionId);

    void activateCardVersion(@Param("cardId") Long cardId, @Param("versionId") Long versionId);

    void disableCard(@Param("cardId") Long cardId);

    void upsertDishBinding(@Param("scopeType") String scopeType,
                           @Param("scopeId") Long scopeId,
                           @Param("dishId") String dishId,
                           @Param("cardId") Long cardId,
                           @Param("defaultWarehouse") String defaultWarehouse,
                           @Param("enabled") boolean enabled);
}
