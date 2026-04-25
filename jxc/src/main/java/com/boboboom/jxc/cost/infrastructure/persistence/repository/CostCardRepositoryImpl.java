package com.boboboom.jxc.cost.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardDetail;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardHeader;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardLine;
import com.boboboom.jxc.cost.application.service.CostCardApplicationService.CostCardVersion;
import com.boboboom.jxc.cost.domain.repository.CostCardRepository;
import com.boboboom.jxc.cost.infrastructure.persistence.mapper.CostCardMapper;

/**
 * 成本卡仓储实现。
 */
@Repository
public class CostCardRepositoryImpl implements CostCardRepository {

    private final CostCardMapper costCardMapper;

    /**
     * 创建成本卡仓储实现。
     *
     * @param costCardMapperValue 成本卡持久化映射
     */
    public CostCardRepositoryImpl(CostCardMapper costCardMapperValue) {
        this.costCardMapper = costCardMapperValue;
    }

    @Override
    public List<CostCardHeader> findCards(String scopeType, Long scopeId) {
        return costCardMapper.selectCards(scopeType, scopeId);
    }

    @Override
    public Optional<CostCardDetail> findDetail(String scopeType, Long scopeId, Long id) {
        return costCardMapper.selectCardByScopeAndId(scopeType, scopeId, id)
                .stream()
                .findFirst()
                .map(header -> toDetail(header, costCardMapper.selectVersionsByCardId(header.id())));
    }

    @Override
    public long countByCodePrefix(String scopeType, Long scopeId, String prefix) {
        Long count = costCardMapper.countByCodePrefix(scopeType, scopeId, prefix + "%");
        return count == null ? 0L : count;
    }

    @Override
    public Long saveCard(CostCardHeader header) {
        return costCardMapper.insertCard(header);
    }

    @Override
    public void updateCard(CostCardHeader header) {
        costCardMapper.updateCard(header);
    }

    @Override
    public Long saveVersion(CostCardVersion version) {
        return costCardMapper.insertVersion(version);
    }

    @Override
    public void saveLines(Long versionId, List<CostCardLine> lines) {
        for (CostCardLine line : lines) {
            costCardMapper.insertLine(versionId, line);
        }
    }

    @Override
    public void activateVersion(Long cardId, Long versionId) {
        costCardMapper.activateVersionStatus(cardId, versionId);
        costCardMapper.activateCardVersion(cardId, versionId);
    }

    @Override
    public void disableCard(Long cardId) {
        costCardMapper.disableCard(cardId);
    }

    @Override
    public void bindDish(String scopeType, Long scopeId, String dishId, Long cardId, String defaultWarehouse, boolean enabled) {
        costCardMapper.upsertDishBinding(scopeType, scopeId, dishId, cardId, defaultWarehouse, enabled);
    }

    private CostCardDetail toDetail(CostCardHeader header, List<CostCardVersion> versions) {
        return new CostCardDetail(
                header.id(),
                header.scopeType(),
                header.scopeId(),
                header.cardCode(),
                header.cardName(),
                header.cardType(),
                header.status(),
                header.effectiveVersionId(),
                header.linkedDishCount(),
                header.remark(),
                header.createdBy(),
                versions
        );
    }
}
