package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.GroupAdministrationService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.UserAdministrationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;

import jakarta.validation.Valid;

/**
 * 组织分组管理接口，负责分组、门店以及分组管理员的维护。
 */
@Validated
@RestController
@RequestMapping("/api/identity/admin/groups")
public class IdentityGroupAdminController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    private final IdentityAccessControlService identityAccessControlService;
    private final GroupAdministrationService groupAdministrationService;
    private final UserAdministrationService userAdministrationService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造组织分组管理接口。
     *
     * @param identityAccessControlServiceValue 组织权限控制服务
     * @param groupAdministrationServiceValue 分组管理服务
     * @param identityAdminSupportValue 当前登录管理员辅助服务
     */
    public IdentityGroupAdminController(IdentityAccessControlService identityAccessControlServiceValue,
                                        GroupAdministrationService groupAdministrationServiceValue,
                                        UserAdministrationService userAdministrationServiceValue,
                                        IdentityAdminSupport identityAdminSupportValue) {
        this.identityAccessControlService = identityAccessControlServiceValue;
        this.groupAdministrationService = groupAdministrationServiceValue;
        this.userAdministrationService = userAdministrationServiceValue;
        this.identityAdminSupport = identityAdminSupportValue;
    }

    /**
     * 查询当前管理员可见的组织分组列表。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分组列表响应
     */
    @GetMapping
    public CodeDataResponse<PageData<GroupAdminView>> listGroups(@RequestParam(defaultValue = "1") Integer pageNum,
                                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        boolean platformAdmin = identityAdminSupport.isPlatformAdmin(operatorId);
        List<GroupAdminView> result = groupAdministrationService.listGroups(operatorId, platformAdmin).stream()
                .map(group -> new GroupAdminView(
                        group.getId(),
                        group.getGroupCode(),
                        group.getGroupName(),
                        group.getStatus(),
                        group.getRemark(),
                        group.getCreatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 新建组织分组。
     *
     * @param request 分组新增请求
     * @return 新建结果
     */
    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createGroup(@Valid @RequestBody GroupUpsertRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        GroupDO group = groupAdministrationService.createGroup(request, identityAdminSupport.currentOperatorId());
        return CodeDataResponse.ok(new IdPayload(group.getId()));
    }

    /**
     * 修改组织分组信息。
     *
     * @param id 分组主键
     * @param request 分组更新请求
     * @return 空响应
     */
    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateGroup(@PathVariable Long id,
                                              @Valid @RequestBody GroupUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        if (!identityAdminSupport.isPlatformAdmin(operatorId)) {
            identityAccessControlService.ensureCanManageGroup(operatorId, id);
        }
        groupAdministrationService.updateGroup(id, request);
        return CodeDataResponse.ok();
    }

    /**
     * 删除组织分组。
     *
     * @param id 分组主键
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteGroup(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        groupAdministrationService.deleteGroup(id);
        return CodeDataResponse.ok();
    }

    /**
     * 更新组织分组状态。
     *
     * @param id 分组主键
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateGroupStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        if (!identityAdminSupport.isPlatformAdmin(operatorId)) {
            identityAccessControlService.ensureCanManageGroup(operatorId, id);
        }
        groupAdministrationService.updateGroupStatus(id, request.getStatus());
        return CodeDataResponse.ok();
    }

    /**
     * 查询分组下的门店列表。
     *
     * @param groupId 分组主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 门店列表响应
     */
    @GetMapping("/{groupId}/stores")
    public CodeDataResponse<PageData<StoreAdminView>> listGroupStores(@PathVariable Long groupId,
                                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        List<StoreAdminView> result = groupAdministrationService.listGroupStores(groupId).stream()
                .map(store -> new StoreAdminView(
                        store.getId(),
                        store.getGroupId(),
                        store.getStoreCode(),
                        store.getStoreName(),
                        store.getStatus(),
                        store.getContactName(),
                        store.getContactPhone(),
                        store.getAddress(),
                        store.getRemark(),
                        store.getCreatedAt()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /** 处理GetMapping。 */
    @GetMapping("/{groupId}/users")
    public CodeDataResponse<PageData<UserOptionView>> listGroupUsers(@PathVariable Long groupId,
                                                                     @RequestParam(defaultValue = "1") Integer pageNum,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        List<UserOptionView> result = userAdministrationService.listGroupUserOptions(groupId).stream()
                .map(user -> new UserOptionView(
                        user.userId(),
                        user.username(),
                        user.realName(),
                        user.phone(),
                        user.status()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    /**
     * 新增分组下的门店。
     *
     * @param groupId 分组主键
     * @param request 门店新增请求
     * @return 新建结果
     */
    @PostMapping("/{groupId}/stores")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<IdPayload> createGroupStore(@PathVariable Long groupId,
                                                        @Valid @RequestBody GroupStoreCreateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        identityAccessControlService.ensureCanManageGroup(operatorId, groupId);
        StoreDO store = groupAdministrationService.createGroupStore(groupId, request, operatorId);
        return CodeDataResponse.ok(new IdPayload(store.getId()));
    }

    /**
     * 更新分组下的门店。
     *
     * @param groupId 分组主键
     * @param storeId 门店主键
     * @param request 门店更新请求
     * @return 空响应
     */
    @PutMapping("/{groupId}/stores/{storeId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> updateGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId,
                                                   @Valid @RequestBody GroupStoreCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.updateGroupStore(groupId, storeId, request);
        return CodeDataResponse.ok();
    }

    /**
     * 删除分组下的门店。
     *
     * @param groupId 分组主键
     * @param storeId 门店主键
     * @return 空响应
     */
    @DeleteMapping("/{groupId}/stores/{storeId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    public CodeDataResponse<Void> deleteGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.deleteGroupStore(groupId, storeId);
        return CodeDataResponse.ok();
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        int fromIndex = Math.min((safePageNum - 1) * safePageSize, rows.size());
        int toIndex = Math.min(fromIndex + safePageSize, rows.size());
        return new PageData<>(rows.subList(fromIndex, toIndex), rows.size(), safePageNum, safePageSize);
    }

    /**
     * 通用分页响应。
     *
     * @param <T> 数据类型
     * @param list 当前页数据
     * @param total 总条数
     * @param pageNum 当前页码
     * @param pageSize 当前页大小
     */
    public record PageData<T>(List<T> list, long total, int pageNum, int pageSize) {
    }
}
