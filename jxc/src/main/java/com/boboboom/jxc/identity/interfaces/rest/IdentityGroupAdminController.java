package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.GroupAdministrationService;
import com.boboboom.jxc.identity.application.service.IdentityAccessControlService;
import com.boboboom.jxc.identity.application.service.IdentityAdminLookupService;
import com.boboboom.jxc.identity.application.service.StoreSampleDataInitializationService;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.GroupDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.StoreDO;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupAdminBindRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupStoreCreateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.GroupUpsertRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import jakarta.validation.Valid;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin/groups")
/**
 * 组织分组管理接口，负责分组、门店以及分组管理员的维护。
 */
public class IdentityGroupAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final GroupAdministrationService groupAdministrationService;
    private final StoreSampleDataInitializationService storeSampleDataInitializationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

    /**
     * 构造组织分组管理接口。
     *
     * @param identityAccessControlService 组织权限控制服务
     * @param groupAdministrationService 分组管理服务
     * @param storeSampleDataInitializationService 门店初始化服务
     * @param identityAdminLookupService 组织管理查询服务
     * @param identityAdminSupport 当前登录管理员辅助服务
     */
    public IdentityGroupAdminController(IdentityAccessControlService identityAccessControlService,
                                        GroupAdministrationService groupAdministrationService,
                                        StoreSampleDataInitializationService storeSampleDataInitializationService,
                                        IdentityAdminLookupService identityAdminLookupService,
                                        IdentityAdminSupport identityAdminSupport) {
        this.identityAccessControlService = identityAccessControlService;
        this.groupAdministrationService = groupAdministrationService;
        this.storeSampleDataInitializationService = storeSampleDataInitializationService;
        this.identityAdminLookupService = identityAdminLookupService;
        this.identityAdminSupport = identityAdminSupport;
    }

    @GetMapping
    /**
     * 查询当前管理员可见的组织分组列表。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 分组列表响应
     */
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

    @PostMapping
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新建组织分组。
     *
     * @param request 分组新增请求
     * @return 新建结果
     */
    public CodeDataResponse<IdPayload> createGroup(@Valid @RequestBody GroupUpsertRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        GroupDO group = groupAdministrationService.createGroup(request, identityAdminSupport.currentOperatorId());
        return CodeDataResponse.ok(new IdPayload(group.getId()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 修改组织分组信息。
     *
     * @param id 分组主键
     * @param request 分组更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateGroup(@PathVariable Long id,
                                              @Valid @RequestBody GroupUpsertRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        if (!identityAdminSupport.isPlatformAdmin(operatorId)) {
            identityAccessControlService.ensureCanManageGroup(operatorId, id);
        }
        groupAdministrationService.updateGroup(id, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除组织分组。
     *
     * @param id 分组主键
     * @return 空响应
     */
    public CodeDataResponse<Void> deleteGroup(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        groupAdministrationService.deleteGroup(id);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新组织分组状态。
     *
     * @param id 分组主键
     * @param request 状态更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateGroupStatus(@PathVariable Long id,
                                                    @Valid @RequestBody StatusUpdateRequest request) {
        Long operatorId = identityAdminSupport.currentOperatorId();
        if (!identityAdminSupport.isPlatformAdmin(operatorId)) {
            identityAccessControlService.ensureCanManageGroup(operatorId, id);
        }
        groupAdministrationService.updateGroupStatus(id, request.getStatus());
        return CodeDataResponse.ok();
    }

    @PostMapping("/{groupId}/bind-admin")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 为分组绑定管理员。
     *
     * @param groupId 分组主键
     * @param request 绑定管理员请求
     * @return 绑定结果
     */
    public CodeDataResponse<BindGroupAdminResult> bindGroupAdmin(@PathVariable Long groupId,
                                                                 @Valid @RequestBody GroupAdminBindRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        GroupDO group = identityAdminLookupService.requireGroup(groupId);
        String phone = identityAdminLookupService.normalizePhone(request.getPhone());
        String realName = identityAdminLookupService.trimNullable(request.getRealName()) == null
                ? phone
                : identityAdminLookupService.trim(request.getRealName());
        GroupAdministrationService.BindGroupAdminSnapshot snapshot = groupAdministrationService.bindGroupAdmin(
                group,
                identityAdminSupport.currentOperatorId(),
                phone,
                realName
        );
        return CodeDataResponse.ok(new BindGroupAdminResult(
                snapshot.groupId(),
                snapshot.groupName(),
                snapshot.userId(),
                snapshot.phone(),
                snapshot.realName()
        ));
    }

    @GetMapping("/{groupId}/stores")
    /**
     * 查询分组下的门店列表。
     *
     * @param groupId 分组主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 门店列表响应
     */
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

    @GetMapping("/{groupId}/admin-candidates")
    /**
     * 查询可绑定为分组管理员的候选人列表。
     *
     * @param groupId 分组主键
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 候选人列表响应
     */
    public CodeDataResponse<PageData<GroupAdminCandidateView>> listGroupAdminCandidates(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        List<GroupAdminCandidateView> result = groupAdministrationService.listGroupAdminCandidates(groupId).stream()
                .map(candidate -> new GroupAdminCandidateView(
                        candidate.userId(),
                        candidate.realName(),
                        candidate.phone(),
                        candidate.storeId(),
                        candidate.storeCode(),
                        candidate.storeName()
                ))
                .toList();
        return CodeDataResponse.ok(paginate(result, pageNum, pageSize));
    }

    @PostMapping("/{groupId}/stores")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 新增分组下的门店。
     *
     * @param groupId 分组主键
     * @param request 门店新增请求
     * @return 新建结果
     */
    public CodeDataResponse<IdPayload> createGroupStore(@PathVariable Long groupId,
                                                        @Valid @RequestBody GroupStoreCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        StoreDO store = groupAdministrationService.createGroupStore(groupId, request);
        storeSampleDataInitializationService.initializeStoreSampleData(store.getId());
        return CodeDataResponse.ok(new IdPayload(store.getId()));
    }

    @PutMapping("/{groupId}/stores/{storeId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 更新分组下的门店。
     *
     * @param groupId 分组主键
     * @param storeId 门店主键
     * @param request 门店更新请求
     * @return 空响应
     */
    public CodeDataResponse<Void> updateGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId,
                                                   @Valid @RequestBody GroupStoreCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.updateGroupStore(groupId, storeId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{groupId}/stores/{storeId}")
    @PreAuthorize("@requestPermissionGuard.authenticated()")
    /**
     * 删除分组下的门店。
     *
     * @param groupId 分组主键
     * @param storeId 门店主键
     * @return 空响应
     */
    public CodeDataResponse<Void> deleteGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.deleteGroupStore(groupId, storeId);
        return CodeDataResponse.ok();
    }

    private <T> PageData<T> paginate(List<T> rows, Integer pageNum, Integer pageSize) {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int safePageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 200);
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
