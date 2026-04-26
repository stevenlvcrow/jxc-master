package com.boboboom.jxc.item.application.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.item.domain.repository.ItemStatisticsTypeRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemStatisticsTypeDO;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeBatchExportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.StatisticsTypeCreateRequest;

/** 物品统计类型业务服务，负责统计类型查询、创建和导出。 */
@Service
public class ItemStatisticsTypeApplicationService {

    private static final long MAX_PAGE_SIZE = 200L;

    private static final String CREATE_TYPE_SYSTEM_BUILTIN = "SYSTEM_BUILTIN";
    private static final String CREATE_TYPE_CUSTOM = "CUSTOM";
    private static final String CODE_PREFIX = "TJLX";

    private final ItemStatisticsTypeRepository itemStatisticsTypeRepository;
    private final OrgScopeService orgScopeService;
    private final BusinessCodeGenerator businessCodeGenerator;

    /** 物品统计类型业务服务，负责统计类型查询、创建和导出。 */
    public ItemStatisticsTypeApplicationService(ItemStatisticsTypeRepository itemStatisticsTypeRepositoryValue,
                                                 OrgScopeService orgScopeServiceValue,
                                                 BusinessCodeGenerator businessCodeGeneratorValue) {
        this.itemStatisticsTypeRepository = itemStatisticsTypeRepositoryValue;
        this.orgScopeService = orgScopeServiceValue;
        this.businessCodeGenerator = businessCodeGeneratorValue;
    }

