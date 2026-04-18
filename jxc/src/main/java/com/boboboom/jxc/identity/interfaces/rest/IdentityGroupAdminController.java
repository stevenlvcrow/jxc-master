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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/identity/admin/groups")
public class IdentityGroupAdminController {

    private final IdentityAccessControlService identityAccessControlService;
    private final GroupAdministrationService groupAdministrationService;
    private final StoreSampleDataInitializationService storeSampleDataInitializationService;
    private final IdentityAdminLookupService identityAdminLookupService;
    private final IdentityAdminSupport identityAdminSupport;

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
    public CodeDataResponse<List<GroupAdminView>> listGroups() {
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
        return CodeDataResponse.ok(result);
    }

    @PostMapping
    public CodeDataResponse<IdPayload> createGroup(@Valid @RequestBody GroupUpsertRequest request) {
        identityAdminSupport.requirePlatformAdmin();
        GroupDO group = groupAdministrationService.createGroup(request, identityAdminSupport.currentOperatorId());
        return CodeDataResponse.ok(new IdPayload(group.getId()));
    }

    @PutMapping("/{id}")
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
    public CodeDataResponse<Void> deleteGroup(@PathVariable Long id) {
        identityAdminSupport.requirePlatformAdmin();
        groupAdministrationService.deleteGroup(id);
        return CodeDataResponse.ok();
    }

    @PutMapping("/{id}/status")
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
    public CodeDataResponse<List<StoreAdminView>> listGroupStores(@PathVariable Long groupId) {
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
        return CodeDataResponse.ok(result);
    }

    @GetMapping("/{groupId}/admin-candidates")
    public CodeDataResponse<List<GroupAdminCandidateView>> listGroupAdminCandidates(@PathVariable Long groupId) {
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
        return CodeDataResponse.ok(result);
    }

    @PostMapping("/{groupId}/stores")
    public CodeDataResponse<IdPayload> createGroupStore(@PathVariable Long groupId,
                                                        @Valid @RequestBody GroupStoreCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        StoreDO store = groupAdministrationService.createGroupStore(groupId, request);
        storeSampleDataInitializationService.initializeStoreSampleData(store.getId());
        return CodeDataResponse.ok(new IdPayload(store.getId()));
    }

    @PutMapping("/{groupId}/stores/{storeId}")
    public CodeDataResponse<Void> updateGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId,
                                                   @Valid @RequestBody GroupStoreCreateRequest request) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.updateGroupStore(groupId, storeId, request);
        return CodeDataResponse.ok();
    }

    @DeleteMapping("/{groupId}/stores/{storeId}")
    public CodeDataResponse<Void> deleteGroupStore(@PathVariable Long groupId,
                                                   @PathVariable Long storeId) {
        identityAccessControlService.ensureCanManageGroup(identityAdminSupport.currentOperatorId(), groupId);
        groupAdministrationService.deleteGroupStore(groupId, storeId);
        return CodeDataResponse.ok();
    }
}
