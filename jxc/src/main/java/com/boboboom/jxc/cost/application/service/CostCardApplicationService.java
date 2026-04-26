package com.boboboom.jxc.cost.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.cost.domain.repository.CostCardRepository;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.interfaces.rest.response.PageData;

/**
 * 成本卡业务服务，负责成本卡版本、明细和菜品绑定的业务校验。
 */
@Service
public class CostCardApplicationService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    private final CostCardRepository costCardRepository;
    private final OrgScopeService orgScopeService;

    /** 成本卡业务服务，负责菜品成本卡版本、明细和绑定关系维护。 */
    public CostCardApplicationService(CostCardRepository costCardRepositoryValue, OrgScopeService orgScopeServiceValue) {
        this.costCardRepository = costCardRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
    }

    /**
     * 分页查询成本卡。
     *
     * @param pageNo 页码
     * @param pageSize 每页条数
     * @param keyword 关键字
     * @param status 状态
     * @param orgId 机构标识
     * @return 分页结果
     */
    public PageData<CostCardRow> page(Integer pageNo, Integer pageSize, String keyword, String status, String orgId) {
        CostScope scope = resolveScope(orgId);
        String keywordValue = toLower(trimNullable(keyword));
        String statusValue = trimNullable(status);
        List<CostCardRow> filtered = costCardRepository.findCards(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> !StringUtils.hasText(keywordValue)
                        || toLower(row.cardCode()).contains(keywordValue)
                        || toLower(row.cardName()).contains(keywordValue))
                .filter(row -> !StringUtils.hasText(statusValue) || Objects.equals(row.status(), statusValue))
                .map(row -> new CostCardRow(
                        row.id(),
                        row.cardCode(),
                        row.cardName(),
                        row.cardType(),
                        row.status(),
                        row.effectiveVersionId(),
                        row.linkedDishCount(),
                        row.remark()
                ))
                .toList();
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNo - 1) * safePageSize, filtered.size());
        int toIndex = Math.min(fromIndex + safePageSize, filtered.size());
        return new PageData<>(filtered.subList(fromIndex, toIndex), filtered.size(), safePageNo, safePageSize);
    }

    /**
     * 查询成本卡详情。
     *
     * @param id 成本卡 ID
     * @param orgId 机构标识
     * @return 成本卡详情
     */
    public CostCardDetail detail(Long id, String orgId) {
        CostScope scope = resolveScope(orgId);
        return costCardRepository.findDetail(scope.scopeType(), scope.scopeId(), id)
                .orElseThrow(() -> new BusinessException("成本卡不存在或无权查看"));
    }

    /**
     * 创建成本卡和初始版本。
     *
     * @param request 请求
     * @param orgId 机构标识
     * @return 创建结果
     */
    @Transactional
    public IdPayload create(CostCardSaveRequest request, String orgId) {
        CostScope scope = resolveScope(orgId);
        validateLines(request.lines());
        String prefix = "CC-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        long next = costCardRepository.countByCodePrefix(scope.scopeType(), scope.scopeId(), prefix) + 1;
        CostCardHeader header = new CostCardHeader(
                null,
                scope.scopeType(),
                scope.scopeId(),
                prefix + String.format(Locale.ROOT, "%03d", next),
                requiredTrim(request.cardName(), "成本卡名称不能为空"),
                requiredTrim(request.cardType(), "成本卡类型不能为空"),
                "DISABLED",
                null,
                0,
                trimNullable(request.remark()),
                AuthContextHolder.requireUserId("登录已失效，请重新登录")
        );
        Long cardId = costCardRepository.saveCard(header);
        Long versionId = costCardRepository.saveVersion(new CostCardVersion(
                null,
                cardId,
                1,
                parseRequiredDate(request.effectiveDate()),
                "DRAFT",
                false,
                trimNullable(request.remark())
        ));
        costCardRepository.saveLines(versionId, request.lines());
        return new IdPayload(cardId, header.cardCode());
    }

    /**
     * 更新未启用成本卡基础信息并新增草稿版本。
     *
     * @param id 成本卡 ID
     * @param request 请求
     * @param orgId 机构标识
     */
    @Transactional
    public void update(Long id, CostCardSaveRequest request, String orgId) {
        CostCardDetail detail = detail(id, orgId);
        if (detail.effectiveVersionId() != null) {
            throw new BusinessException("已启用成本卡不能覆盖修改，请新增版本");
        }
        validateLines(request.lines());
        CostCardHeader updated = new CostCardHeader(
                detail.id(),
                detail.scopeType(),
                detail.scopeId(),
                detail.cardCode(),
                requiredTrim(request.cardName(), "成本卡名称不能为空"),
                requiredTrim(request.cardType(), "成本卡类型不能为空"),
                detail.status(),
                detail.effectiveVersionId(),
                detail.linkedDishCount(),
                trimNullable(request.remark()),
                detail.createdBy()
        );
        costCardRepository.updateCard(updated);
        Long versionId = costCardRepository.saveVersion(new CostCardVersion(
                null,
                detail.id(),
                detail.versions().size() + 1,
                parseRequiredDate(request.effectiveDate()),
                "DRAFT",
                false,
                trimNullable(request.remark())
        ));
        costCardRepository.saveLines(versionId, request.lines());
    }

    /**
     * 启用指定成本卡版本。
     *
     * @param cardId 成本卡 ID
     * @param versionId 版本 ID
     * @param orgId 机构标识
     */
    @Transactional
    public void activateVersion(Long cardId, Long versionId, String orgId) {
        CostCardDetail detail = detail(cardId, orgId);
        boolean exists = detail.versions().stream().anyMatch(item -> Objects.equals(item.id(), versionId));
        if (!exists) {
            throw new BusinessException("成本卡版本不存在");
        }
        costCardRepository.activateVersion(cardId, versionId);
    }

    /**
     * 停用成本卡。
     *
     * @param cardId 成本卡 ID
     * @param orgId 机构标识
     */
    @Transactional
    public void disable(Long cardId, String orgId) {
        detail(cardId, orgId);
        costCardRepository.disableCard(cardId);
    }

    /**
     * 绑定菜品成本卡和默认扣减仓。
     *
     * @param cardId 成本卡 ID
     * @param request 请求
     * @param orgId 机构标识
     */
    @Transactional
    public void bindDish(Long cardId, DishCostCardBindingRequest request, String orgId) {
        CostScope scope = resolveScope(orgId);
        detail(cardId, orgId);
        String dishId = requiredTrim(request.dishId(), "菜品不能为空");
        String warehouse = requiredTrim(request.defaultWarehouse(), "默认扣减仓不能为空");
        costCardRepository.bindDish(scope.scopeType(), scope.scopeId(), dishId, cardId, warehouse, request.enabled());
    }

    private CostScope resolveScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(
                AuthContextHolder.requireUserId("登录已失效，请重新登录"),
                orgId
        );
        return new CostScope(scope.scopeType(), scope.scopeId());
    }

    private void validateLines(List<CostCardLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException("成本卡原料明细不能为空");
        }
        for (CostCardLine line : lines) {
            requiredTrim(line.itemCode(), "原料物品不能为空");
            requiredTrim(line.itemName(), "原料名称不能为空");
            requiredTrim(line.unitName(), "消耗单位不能为空");
            requiredTrim(line.defaultWarehouse(), "默认扣减仓不能为空");
            if (line.quantity() == null || line.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("原料用量必须大于0");
            }
            if (line.conversionRate() == null || line.conversionRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("单位换算率必须大于0");
            }
        }
    }

    private LocalDate parseRequiredDate(String value) {
        String normalized = requiredTrim(value, "生效日期不能为空");
        return LocalDate.parse(normalized);
    }

    private String requiredTrim(String value, String message) {
        String normalized = trimNullable(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private String trimNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    /** 成本卡载荷模型，承载接口返回的关键标识。 */
    public record IdPayload(Long id, String cardCode) {
    }

    /** 成本卡行数据模型，承载列表或报表明细。 */
    public record CostCardRow(Long id, String cardCode, String cardName, String cardType, String status,
                              Long effectiveVersionId, Integer linkedDishCount, String remark) {
    }

    /** 成本卡数据模型，承载成本卡单头数据。 */
    public record CostCardHeader(Long id, String scopeType, Long scopeId, String cardCode, String cardName,
                                 String cardType, String status, Long effectiveVersionId, Integer linkedDishCount,
                                 String remark, Long createdBy) {
    }

    /** 成本卡数据模型，承载成本卡明细数据。 */
    public record CostCardDetail(Long id, String scopeType, Long scopeId, String cardCode, String cardName,
                                 String cardType, String status, Long effectiveVersionId, Integer linkedDishCount,
                                 String remark, Long createdBy, List<CostCardVersion> versions) {
    }

    /** 成本卡数据模型，承载成本卡版本数据。 */
    public record CostCardVersion(Long id, Long cardId, Integer versionNo, LocalDate effectiveDate,
                                  String versionStatus, Boolean referenced, String remark) {
    }

    /** 成本卡数据模型，承载成本卡明细行数据。 */
    public record CostCardLine(String itemCode, String itemName, String unitName, BigDecimal quantity,
                               BigDecimal conversionRate, BigDecimal lossRate, String defaultWarehouse,
                               String remark) {
    }

    /** 成本卡请求参数，承载接口入参。 */
    public record CostCardSaveRequest(String cardName, String cardType, String effectiveDate, String remark,
                                      List<CostCardLine> lines) {
    }

    /** 成本卡请求参数，承载接口入参。 */
    public record DishCostCardBindingRequest(String dishId, String defaultWarehouse, boolean enabled) {
    }

    private record CostScope(String scopeType, Long scopeId) {
    }
}
