package com.boboboom.jxc.item.application.service;

import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.domain.repository.ItemTagRepository;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.ItemTagDO;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagBatchImportRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagCreateRequest;
import com.boboboom.jxc.item.interfaces.rest.request.ItemTagUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class ItemTagApplicationService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
    private static final String STATUS_ENABLED = "启用";
    private static final String TAG_CODE_PREFIX = "BQBM";

    private final ItemTagRepository itemTagRepository;
    private final OrgScopeService orgScopeService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public ItemTagApplicationService(ItemTagRepository itemTagRepository,
                                     OrgScopeService orgScopeService,
                                     BusinessCodeGenerator businessCodeGenerator) {
        this.itemTagRepository = itemTagRepository;
        this.orgScopeService = orgScopeService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CodeDataResponse<PageData<ItemTagRow>> list(Integer pageNo,
                                                       Integer pageSize,
                                                       String tagCode,
                                                       String tagName,
                                                       String itemName,
                                                       String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        int safePageNo = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
        int offset = (safePageNo - 1) * safePageSize;

        List<ItemTagDO> allRows = itemTagRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId());
        List<ItemTagDO> filteredRows = filterList(allRows, tagCode, tagName, itemName);
        Long total = (long) filteredRows.size();
        List<ItemTagDO> rows = filteredRows.stream()
                .skip(offset)
                .limit(safePageSize)
                .toList();

        List<ItemTagRow> list = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            ItemTagDO row = rows.get(i);
            list.add(new ItemTagRow(
                    row.getId(),
                    offset + i + 1,
                    row.getTagCode(),
                    row.getTagName(),
                    trimNullable(row.getRemark()),
                    formatDateTime(row.getCreatedAt()),
                    formatDateTime(row.getUpdatedAt())
            ));
        }

        return CodeDataResponse.ok(new PageData<>(list, total == null ? 0 : total, safePageNo, safePageSize));
    }

    @Transactional
    public CodeDataResponse<IdPayload> create(String orgId,
                                              ItemTagCreateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        String tagCode = generateTagCode(scope);
        String tagName = trim(request.tagName());

        ensureTagCodeUnique(scope, tagCode, null);
        ensureTagNameUnique(scope, tagName, null);

        ItemTagDO row = new ItemTagDO();
        row.setScopeType(scope.scopeType());
        row.setScopeId(scope.scopeId());
        row.setTagCode(tagCode);
        row.setTagName(tagName);
        row.setStatus(STATUS_ENABLED);
        row.setRemark(trimNullable(request.itemName()));
        itemTagRepository.save(row);

        return CodeDataResponse.ok(new IdPayload(row.getId()));
    }

    @Transactional
    public CodeDataResponse<Void> update(Long id,
                                         String orgId,
                                         ItemTagUpdateRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        ItemTagDO existing = requireTag(id, scope);
        String tagCode = trim(request.tagCode());
        String tagName = trim(request.tagName());

        ensureTagCodeUnique(scope, tagCode, id);
        ensureTagNameUnique(scope, tagName, id);

        existing.setTagCode(tagCode);
        existing.setTagName(tagName);
        existing.setRemark(trimNullable(request.itemName()));
        itemTagRepository.update(existing);
        return CodeDataResponse.ok();
    }

    @Transactional
    public CodeDataResponse<Void> delete(Long id,
                                         String orgId) {
        ItemScope scope = resolveItemScope(orgId);
        requireTag(id, scope);
        itemTagRepository.deleteById(id);
        return CodeDataResponse.ok();
    }

    @Transactional
    public CodeDataResponse<BatchImportResult> batchImport(String orgId,
                                                           ItemTagBatchImportRequest request) {
        ItemScope scope = resolveItemScope(orgId);
        Set<String> seenNames = new HashSet<>();
        BusinessCodeGenerator.CodeAllocator allocator = tagCodeAllocator(scope);
        int inserted = 0;
        int skipped = 0;

        for (ItemTagBatchImportRequest.Item item : request.items()) {
            String tagName = trim(item.tagName());
            if (!seenNames.add(tagName)) {
                skipped++;
                continue;
            }
            if (existsTagName(scope, tagName)) {
                skipped++;
                continue;
            }

            ItemTagDO row = new ItemTagDO();
            row.setScopeType(scope.scopeType());
            row.setScopeId(scope.scopeId());
            row.setTagCode(allocator.nextCode());
            row.setTagName(tagName);
            row.setStatus(STATUS_ENABLED);
            row.setRemark(trimNullable(item.itemName()));
            itemTagRepository.save(row);
            inserted++;
        }

        return CodeDataResponse.ok(new BatchImportResult(request.items().size(), inserted, skipped));
    }

    private ItemTagDO requireTag(Long id, ItemScope scope) {
        ItemTagDO row = itemTagRepository.findById(id).orElse(null);
        if (row == null) {
            throw new BusinessException("标签不存在");
        }
        if (!Objects.equals(row.getScopeType(), scope.scopeType()) || !Objects.equals(row.getScopeId(), scope.scopeId())) {
            throw new BusinessException("标签不存在");
        }
        return row;
    }

    private void ensureTagCodeUnique(ItemScope scope, String tagCode, Long excludeId) {
        boolean exists = itemTagRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getTagCode(), tagCode)
                        && (excludeId == null || !Objects.equals(row.getId(), excludeId)));
        if (exists) {
            throw new BusinessException("标签编码已存在");
        }
    }

    private void ensureTagNameUnique(ItemScope scope, String tagName, Long excludeId) {
        boolean exists = itemTagRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .anyMatch(row -> Objects.equals(row.getTagName(), tagName)
                        && (excludeId == null || !Objects.equals(row.getId(), excludeId)));
        if (exists) {
            throw new BusinessException("标签名称已存在");
        }
    }

    private boolean existsTagCode(ItemScope scope, String tagCode) {
        Long count = itemTagRepository.countByScopeAndTagCode(scope.scopeType(), scope.scopeId(), tagCode);
        return count != null && count > 0;
    }

    private boolean existsTagName(ItemScope scope, String tagName) {
        Long count = itemTagRepository.countByScopeAndTagName(scope.scopeType(), scope.scopeId(), tagName);
        return count != null && count > 0;
    }

    private List<ItemTagDO> filterList(List<ItemTagDO> rows,
                                       String tagCode,
                                       String tagName,
                                       String itemName) {
        String tagCodeKeyword = trimNullable(tagCode);
        String tagNameKeyword = trimNullable(tagName);
        String itemKeyword = trimNullable(itemName);
        return rows.stream()
                .filter(row -> tagCodeKeyword == null || contains(row.getTagCode(), tagCodeKeyword))
                .filter(row -> tagNameKeyword == null || contains(row.getTagName(), tagNameKeyword))
                .filter(row -> itemKeyword == null || contains(row.getRemark(), itemKeyword))
                .toList();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return DATETIME_FORMATTER.format(dateTime);
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

    private ItemScope resolveItemScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new ItemScope(scope.scopeType(), scope.scopeId());
    }

    private String generateTagCode(ItemScope scope) {
        return tagCodeAllocator(scope).nextCode();
    }

    private BusinessCodeGenerator.CodeAllocator tagCodeAllocator(ItemScope scope) {
        List<String> existingCodes = itemTagRepository.findByScopeOrdered(scope.scopeType(), scope.scopeId()).stream()
                .map(ItemTagDO::getTagCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.allocator(TAG_CODE_PREFIX, existingCodes);
    }

    public record PageData<T>(List<T> list,
                              long total,
                              int pageNo,
                              int pageSize) {
    }

    public record ItemTagRow(Long id,
                             int index,
                             String tagCode,
                             String tagName,
                             String itemName,
                             String createdAt,
                             String updatedAt) {
    }

    public record IdPayload(Long id) {
    }

    public record BatchImportResult(int totalCount,
                                    int insertedCount,
                                    int skippedCount) {
    }

    private record ItemScope(String scopeType, Long scopeId) {
    }
}

