package com.boboboom.jxc.item.interfaces.rest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.common.BusinessException;
import com.boboboom.jxc.identity.application.auth.AuthContextHolder;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.RoleDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.RoleMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.StoreMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserRoleRelMapper;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.item.infrastructure.persistence.dataobject.SupplierCategoryDO;
import com.boboboom.jxc.item.infrastructure.persistence.mapper.SupplierCategoryMapper;
import com.boboboom.jxc.item.interfaces.rest.request.SupplierCategoryCreateRequest;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Validated
@RestController
@RequestMapping("/api/items/supplier-categories")
public class SupplierCategoryController {

    private static final String ROOT_CATEGORY = "供应商类别";
    private static final String SCOPE_PLATFORM = "PLATFORM";
    private static final String SCOPE_GROUP = "GROUP";
    private static final String SCOPE_STORE = "STORE";

    private final SupplierCategoryMapper supplierCategoryMapper;
    private final RoleMapper roleMapper;
    private final UserRoleRelMapper userRoleRelMapper;
    private final StoreMapper storeMapper;

    public SupplierCategoryController(SupplierCategoryMapper supplierCategoryMapper,
                                      RoleMapper roleMapper,
                                      UserRoleRelMapper userRoleRelMapper,
                                      StoreMapper storeMapper) {
        this.supplierCategoryMapper = supplierCategoryMapper;
        this.roleMapper = roleMapper;
        this.userRoleRelMapper = userRoleRelMapper;
        this.storeMapper = storeMapper;
    }

    @GetMapping("/tree")
    public CodeDataResponse<List<TreeNode>> tree(@RequestParam(required = false) String orgId) {
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

    @PostMapping
    @Transactional
    public CodeDataResponse<IdPayload> create(@RequestParam(required = false) String orgId,
                                              @Valid @RequestBody SupplierCategoryCreateRequest request) {
        CategoryScope scope = resolveScope(orgId);
        String categoryCode = trim(request.categoryCode());
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
        Long userId = currentUserId();
        Scope requested = parseScope(orgId);
        if (isPlatformAdmin(userId)) {
            return new CategoryScope(requested.scopeType(), requested.scopeId());
        }
        if (SCOPE_PLATFORM.equals(requested.scopeType())) {
            throw new BusinessException("请先选择有权限的机构");
        }
        if (SCOPE_GROUP.equals(requested.scopeType())) {
            if (!hasScope(userId, SCOPE_GROUP, requested.scopeId())) {
                throw new BusinessException("当前账号无该集团权限");
            }
            return new CategoryScope(SCOPE_GROUP, requested.scopeId());
        }
        if (SCOPE_STORE.equals(requested.scopeType())) {
            if (hasScope(userId, SCOPE_STORE, requested.scopeId())) {
                return new CategoryScope(SCOPE_STORE, requested.scopeId());
            }
            Long groupId = findGroupIdByStoreId(requested.scopeId());
            if (groupId != null && hasScope(userId, SCOPE_GROUP, groupId)) {
                return new CategoryScope(SCOPE_STORE, requested.scopeId());
            }
            throw new BusinessException("当前账号无该门店权限");
        }
        throw new BusinessException("机构参数非法");
    }

    private Scope parseScope(String orgId) {
        if (orgId == null || orgId.isBlank()) {
            return new Scope(SCOPE_PLATFORM, 0L);
        }
        if (orgId.startsWith("group-")) {
            return new Scope(SCOPE_GROUP, parseNumericId(orgId.substring("group-".length())));
        }
        if (orgId.startsWith("store-")) {
            return new Scope(SCOPE_STORE, parseNumericId(orgId.substring("store-".length())));
        }
        throw new BusinessException("机构参数非法");
    }

    private Long parseNumericId(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("机构参数非法");
        }
    }

    private Long currentUserId() {
        if (AuthContextHolder.get() == null || AuthContextHolder.get().getUserId() == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        return AuthContextHolder.get().getUserId();
    }

    private boolean isPlatformAdmin(Long userId) {
        RoleDO role = roleMapper.selectOne(new LambdaQueryWrapper<RoleDO>()
                .eq(RoleDO::getRoleCode, "PLATFORM_SUPER_ADMIN")
                .last("limit 1"));
        if (role == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getRoleId, role.getId())
                .eq(UserRoleRelDO::getScopeType, SCOPE_PLATFORM)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }

    private boolean hasScope(Long userId, String scopeType, Long scopeId) {
        if (scopeId == null) {
            return false;
        }
        UserRoleRelDO rel = userRoleRelMapper.selectOne(new LambdaQueryWrapper<UserRoleRelDO>()
                .eq(UserRoleRelDO::getUserId, userId)
                .eq(UserRoleRelDO::getScopeType, scopeType)
                .eq(UserRoleRelDO::getScopeId, scopeId)
                .eq(UserRoleRelDO::getStatus, "ENABLED")
                .last("limit 1"));
        return rel != null;
    }

    private Long findGroupIdByStoreId(Long storeId) {
        if (storeId == null) {
            return null;
        }
        StoreDO store = storeMapper.selectById(storeId);
        if (store == null) {
            return null;
        }
        return store.getGroupId();
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

    private record Scope(String scopeType, Long scopeId) {
    }

    private record CategoryScope(String scopeType, Long scopeId) {
    }
}
