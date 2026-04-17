package com.boboboom.jxc.identity.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserAccountDO;
import com.boboboom.jxc.identity.infrastructure.persistence.dataobject.UserRoleRelDO;
import com.boboboom.jxc.identity.domain.repository.StoreRepository;
import com.boboboom.jxc.identity.domain.repository.UserRoleRelRepository;
import com.boboboom.jxc.identity.infrastructure.persistence.mapper.UserAccountMapper;
import com.boboboom.jxc.identity.infrastructure.persistence.query.UserRoleView;
import com.boboboom.jxc.identity.interfaces.rest.request.StatusUpdateRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.UserUpsertRequest;
import com.boboboom.jxc.identity.application.auth.PasswordCodec;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserAdministrationService {

    private static final String STATUS_ENABLED = "ENABLED";

    private final UserAccountMapper userAccountMapper;
    private final UserRoleRelRepository userRoleRelRepository;
    private final StoreRepository storeRepository;
    private final IdentityAccessControlService identityAccessControlService;
    private final IdentityAdminLookupService identityAdminLookupService;

    public UserAdministrationService(UserAccountMapper userAccountMapper,
                                     UserRoleRelRepository userRoleRelRepository,
                                     StoreRepository storeRepository,
                                     IdentityAccessControlService identityAccessControlService,
                                     IdentityAdminLookupService identityAdminLookupService) {
        this.userAccountMapper = userAccountMapper;
        this.userRoleRelRepository = userRoleRelRepository;
        this.storeRepository = storeRepository;
        this.identityAccessControlService = identityAccessControlService;
        this.identityAdminLookupService = identityAdminLookupService;
    }

    public UserAccountDO createUser(UserUpsertRequest request, String phone) {
        UserAccountDO exists = userAccountMapper.selectOne(
                new LambdaQueryWrapper<UserAccountDO>().eq(UserAccountDO::getPhone, phone).last("limit 1")
        );
        if (exists != null) {
            throw new com.boboboom.jxc.common.BusinessException("手机号已存在");
        }

        UserAccountDO user = new UserAccountDO();
        user.setUsername(phone);
        user.setRealName(identityAdminLookupService.trim(request.getRealName()));
        user.setPhone(phone);
        user.setPasswordHash(PasswordCodec.encode("123654"));
        user.setPasswordSalt(null);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        user.setSourceType("MANUAL");
        user.setFirstLoginChangedPwd(Boolean.FALSE);
        userAccountMapper.insert(user);
        return user;
    }

    public UserAccountDO updateUserStatus(Long id, StatusUpdateRequest request) {
        UserAccountDO user = identityAdminLookupService.requireUser(id);
        user.setStatus(identityAdminLookupService.normalizeStatus(request.getStatus()));
        userAccountMapper.updateById(user);
        return user;
    }

    public List<UserAdminSnapshot> listUsers(Long operatorId, boolean platformAdmin) {
        List<UserAccountDO> users;
        if (platformAdmin) {
            users = userAccountMapper.selectList(
                    new LambdaQueryWrapper<UserAccountDO>()
                            .orderByDesc(UserAccountDO::getCreatedAt)
                            .orderByDesc(UserAccountDO::getId)
            );
        } else {
            List<Long> managedGroupIds = userRoleRelRepository.findByUserIdAndScopeTypeAndStatus(operatorId, "GROUP", STATUS_ENABLED)
                    .stream()
                    .map(UserRoleRelDO::getScopeId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            if (managedGroupIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Long> managedStoreIds = storeRepository.findByGroupIds(managedGroupIds)
                    .stream()
                    .map(store -> store.getId())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            List<Long> scopedUserIds = userRoleRelRepository.findByStatusAndGroupOrStoreScopes(
                            STATUS_ENABLED, new LinkedHashSet<>(managedGroupIds), new LinkedHashSet<>(managedStoreIds))
                    .stream()
                    .map(UserRoleRelDO::getUserId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            LinkedHashSet<Long> visibleUserIds = new LinkedHashSet<>(scopedUserIds);
            if (visibleUserIds.isEmpty()) {
                return Collections.emptyList();
            }

            users = userAccountMapper.selectList(new LambdaQueryWrapper<UserAccountDO>()
                    .in(UserAccountDO::getId, visibleUserIds)
                    .orderByDesc(UserAccountDO::getCreatedAt)
                    .orderByDesc(UserAccountDO::getId));
        }

        if (users.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = users.stream().map(UserAccountDO::getId).toList();
        Map<Long, List<UserRoleView>> userRolesMap = userAccountMapper.selectUserRolesByUserIds(userIds).stream()
                .collect(Collectors.groupingBy(UserRoleView::getUserId));

        return users.stream()
                .map(user -> new UserAdminSnapshot(
                        user.getId(),
                        user.getUsername(),
                        user.getRealName(),
                        user.getPhone(),
                        user.getStatus(),
                        user.getCreatedAt(),
                        userRolesMap.getOrDefault(user.getId(), Collections.emptyList()).stream()
                                .map(role -> new RoleAssignmentSnapshot(
                                        role.getRoleId(),
                                        role.getRoleCode(),
                                        role.getRoleName(),
                                        role.getRoleType(),
                                        role.getScopeType(),
                                        role.getScopeId(),
                                        role.getScopeName()
                                ))
                                .toList()
                ))
                .toList();
    }

    public void ensureCanManageUser(Long targetUserId, Long operatorId) {
        LinkedHashSet<Long> managedGroupIds = new LinkedHashSet<>(identityAccessControlService.listManagedGroupIds(operatorId));
        LinkedHashSet<Long> managedStoreIds = new LinkedHashSet<>(identityAccessControlService.listManagedStoreIds(managedGroupIds));
        if (managedGroupIds.isEmpty() && managedStoreIds.isEmpty()) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无可管理用户范围");
        }

        Long matched = userRoleRelRepository.countByUserAndScopedRoles(
                targetUserId, STATUS_ENABLED, managedGroupIds, managedStoreIds
        );
        if (matched == null || matched == 0) {
            throw new com.boboboom.jxc.common.BusinessException("当前账号无该用户操作权限");
        }
    }

    public boolean hasEnabledRoleAssignments(Long userId) {
        if (userId == null) {
            return false;
        }
        Long count = userRoleRelRepository.countByUserIdAndStatus(userId, STATUS_ENABLED);
        return count != null && count > 0;
    }

    public record UserAdminSnapshot(Long id,
                                    String username,
                                    String realName,
                                    String phone,
                                    String status,
                                    LocalDateTime createdAt,
                                    List<RoleAssignmentSnapshot> roles) {
    }

    public record RoleAssignmentSnapshot(Long roleId,
                                         String roleCode,
                                         String roleName,
                                         String roleType,
                                         String scopeType,
                                         Long scopeId,
                                         String scopeName) {
    }
}
