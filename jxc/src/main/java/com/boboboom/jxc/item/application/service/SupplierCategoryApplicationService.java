package com.boboboom.jxc.item.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessCodeGenerator;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.application.auth.OrgScopeService;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierCategoryMapper;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCategoryCreateRequest;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class SupplierCategoryApplicationService {

    private static final String ROOT_CATEGORY = "供应商类别";
    private static final String CATEGORY_CODE_PREFIX = "GYLB";

    private final SupplierCategoryMapper supplierCategoryMapper;
    private final OrgScopeService orgScopeService;
    private final BusinessCodeGenerator businessCodeGenerator;

    public SupplierCategoryApplicationService(SupplierCategoryMapper supplierCategoryMapper,
                                              OrgScopeService orgScopeService,
                                              BusinessCodeGenerator businessCodeGenerator) {
        this.supplierCategoryMapper = supplierCategoryMapper;
        this.orgScopeService = orgScopeService;
        this.businessCodeGenerator = businessCodeGenerator;
    }

    public CodeDataResponse<List<TreeNode>> tree(String orgId) {
        CategoryScope scope = resolveScope(orgId);
        List<SupplierCategoryDO> list = supplierCategoryMapper.selectList(
                scopeQuery(scope)
                        .orderByAsc(SupplierCategoryDO::getCreatedAt)
                        .orderByAsc(SupplierCategoryDO::getId)
        );

        Map<String, TreeNode> nodeMap = new LinkedHashMap<>();
        nodeMap.put(ROOT_CATEGORY, new TreeNode("all", ROOT_CATEGORY, new ArrayList<>()));
        for (SupplierCategoryDO category : list) {
            nodeMap.putIfAbsent(category.getCategoryName(),
                    new TreeNode(category.getCategoryName(), category.getCategoryName(), new ArrayList<>()));
        }

        for (SupplierCategoryDO category : list) {
            String parentName = normalizeParentForTree(category.getParentCategory());
            TreeNode parentNode = nodeMap.computeIfAbsent(parentName,
                    key -> new TreeNode(key, key, new ArrayList<>()));
            TreeNode childNode = nodeMap.get(category.getCategoryName());
            if (childNode != null && parentNode.children().stream().noneMatch(item -> Objects.equals(item.id(), childNode.id()))) {
                parentNode.children().add(childNode);
            }
        }
        return CodeDataResponse.ok(List.of(nodeMap.get(ROOT_CATEGORY)));
    }

    @Transactional
    public CodeDataResponse<IdPayload> create(String orgId,
                                              SupplierCategoryCreateRequest request) {
        CategoryScope scope = resolveScope(orgId);
        String categoryCode = generateCategoryCode(scope);
        String categoryName = trim(request.categoryName());
        String parentCategory = normalizeParent(trim(request.parentCategory()));

        ensureCodeUnique(scope, categoryCode);
        ensureNameUnique(scope, categoryName);
        ensureParentExists(scope, parentCategory);

        SupplierCategoryDO row = new SupplierCategoryDO();
        row.setScopeType(scope.scopeType());
        row.setScopeId(scope.scopeId());
        row.setCategoryCode(categoryCode);
        row.setCategoryName(categoryName);
        row.setParentCategory(parentCategory);
        supplierCategoryMapper.insert(row);
        return CodeDataResponse.ok(new IdPayload(row.getId()));
    }

    private String generateCategoryCode(CategoryScope scope) {
        List<String> existingCodes = supplierCategoryMapper.selectList(
                        scopeQuery(scope).select(SupplierCategoryDO::getCategoryCode))
                .stream()
                .map(SupplierCategoryDO::getCategoryCode)
                .filter(StringUtils::hasText)
                .toList();
        return businessCodeGenerator.nextCode(CATEGORY_CODE_PREFIX, existingCodes);
    }

    private void ensureCodeUnique(CategoryScope scope, String code) {
        SupplierCategoryDO existing = supplierCategoryMapper.selectOne(
                scopeQuery(scope).eq(SupplierCategoryDO::getCategoryCode, code).last("limit 1")
        );
        if (existing != null) {
            throw new BusinessException("类别编码已存在");
        }
    }

    private void ensureNameUnique(CategoryScope scope, String name) {
        SupplierCategoryDO existing = supplierCategoryMapper.selectOne(
                scopeQuery(scope).eq(SupplierCategoryDO::getCategoryName, name).last("limit 1")
        );
        if (existing != null) {
            throw new BusinessException("类别名称已存在");
        }
    }

    private void ensureParentExists(CategoryScope scope, String parentCategory) {
        if (Objects.equals(parentCategory, ROOT_CATEGORY)) {
            return;
        }
        SupplierCategoryDO parent = supplierCategoryMapper.selectOne(
                scopeQuery(scope).eq(SupplierCategoryDO::getCategoryName, parentCategory).last("limit 1")
        );
        if (parent == null) {
            throw new BusinessException("上级类别不存在");
        }
    }

    private String normalizeParentForTree(String parentCategory) {
        if (!StringUtils.hasText(parentCategory)) {
            return ROOT_CATEGORY;
        }
        String normalized = parentCategory.trim();
        if (normalized.isEmpty() || Objects.equals(normalized, ROOT_CATEGORY)) {
            return ROOT_CATEGORY;
        }
        return normalized;
    }

    private String normalizeParent(String parentCategory) {
        if (!StringUtils.hasText(parentCategory)) {
            return ROOT_CATEGORY;
        }
        String normalized = parentCategory.trim();
        if (normalized.isEmpty() || Objects.equals(normalized, "all")) {
            return ROOT_CATEGORY;
        }
        return normalized;
    }

    private LambdaQueryWrapper<SupplierCategoryDO> scopeQuery(CategoryScope scope) {
        return new LambdaQueryWrapper<SupplierCategoryDO>()
                .eq(SupplierCategoryDO::getScopeType, scope.scopeType())
                .eq(SupplierCategoryDO::getScopeId, scope.scopeId());
    }

    private CategoryScope resolveScope(String orgId) {
        OrgScopeService.AccessibleScope scope = orgScopeService.resolveAccessibleScope(AuthContextHolder.requireUserId("登录已失效，请重新登录"), orgId);
        return new CategoryScope(scope.scopeType(), scope.scopeId());
    }

    private String trim(String value) {
        if (value == null) {
            throw new BusinessException("参数不能为空");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException("参数不能为空");
        }
        return trimmed;
    }

    public record TreeNode(String id, String label, List<TreeNode> children) {
    }

    public record IdPayload(Long id) {
    }

    private record CategoryScope(String scopeType, Long scopeId) {
    }
}