    /** 分页查询业务列表。 */
    public PageResult<StatisticsTypeListItem> list(
            long pageNo,
            long pageSize,
            String keyword,
            String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        long normalizedPageNo = Math.max(pageNo, 1L);
        long normalizedPageSize = Math.max(1L, Math.min(pageSize, MAX_PAGE_SIZE));
        String normalizedKeyword = trimNullable(keyword);

        List<ItemStatisticsTypeDO> filtered = itemStatisticsTypeRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .filter(row -> normalizedKeyword == null
                        || contains(row.getCode(), normalizedKeyword)
                        || contains(row.getName(), normalizedKeyword)
                        || contains(row.getStatisticsCategory(), normalizedKeyword))
                .toList();
        long total = filtered.size();
        long baseIndex = (normalizedPageNo - 1) * normalizedPageSize;
        List<ItemStatisticsTypeDO> records = filtered.stream()
                .skip(baseIndex)
                .limit(normalizedPageSize)
                .toList();
        List<StatisticsTypeListItem> list = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            list.add(toListItem(records.get(i), baseIndex + i + 1));
        }
        return new PageResult<>(
                list,
                total,
                normalizedPageNo,
                normalizedPageSize
        );
    }

    /** 查询业务详情。 */
    public StatisticsTypeDetailItem detail(Long id,
                                           String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        ItemStatisticsTypeDO row = requireById(id, scope);
        return new StatisticsTypeDetailItem(
                row.getId(),
                row.getCode(),
                row.getName(),
                row.getStatisticsCategory(),
                toCreateTypeLabel(row.getCreateType()),
                toModifiedTimeLabel(row),
                row.getCreatedAt(),
                row.getUpdatedAt()
        );
    }

    /** 创建业务记录。 */
    @Transactional
    public CreateResult create(String orgId,
                               StatisticsTypeCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String name = trim(request.getName());
        String statisticsCategory = trim(request.getStatisticsCategory());

        boolean duplicated = itemStatisticsTypeRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getName(), name)
                        && Objects.equals(row.getStatisticsCategory(), statisticsCategory));
        if (duplicated) {
            throw new BusinessException("统计类型名称已存在");
        }

        ItemStatisticsTypeDO row = new ItemStatisticsTypeDO();
        row.setScopeType(scope.scopeType());
        row.setScopeId(scope.scopeId());
        row.setCode(nextCode(scope));
        row.setName(name);
        row.setStatisticsCategory(statisticsCategory);
        row.setCreateType(CREATE_TYPE_CUSTOM);
        itemStatisticsTypeRepository.save(row);

        return new CreateResult(row.getId(), row.getCode());
    }

    /** 批量导出业务数据。 */
    public BatchExportResult batchExport(String orgId,
                                         StatisticsTypeBatchExportRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        List<Long> ids = request == null ? List.of() : request.getIds();
        List<ItemStatisticsTypeDO> rows;
        if (ids == null || ids.isEmpty()) {
            rows = itemStatisticsTypeRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        } else {
            rows = itemStatisticsTypeRepository.findByScopeAndIds(scope.scopeType(), scope.scopeId(), ids).stream()
                    .sorted((left, right) -> {
                        int codeCompare = nullSafeString(right.getCode()).compareTo(nullSafeString(left.getCode()));
                        if (codeCompare != 0) {
                            return codeCompare;
                        }
                        return Long.compare(right.getId(), left.getId());
                    })
                    .toList();
        }

        List<StatisticsTypeExportItem> exportedRows = rows.stream()
                .map(row -> new StatisticsTypeExportItem(
                        row.getId(),
                        row.getCode(),
                        row.getName(),
                        row.getStatisticsCategory(),
                        toCreateTypeLabel(row.getCreateType()),
                        toModifiedTimeLabel(row)
                ))
                .toList();

        String fileName = "statistics-types-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".csv";
        return new BatchExportResult(fileName, exportedRows);
    }

    private ItemStatisticsTypeDO requireById(Long id, ItemScope scope) {
        ItemStatisticsTypeDO row = itemStatisticsTypeRepository.findById(id)
                .filter(value -> Objects.equals(value.getScopeType(), scope.scopeType()))
                .filter(value -> Objects.equals(value.getScopeId(), scope.scopeId()))
                .orElse(null);
        if (row == null) {
            throw new BusinessException("统计类型不存在");
        }
        return row;
    }

    private StatisticsTypeListItem toListItem(ItemStatisticsTypeDO row, long index) {
        return new StatisticsTypeListItem(
                row.getId(),
                index,
                row.getCode(),
                row.getName(),
                row.getStatisticsCategory(),
                toCreateTypeLabel(row.getCreateType()),
                toModifiedTimeLabel(row)
        );
    }

    private String toCreateTypeLabel(String createType) {
        if (CREATE_TYPE_SYSTEM_BUILTIN.equals(createType)) {
            return "系统内置";
        }
        return "自定义";
    }

    private String toModifiedTimeLabel(ItemStatisticsTypeDO row) {
        if (CREATE_TYPE_SYSTEM_BUILTIN.equals(row.getCreateType())) {
            return "- -";
        }
        if (row.getUpdatedAt() == null) {
            return "- -";
        }
        return row.getUpdatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT));
    }

    private String nextCode(ItemScope scope) {
        List<String> existingCodes = itemStatisticsTypeRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(ItemStatisticsTypeDO::getCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(CODE_PREFIX, existingCodes);
    }

    private String trim(String value) {
        String trimmed = trimNullable(value);
        if (trimmed == null) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    private String trimNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullSafeString(String value) {
        return value == null ? "" : value;
    }

    private ItemScope resolveItemScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolvePlatformOrStoreScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new ItemScope(scope.scopeType(), scope.scopeId());
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    /** 物品与供应商结果模型，承载业务处理结果。 */
    public record PageResult<T>(List<T> list, long total, long pageNo, long pageSize) {
    }

    /** 物品与供应商明细项模型，承载子表或批量操作明细。 */
    public record StatisticsTypeListItem(Long id,
                                         long index,
                                         String code,
                                         String name,
                                         String statisticsCategory,
                                         String createType,
                                         String modifiedTime) {
    }

    /** 物品与供应商明细项模型，承载子表或批量操作明细。 */
    public record StatisticsTypeDetailItem(Long id,
                                           String code,
                                           String name,
                                           String statisticsCategory,
                                           String createType,
                                           String modifiedTime,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
    }

    /** 物品与供应商结果模型，承载业务处理结果。 */
    public record CreateResult(Long id, String code) {
    }

    /** 物品与供应商明细项模型，承载子表或批量操作明细。 */
    public record StatisticsTypeExportItem(Long id,
                                           String code,
                                           String name,
                                           String statisticsCategory,
                                           String createType,
                                           String modifiedTime) {
    }

    /** 物品与供应商结果模型，承载业务处理结果。 */
    public record BatchExportResult(String fileName, List<StatisticsTypeExportItem> rows) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}
