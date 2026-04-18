/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { batchDeleteAdminUsersApi, assignAdminUserRolesApi, createAdminUserApi, fetchAdminGroupsApi, fetchAdminRolesApi, fetchAdminUsersApi, fetchGroupStoresApi, deleteAdminUserApi, updateAdminUserApi, updateAdminUserStatusApi, } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const roleAssignmentKey = (assignment) => assignment.scopeType === 'STORE'
    ? `STORE-${assignment.scopeId ?? 'null'}`
    : `${assignment.roleId ?? 'null'}-${assignment.scopeType}-${assignment.scopeId ?? 'null'}`;
const dedupeAssignments = (rows) => {
    const unique = new Map();
    rows.forEach((row) => {
        unique.set(roleAssignmentKey(row), row);
    });
    return [...unique.values()];
};
const loading = ref(false);
const scopeLoading = ref(false);
const users = ref([]);
const roles = ref([]);
const groups = ref([]);
const stores = ref([]);
const filteredUsers = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedUserIds = ref([]);
const userTableRef = ref(null);
const sessionStore = useSessionStore();
const createDialogVisible = ref(false);
const assignDialogVisible = ref(false);
const copyDialogVisible = ref(false);
const bulkCopyDialogVisible = ref(false);
const submitting = ref(false);
const assigning = ref(false);
const copying = ref(false);
const bulkCopying = ref(false);
const selectedUser = ref(null);
const editingUser = ref(null);
const copiedUser = ref(null);
const editableAssignments = ref([]);
const copySourceStoreId = ref(null);
const copyTargetStoreIds = ref([]);
const bulkCopySourceStoreId = ref(null);
const bulkCopyTargetStoreIds = ref([]);
const createForm = reactive({
    realName: '',
    phone: '',
    status: 'ENABLED',
});
const queryForm = reactive({
    realName: '',
    phone: '',
    roleId: undefined,
    storeId: undefined,
});
const roleTypeScopeMap = {
    PLATFORM: 'PLATFORM',
    GROUP: 'GROUP',
    STORE: 'STORE',
};
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);
const roleOptions = computed(() => roles.value.map((item) => ({
    label: `${item.roleName}（${item.roleCode}）`,
    value: item.id,
})));
const userDialogTitle = computed(() => (editingUser.value ? '编辑用户' : '新增用户'));
const pagedUsers = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredUsers.value.slice(start, start + pageSize.value);
});
const groupScopeOptions = computed(() => groups.value.map((group) => ({
    label: `${group.groupName}（${group.groupCode}）`,
    value: group.id,
})));
const groupNameMap = computed(() => new Map(groups.value.map((group) => [group.id, group.groupName])));
const storeScopeOptions = computed(() => stores.value.map((store) => ({
    label: `${groupNameMap.value.get(store.groupId) ?? '集团'} / ${store.storeName}（${store.storeCode}）`,
    value: store.id,
})));
const formatDateTime = (value) => {
    if (!value) {
        return '-';
    }
    const match = value.trim().match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.\d+)?$/);
    if (!match) {
        return value;
    }
    const [, year, month, day, hour, minute, second] = match;
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
};
const buildCopySourceStoreOptions = (usersToScan) => {
    const seen = new Map();
    usersToScan.forEach((user) => {
        user.roles
            .filter((role) => role.scopeType === 'STORE' && role.scopeId != null && role.builtin)
            .forEach((role) => {
            if (!seen.has(role.scopeId)) {
                seen.set(role.scopeId, role);
            }
        });
    });
    return [...seen.values()].map((role) => ({
        label: `${role.scopeName ?? '门店'} / ${role.roleName}`,
        value: role.scopeId,
    }));
};
const copySourceStoreOptions = computed(() => buildCopySourceStoreOptions(copiedUser.value ? [copiedUser.value] : []));
const bulkCopySourceUsers = computed(() => users.value.filter((item) => selectedUserIds.value.includes(item.id)));
const bulkCopySourceStoreOptions = computed(() => buildCopySourceStoreOptions(bulkCopySourceUsers.value));
const copyTargetStoreOptions = computed(() => storeScopeOptions.value.filter((item) => item.value !== copySourceStoreId.value));
const bulkCopyTargetStoreOptions = computed(() => storeScopeOptions.value.filter((item) => item.value !== bulkCopySourceStoreId.value));
let assignmentSeed = 0;
const nextAssignmentUid = () => {
    assignmentSeed += 1;
    return `assignment-${assignmentSeed}`;
};
const buildEmptyAssignment = () => ({
    uid: nextAssignmentUid(),
    roleId: undefined,
    scopeType: 'PLATFORM',
    scopeId: null,
});
const getRoleById = (roleId) => roles.value.find((role) => role.id === roleId);
const applyQuery = () => {
    const realName = queryForm.realName.trim().toLowerCase();
    const phone = queryForm.phone.trim();
    const roleId = queryForm.roleId;
    const storeId = queryForm.storeId;
    filteredUsers.value = users.value.filter((item) => {
        const matchRealName = !realName || item.realName.toLowerCase().includes(realName);
        const matchPhone = !phone || item.phone.includes(phone);
        const matchRole = !roleId || item.roles.some((role) => role.roleId === roleId);
        const matchStore = !storeId || item.roles.some((role) => role.scopeType === 'STORE' && role.scopeId === storeId);
        return matchRealName && matchPhone && matchRole && matchStore;
    });
    currentPage.value = 1;
};
const resetQuery = () => {
    queryForm.realName = '';
    queryForm.phone = '';
    queryForm.roleId = undefined;
    queryForm.storeId = undefined;
    applyQuery();
};
const resetCreateForm = () => {
    createForm.realName = '';
    createForm.phone = '';
    createForm.status = 'ENABLED';
    editingUser.value = null;
};
const openCreateDialog = () => {
    editingUser.value = null;
    resetCreateForm();
    createDialogVisible.value = true;
};
const openEditDialog = (row) => {
    editingUser.value = row;
    createForm.realName = row.realName;
    createForm.phone = row.phone;
    createForm.status = row.status;
    createDialogVisible.value = true;
};
const loadData = async () => {
    loading.value = true;
    try {
        const [userList, roleList] = await Promise.all([fetchAdminUsersApi(), fetchAdminRolesApi(currentOrgId.value)]);
        users.value = userList.map((item) => ({
            ...item,
            roles: dedupeAssignments(item.roles),
        }));
        roles.value = roleList;
        applyQuery();
    }
    finally {
        loading.value = false;
    }
};
const loadManagedScopes = async () => {
    scopeLoading.value = true;
    try {
        const groupList = await fetchAdminGroupsApi();
        groups.value = groupList;
        const storeBuckets = await Promise.all(groupList.map((group) => fetchGroupStoresApi(group.id)));
        stores.value = storeBuckets.flat();
    }
    finally {
        scopeLoading.value = false;
    }
};
const ensureManagedScopesLoaded = async () => {
    if (groups.value.length || stores.value.length) {
        return;
    }
    await loadManagedScopes();
};
const submitUserForm = async () => {
    if (!createForm.realName.trim() || !createForm.phone.trim()) {
        ElMessage.warning('请填写姓名和手机号');
        return;
    }
    submitting.value = true;
    try {
        const payload = {
            realName: createForm.realName.trim(),
            phone: createForm.phone.trim(),
            status: createForm.status,
        };
        if (editingUser.value) {
            await updateAdminUserApi(editingUser.value.id, payload);
            ElMessage.success('用户更新成功');
        }
        else {
            await createAdminUserApi(payload);
            ElMessage.success('新增用户成功');
        }
        createDialogVisible.value = false;
        resetCreateForm();
        await loadData();
    }
    finally {
        submitting.value = false;
    }
};
const handleDeleteUser = async (row) => {
    try {
        await ElMessageBox.confirm(`确认删除用户“${row.realName}”吗？`, '删除确认', { type: 'warning' });
        await deleteAdminUserApi(row.id);
        ElMessage.success('删除成功');
        selectedUserIds.value = selectedUserIds.value.filter((id) => id !== row.id);
        userTableRef.value?.clearSelection?.();
        await loadData();
    }
    catch {
        // Cancelled or failed; global handler already shows API errors.
    }
};
const handleBatchDeleteUsers = async () => {
    if (!selectedUserIds.value.length) {
        ElMessage.warning('请先选择要删除的用户');
        return;
    }
    try {
        await ElMessageBox.confirm(`确认删除已选 ${selectedUserIds.value.length} 位用户吗？`, '批量删除', { type: 'warning' });
        await batchDeleteAdminUsersApi(selectedUserIds.value);
        ElMessage.success('批量删除成功');
        selectedUserIds.value = [];
        userTableRef.value?.clearSelection?.();
        await loadData();
    }
    catch {
        // Cancelled or failed; global handler already shows API errors.
    }
};
const handleStatusChange = async (row, value) => {
    const status = value ? 'ENABLED' : 'DISABLED';
    try {
        await updateAdminUserStatusApi(row.id, status);
        row.status = status;
        ElMessage.success('状态更新成功');
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
const handleSelectionChange = (rows) => {
    selectedUserIds.value = rows.map((item) => item.id);
};
const updateAssignmentScopeByRole = (assignment) => {
    const role = getRoleById(assignment.roleId);
    const scopeType = roleTypeScopeMap[role?.roleType ?? 'PLATFORM'] ?? 'PLATFORM';
    assignment.scopeType = scopeType;
    assignment.scopeId = null;
};
const openAssignDialog = async (row) => {
    selectedUser.value = row;
    await ensureManagedScopesLoaded();
    const rows = dedupeAssignments(row.roles).map((role) => ({
        uid: nextAssignmentUid(),
        roleId: role.roleId,
        scopeType: role.scopeType,
        scopeId: role.scopeId,
    }));
    editableAssignments.value = rows.length ? rows : [buildEmptyAssignment()];
    assignDialogVisible.value = true;
};
const openCopyDialog = async (row) => {
    const storeRoles = dedupeAssignments(row.roles).filter((role) => role.scopeType === 'STORE' && role.scopeId != null && role.builtin);
    if (!storeRoles.length) {
        ElMessage.warning('该用户没有可复制的系统内置门店角色');
        return;
    }
    copiedUser.value = row;
    await ensureManagedScopesLoaded();
    copySourceStoreId.value = storeRoles[0].scopeId;
    copyTargetStoreIds.value = [];
    copyDialogVisible.value = true;
};
const openBulkCopyDialog = async () => {
    if (!selectedUserIds.value.length) {
        ElMessage.warning('请先选择要复制的用户');
        return;
    }
    await ensureManagedScopesLoaded();
    if (!bulkCopySourceUsers.value.length) {
        ElMessage.warning('所选用户不存在');
        return;
    }
    if (!bulkCopySourceStoreOptions.value.length) {
        ElMessage.warning('所选用户没有可批量复制的门店角色');
        return;
    }
    bulkCopySourceStoreId.value = bulkCopySourceStoreOptions.value[0].value;
    bulkCopyTargetStoreIds.value = [];
    bulkCopyDialogVisible.value = true;
};
const getStoreRoleByScopeId = (user, scopeId) => {
    if (!user || scopeId == null) {
        return undefined;
    }
    return dedupeAssignments(user.roles).find((role) => role.scopeType === 'STORE' && role.scopeId === scopeId && role.builtin);
};
const handleCopySourceChange = (value) => {
    const sourceStoreId = Number(value);
    copySourceStoreId.value = sourceStoreId;
    copyTargetStoreIds.value = copyTargetStoreIds.value.filter((storeId) => storeId !== sourceStoreId);
};
const handleCopyTargetChange = (value) => {
    copyTargetStoreIds.value = value.map((item) => Number(item)).filter((item) => item !== copySourceStoreId.value);
};
const handleBulkCopySourceChange = (value) => {
    const sourceStoreId = Number(value);
    bulkCopySourceStoreId.value = sourceStoreId;
    bulkCopyTargetStoreIds.value = bulkCopyTargetStoreIds.value.filter((storeId) => storeId !== sourceStoreId);
};
const handleBulkCopyTargetChange = (value) => {
    bulkCopyTargetStoreIds.value = value.map((item) => Number(item)).filter((item) => item !== bulkCopySourceStoreId.value);
};
const handleCopyToStores = async () => {
    if (!copiedUser.value || copySourceStoreId.value == null) {
        return;
    }
    copying.value = true;
    try {
        const success = await copyStoreRolesToTargets([copiedUser.value], copySourceStoreId.value, copyTargetStoreIds.value);
        if (!success) {
            return;
        }
        ElMessage.success('复制成功');
        copyDialogVisible.value = false;
        await loadData();
    }
    finally {
        copying.value = false;
    }
};
const copyStoreRolesToTargets = async (targetUsers, sourceStoreId, targetStoreIds) => {
    const normalizedTargetStoreIds = Array.from(new Set(targetStoreIds.map((item) => Number(item))))
        .filter((storeId) => storeId !== sourceStoreId);
    if (!normalizedTargetStoreIds.length) {
        ElMessage.warning('请选择至少一个目标门店');
        return false;
    }
    const missingUsers = targetUsers
        .filter((user) => !getStoreRoleByScopeId(user, sourceStoreId))
        .map((user) => user.realName || user.phone || String(user.id));
    if (missingUsers.length) {
        ElMessage.warning(`以下用户没有所选源门店的系统内置角色：${missingUsers.join('、')}`);
        return false;
    }
    for (const user of targetUsers) {
        const sourceRole = getStoreRoleByScopeId(user, sourceStoreId);
        if (!sourceRole) {
            continue;
        }
        const baseAssignments = dedupeAssignments(user.roles)
            .filter((item) => !(item.scopeType === 'STORE' && normalizedTargetStoreIds.includes(item.scopeId ?? -1)))
            .map((item) => ({
            roleId: item.roleId,
            scopeType: item.scopeType,
            scopeId: item.scopeType === 'PLATFORM' ? null : item.scopeId,
        }));
        const copiedAssignments = normalizedTargetStoreIds.map((storeId) => ({
            roleId: sourceRole.roleId,
            scopeType: 'STORE',
            scopeId: storeId,
        }));
        const finalAssignments = dedupeAssignments([...baseAssignments, ...copiedAssignments]);
        await assignAdminUserRolesApi(user.id, finalAssignments);
    }
    return true;
};
const handleBulkCopyToStores = async () => {
    if (!bulkCopySourceUsers.value.length || bulkCopySourceStoreId.value == null) {
        return;
    }
    bulkCopying.value = true;
    try {
        const success = await copyStoreRolesToTargets(bulkCopySourceUsers.value, bulkCopySourceStoreId.value, bulkCopyTargetStoreIds.value);
        if (!success) {
            return;
        }
        ElMessage.success('批量复制成功');
        bulkCopyDialogVisible.value = false;
        selectedUserIds.value = [];
        await loadData();
    }
    finally {
        bulkCopying.value = false;
    }
};
const resetCopyDialog = () => {
    copiedUser.value = null;
    copySourceStoreId.value = null;
    copyTargetStoreIds.value = [];
};
const resetBulkCopyDialog = () => {
    bulkCopySourceStoreId.value = null;
    bulkCopyTargetStoreIds.value = [];
};
const addAssignmentRow = () => {
    editableAssignments.value.push(buildEmptyAssignment());
};
const removeAssignmentRow = (uid) => {
    editableAssignments.value = editableAssignments.value.filter((item) => item.uid !== uid);
    if (!editableAssignments.value.length) {
        editableAssignments.value = [buildEmptyAssignment()];
    }
};
const handleAssignmentRoleChange = (row, value) => {
    row.roleId = Number(value);
    updateAssignmentScopeByRole(row);
};
const handleAssignRoles = async () => {
    if (!selectedUser.value) {
        return;
    }
    const assignments = editableAssignments.value
        .filter((item) => item.roleId != null)
        .map((item) => ({
        roleId: Number(item.roleId),
        scopeType: item.scopeType,
        scopeId: item.scopeType === 'PLATFORM' ? null : item.scopeId,
    }));
    if (!assignments.length) {
        ElMessage.warning('请至少选择一个角色');
        return;
    }
    const missingScope = assignments.find((item) => item.scopeType !== 'PLATFORM' && item.scopeId == null);
    if (missingScope) {
        ElMessage.warning('集团/门店角色必须指定作用域');
        return;
    }
    const deduped = Array.from(new Map(assignments.map((item) => [roleAssignmentKey(item), item])).values());
    if (deduped.length !== assignments.length) {
        ElMessage.warning('同一用户同一门店只能分配一个角色');
        return;
    }
    assigning.value = true;
    try {
        await assignAdminUserRolesApi(selectedUser.value.id, deduped);
        ElMessage.success('角色分配成功');
        assignDialogVisible.value = false;
        await loadData();
    }
    finally {
        assigning.value = false;
    }
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
onMounted(() => {
    loadManagedScopes();
    loadData();
});
watch(() => sessionStore.currentOrgId, () => {
    loadManagedScopes();
    loadData();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-grid single" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
const __VLS_0 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    model: (__VLS_ctx.queryForm),
    inline: (true),
    ...{ class: "filter-bar compact-filter-bar" },
}));
const __VLS_2 = __VLS_1({
    model: (__VLS_ctx.queryForm),
    inline: (true),
    ...{ class: "filter-bar compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    label: "姓名查询",
}));
const __VLS_6 = __VLS_5({
    label: "姓名查询",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    modelValue: (__VLS_ctx.queryForm.realName),
    placeholder: "请输入姓名",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_10 = __VLS_9({
    modelValue: (__VLS_ctx.queryForm.realName),
    placeholder: "请输入姓名",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
var __VLS_7;
const __VLS_12 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    label: "手机号查询",
}));
const __VLS_14 = __VLS_13({
    label: "手机号查询",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    modelValue: (__VLS_ctx.queryForm.phone),
    placeholder: "请输入手机号",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_18 = __VLS_17({
    modelValue: (__VLS_ctx.queryForm.phone),
    placeholder: "请输入手机号",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
var __VLS_15;
const __VLS_20 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    label: "角色查询",
}));
const __VLS_22 = __VLS_21({
    label: "角色查询",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_23.slots.default;
const __VLS_24 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    modelValue: (__VLS_ctx.queryForm.roleId),
    placeholder: "请选择角色",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_26 = __VLS_25({
    modelValue: (__VLS_ctx.queryForm.roleId),
    placeholder: "请选择角色",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
__VLS_27.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleOptions))) {
    const __VLS_28 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }));
    const __VLS_30 = __VLS_29({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
}
var __VLS_27;
var __VLS_23;
const __VLS_32 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    label: "门店查询",
}));
const __VLS_34 = __VLS_33({
    label: "门店查询",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_35.slots.default;
const __VLS_36 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    modelValue: (__VLS_ctx.queryForm.storeId),
    placeholder: "请选择门店",
    clearable: true,
    filterable: true,
    ...{ style: {} },
}));
const __VLS_38 = __VLS_37({
    modelValue: (__VLS_ctx.queryForm.storeId),
    placeholder: "请选择门店",
    clearable: true,
    filterable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.storeScopeOptions))) {
    const __VLS_40 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }));
    const __VLS_42 = __VLS_41({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_41));
}
var __VLS_39;
var __VLS_35;
const __VLS_44 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({}));
const __VLS_46 = __VLS_45({}, ...__VLS_functionalComponentArgsRest(__VLS_45));
__VLS_47.slots.default;
const __VLS_48 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_50 = __VLS_49({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
let __VLS_52;
let __VLS_53;
let __VLS_54;
const __VLS_55 = {
    onClick: (__VLS_ctx.applyQuery)
};
__VLS_51.slots.default;
var __VLS_51;
const __VLS_56 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    ...{ 'onClick': {} },
}));
const __VLS_58 = __VLS_57({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
let __VLS_60;
let __VLS_61;
let __VLS_62;
const __VLS_63 = {
    onClick: (__VLS_ctx.resetQuery)
};
__VLS_59.slots.default;
var __VLS_59;
var __VLS_47;
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_64 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_66 = __VLS_65({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
let __VLS_68;
let __VLS_69;
let __VLS_70;
const __VLS_71 = {
    onClick: (__VLS_ctx.openCreateDialog)
};
__VLS_67.slots.default;
var __VLS_67;
const __VLS_72 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    ...{ 'onClick': {} },
    type: "danger",
    disabled: (!__VLS_ctx.selectedUserIds.length),
}));
const __VLS_74 = __VLS_73({
    ...{ 'onClick': {} },
    type: "danger",
    disabled: (!__VLS_ctx.selectedUserIds.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
let __VLS_76;
let __VLS_77;
let __VLS_78;
const __VLS_79 = {
    onClick: (__VLS_ctx.handleBatchDeleteUsers)
};
__VLS_75.slots.default;
var __VLS_75;
const __VLS_80 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    ...{ 'onClick': {} },
    type: "primary",
    disabled: (!__VLS_ctx.selectedUserIds.length),
}));
const __VLS_82 = __VLS_81({
    ...{ 'onClick': {} },
    type: "primary",
    disabled: (!__VLS_ctx.selectedUserIds.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
let __VLS_84;
let __VLS_85;
let __VLS_86;
const __VLS_87 = {
    onClick: (__VLS_ctx.openBulkCopyDialog)
};
__VLS_83.slots.default;
var __VLS_83;
const __VLS_88 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
    ...{ 'onSelectionChange': {} },
    ref: "userTableRef",
    data: (__VLS_ctx.pagedUsers),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    rowKey: "id",
}));
const __VLS_90 = __VLS_89({
    ...{ 'onSelectionChange': {} },
    ref: "userTableRef",
    data: (__VLS_ctx.pagedUsers),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    rowKey: "id",
}, ...__VLS_functionalComponentArgsRest(__VLS_89));
let __VLS_92;
let __VLS_93;
let __VLS_94;
const __VLS_95 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
/** @type {typeof __VLS_ctx.userTableRef} */ ;
var __VLS_96 = {};
__VLS_91.slots.default;
const __VLS_98 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    type: "selection",
    width: "48",
    reserveSelection: true,
}));
const __VLS_100 = __VLS_99({
    type: "selection",
    width: "48",
    reserveSelection: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
const __VLS_102 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    prop: "id",
    label: "ID",
    width: "80",
}));
const __VLS_104 = __VLS_103({
    prop: "id",
    label: "ID",
    width: "80",
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
const __VLS_106 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    prop: "username",
    label: "用户编码",
    minWidth: "160",
}));
const __VLS_108 = __VLS_107({
    prop: "username",
    label: "用户编码",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
const __VLS_110 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    prop: "realName",
    label: "姓名",
    minWidth: "140",
}));
const __VLS_112 = __VLS_111({
    prop: "realName",
    label: "姓名",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
const __VLS_114 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
    prop: "phone",
    label: "手机号",
    minWidth: "140",
}));
const __VLS_116 = __VLS_115({
    prop: "phone",
    label: "手机号",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_115));
const __VLS_118 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    label: "拥有角色",
    minWidth: "280",
}));
const __VLS_120 = __VLS_119({
    label: "拥有角色",
    minWidth: "280",
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
__VLS_121.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_121.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_122 = {}.ElSpace;
    /** @type {[typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, ]} */ ;
    // @ts-ignore
    const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
        wrap: true,
    }));
    const __VLS_124 = __VLS_123({
        wrap: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_123));
    __VLS_125.slots.default;
    for (const [role] of __VLS_getVForSourceType((row.roles))) {
        const __VLS_126 = {}.ElTag;
        /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
        // @ts-ignore
        const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
            key: (`${role.roleId}-${role.scopeType}-${role.scopeId ?? 'null'}`),
            size: "small",
        }));
        const __VLS_128 = __VLS_127({
            key: (`${role.roleId}-${role.scopeType}-${role.scopeId ?? 'null'}`),
            size: "small",
        }, ...__VLS_functionalComponentArgsRest(__VLS_127));
        __VLS_129.slots.default;
        (role.roleName);
        (role.scopeName ? ` / ${role.scopeName}` : '');
        var __VLS_129;
    }
    if (!row.roles.length) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
    var __VLS_125;
}
var __VLS_121;
const __VLS_130 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    label: "状态",
    width: "110",
}));
const __VLS_132 = __VLS_131({
    label: "状态",
    width: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
__VLS_133.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_133.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_134 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
        ...{ 'onChange': {} },
        modelValue: (row.status === 'ENABLED'),
    }));
    const __VLS_136 = __VLS_135({
        ...{ 'onChange': {} },
        modelValue: (row.status === 'ENABLED'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_135));
    let __VLS_138;
    let __VLS_139;
    let __VLS_140;
    const __VLS_141 = {
        onChange: (...[$event]) => {
            __VLS_ctx.handleStatusChange(row, $event);
        }
    };
    var __VLS_137;
}
var __VLS_133;
const __VLS_142 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    label: "创建时间",
    minWidth: "180",
}));
const __VLS_144 = __VLS_143({
    label: "创建时间",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
__VLS_145.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_145.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatDateTime(row.createdAt));
}
var __VLS_145;
const __VLS_146 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    label: "操作",
    width: "220",
    fixed: "right",
}));
const __VLS_148 = __VLS_147({
    label: "操作",
    width: "220",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_149.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_150 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }));
    const __VLS_152 = __VLS_151({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_151));
    let __VLS_154;
    let __VLS_155;
    let __VLS_156;
    const __VLS_157 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_153.slots.default;
    var __VLS_153;
    const __VLS_158 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }));
    const __VLS_160 = __VLS_159({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_159));
    let __VLS_162;
    let __VLS_163;
    let __VLS_164;
    const __VLS_165 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDeleteUser(row);
        }
    };
    __VLS_161.slots.default;
    var __VLS_161;
    const __VLS_166 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }));
    const __VLS_168 = __VLS_167({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_167));
    let __VLS_170;
    let __VLS_171;
    let __VLS_172;
    const __VLS_173 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openAssignDialog(row);
        }
    };
    __VLS_169.slots.default;
    var __VLS_169;
    const __VLS_174 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }));
    const __VLS_176 = __VLS_175({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_175));
    let __VLS_178;
    let __VLS_179;
    let __VLS_180;
    const __VLS_181 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openCopyDialog(row);
        }
    };
    __VLS_177.slots.default;
    var __VLS_177;
}
var __VLS_149;
var __VLS_91;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_182 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedUserIds.length),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredUsers.length),
}));
const __VLS_183 = __VLS_182({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (__VLS_ctx.selectedUserIds.length),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredUsers.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_182));
let __VLS_185;
let __VLS_186;
let __VLS_187;
const __VLS_188 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_189 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_184;
const __VLS_190 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.userDialogTitle),
    width: "420px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_192 = __VLS_191({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.userDialogTitle),
    width: "420px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_191));
let __VLS_194;
let __VLS_195;
let __VLS_196;
const __VLS_197 = {
    onClosed: (__VLS_ctx.resetCreateForm)
};
__VLS_193.slots.default;
const __VLS_198 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_200 = __VLS_199({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_199));
__VLS_201.slots.default;
const __VLS_202 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
    label: "姓名",
    required: true,
}));
const __VLS_204 = __VLS_203({
    label: "姓名",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_203));
__VLS_205.slots.default;
const __VLS_206 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_207 = __VLS_asFunctionalComponent(__VLS_206, new __VLS_206({
    modelValue: (__VLS_ctx.createForm.realName),
    maxlength: "32",
}));
const __VLS_208 = __VLS_207({
    modelValue: (__VLS_ctx.createForm.realName),
    maxlength: "32",
}, ...__VLS_functionalComponentArgsRest(__VLS_207));
var __VLS_205;
const __VLS_210 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
    label: "手机号",
    required: true,
}));
const __VLS_212 = __VLS_211({
    label: "手机号",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_211));
__VLS_213.slots.default;
const __VLS_214 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
    modelValue: (__VLS_ctx.createForm.phone),
    maxlength: "20",
}));
const __VLS_216 = __VLS_215({
    modelValue: (__VLS_ctx.createForm.phone),
    maxlength: "20",
}, ...__VLS_functionalComponentArgsRest(__VLS_215));
var __VLS_213;
const __VLS_218 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
    label: "状态",
}));
const __VLS_220 = __VLS_219({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_219));
__VLS_221.slots.default;
const __VLS_222 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
    modelValue: (__VLS_ctx.createForm.status),
    ...{ style: {} },
}));
const __VLS_224 = __VLS_223({
    modelValue: (__VLS_ctx.createForm.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_223));
__VLS_225.slots.default;
const __VLS_226 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_228 = __VLS_227({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_227));
const __VLS_230 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_232 = __VLS_231({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_231));
var __VLS_225;
var __VLS_221;
var __VLS_201;
{
    const { footer: __VLS_thisSlot } = __VLS_193.slots;
    const __VLS_234 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_235 = __VLS_asFunctionalComponent(__VLS_234, new __VLS_234({
        ...{ 'onClick': {} },
    }));
    const __VLS_236 = __VLS_235({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_235));
    let __VLS_238;
    let __VLS_239;
    let __VLS_240;
    const __VLS_241 = {
        onClick: (...[$event]) => {
            __VLS_ctx.createDialogVisible = false;
        }
    };
    __VLS_237.slots.default;
    var __VLS_237;
    const __VLS_242 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_243 = __VLS_asFunctionalComponent(__VLS_242, new __VLS_242({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.submitting),
    }));
    const __VLS_244 = __VLS_243({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.submitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_243));
    let __VLS_246;
    let __VLS_247;
    let __VLS_248;
    const __VLS_249 = {
        onClick: (__VLS_ctx.submitUserForm)
    };
    __VLS_245.slots.default;
    (__VLS_ctx.editingUser ? '更新' : '保存');
    var __VLS_245;
}
var __VLS_193;
const __VLS_250 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_251 = __VLS_asFunctionalComponent(__VLS_250, new __VLS_250({
    modelValue: (__VLS_ctx.assignDialogVisible),
    title: "分配角色",
    width: "840px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_252 = __VLS_251({
    modelValue: (__VLS_ctx.assignDialogVisible),
    title: "分配角色",
    width: "840px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_251));
__VLS_253.slots.default;
const __VLS_254 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_255 = __VLS_asFunctionalComponent(__VLS_254, new __VLS_254({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_256 = __VLS_255({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_255));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.scopeLoading) }, null, null);
__VLS_257.slots.default;
const __VLS_258 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_259 = __VLS_asFunctionalComponent(__VLS_258, new __VLS_258({
    label: "用户",
}));
const __VLS_260 = __VLS_259({
    label: "用户",
}, ...__VLS_functionalComponentArgsRest(__VLS_259));
__VLS_261.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.selectedUser?.realName);
(__VLS_ctx.selectedUser?.phone);
var __VLS_261;
const __VLS_262 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_263 = __VLS_asFunctionalComponent(__VLS_262, new __VLS_262({
    label: "授权明细",
}));
const __VLS_264 = __VLS_263({
    label: "授权明细",
}, ...__VLS_functionalComponentArgsRest(__VLS_263));
__VLS_265.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_266 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_267 = __VLS_asFunctionalComponent(__VLS_266, new __VLS_266({
    data: (__VLS_ctx.editableAssignments),
    border: true,
    size: "small",
}));
const __VLS_268 = __VLS_267({
    data: (__VLS_ctx.editableAssignments),
    border: true,
    size: "small",
}, ...__VLS_functionalComponentArgsRest(__VLS_267));
__VLS_269.slots.default;
const __VLS_270 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_271 = __VLS_asFunctionalComponent(__VLS_270, new __VLS_270({
    label: "角色",
    minWidth: "280",
}));
const __VLS_272 = __VLS_271({
    label: "角色",
    minWidth: "280",
}, ...__VLS_functionalComponentArgsRest(__VLS_271));
__VLS_273.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_273.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_274 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_275 = __VLS_asFunctionalComponent(__VLS_274, new __VLS_274({
        ...{ 'onChange': {} },
        modelValue: (row.roleId),
        placeholder: "请选择角色",
        filterable: true,
        ...{ style: {} },
    }));
    const __VLS_276 = __VLS_275({
        ...{ 'onChange': {} },
        modelValue: (row.roleId),
        placeholder: "请选择角色",
        filterable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_275));
    let __VLS_278;
    let __VLS_279;
    let __VLS_280;
    const __VLS_281 = {
        onChange: (...[$event]) => {
            __VLS_ctx.handleAssignmentRoleChange(row, $event);
        }
    };
    __VLS_277.slots.default;
    for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleOptions))) {
        const __VLS_282 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_283 = __VLS_asFunctionalComponent(__VLS_282, new __VLS_282({
            key: (item.value),
            label: (item.label),
            value: (item.value),
        }));
        const __VLS_284 = __VLS_283({
            key: (item.value),
            label: (item.label),
            value: (item.value),
        }, ...__VLS_functionalComponentArgsRest(__VLS_283));
    }
    var __VLS_277;
}
var __VLS_273;
const __VLS_286 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_287 = __VLS_asFunctionalComponent(__VLS_286, new __VLS_286({
    label: "作用域",
    minWidth: "260",
}));
const __VLS_288 = __VLS_287({
    label: "作用域",
    minWidth: "260",
}, ...__VLS_functionalComponentArgsRest(__VLS_287));
__VLS_289.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_289.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    if (row.scopeType === 'GROUP') {
        const __VLS_290 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_291 = __VLS_asFunctionalComponent(__VLS_290, new __VLS_290({
            modelValue: (row.scopeId),
            placeholder: "请选择集团",
            filterable: true,
            ...{ style: {} },
        }));
        const __VLS_292 = __VLS_291({
            modelValue: (row.scopeId),
            placeholder: "请选择集团",
            filterable: true,
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_291));
        __VLS_293.slots.default;
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.groupScopeOptions))) {
            const __VLS_294 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_295 = __VLS_asFunctionalComponent(__VLS_294, new __VLS_294({
                key: (item.value),
                label: (item.label),
                value: (item.value),
            }));
            const __VLS_296 = __VLS_295({
                key: (item.value),
                label: (item.label),
                value: (item.value),
            }, ...__VLS_functionalComponentArgsRest(__VLS_295));
        }
        var __VLS_293;
    }
    else if (row.scopeType === 'STORE') {
        const __VLS_298 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_299 = __VLS_asFunctionalComponent(__VLS_298, new __VLS_298({
            modelValue: (row.scopeId),
            placeholder: "请选择门店",
            filterable: true,
            ...{ style: {} },
        }));
        const __VLS_300 = __VLS_299({
            modelValue: (row.scopeId),
            placeholder: "请选择门店",
            filterable: true,
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_299));
        __VLS_301.slots.default;
        for (const [item] of __VLS_getVForSourceType((__VLS_ctx.storeScopeOptions))) {
            const __VLS_302 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_303 = __VLS_asFunctionalComponent(__VLS_302, new __VLS_302({
                key: (item.value),
                label: (item.label),
                value: (item.value),
            }));
            const __VLS_304 = __VLS_303({
                key: (item.value),
                label: (item.label),
                value: (item.value),
            }, ...__VLS_functionalComponentArgsRest(__VLS_303));
        }
        var __VLS_301;
    }
    else {
        const __VLS_306 = {}.ElInput;
        /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
        // @ts-ignore
        const __VLS_307 = __VLS_asFunctionalComponent(__VLS_306, new __VLS_306({
            modelValue: "平台",
            disabled: true,
        }));
        const __VLS_308 = __VLS_307({
            modelValue: "平台",
            disabled: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_307));
    }
}
var __VLS_289;
const __VLS_310 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_311 = __VLS_asFunctionalComponent(__VLS_310, new __VLS_310({
    label: "操作",
    width: "100",
    align: "center",
}));
const __VLS_312 = __VLS_311({
    label: "操作",
    width: "100",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_311));
__VLS_313.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_313.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_314 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_315 = __VLS_asFunctionalComponent(__VLS_314, new __VLS_314({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }));
    const __VLS_316 = __VLS_315({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_315));
    let __VLS_318;
    let __VLS_319;
    let __VLS_320;
    const __VLS_321 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeAssignmentRow(row.uid);
        }
    };
    __VLS_317.slots.default;
    var __VLS_317;
}
var __VLS_313;
var __VLS_269;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_322 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_323 = __VLS_asFunctionalComponent(__VLS_322, new __VLS_322({
    ...{ 'onClick': {} },
    plain: true,
    type: "primary",
}));
const __VLS_324 = __VLS_323({
    ...{ 'onClick': {} },
    plain: true,
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_323));
let __VLS_326;
let __VLS_327;
let __VLS_328;
const __VLS_329 = {
    onClick: (__VLS_ctx.addAssignmentRow)
};
__VLS_325.slots.default;
var __VLS_325;
var __VLS_265;
var __VLS_257;
{
    const { footer: __VLS_thisSlot } = __VLS_253.slots;
    const __VLS_330 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_331 = __VLS_asFunctionalComponent(__VLS_330, new __VLS_330({
        ...{ 'onClick': {} },
    }));
    const __VLS_332 = __VLS_331({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_331));
    let __VLS_334;
    let __VLS_335;
    let __VLS_336;
    const __VLS_337 = {
        onClick: (...[$event]) => {
            __VLS_ctx.assignDialogVisible = false;
        }
    };
    __VLS_333.slots.default;
    var __VLS_333;
    const __VLS_338 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_339 = __VLS_asFunctionalComponent(__VLS_338, new __VLS_338({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.assigning),
    }));
    const __VLS_340 = __VLS_339({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.assigning),
    }, ...__VLS_functionalComponentArgsRest(__VLS_339));
    let __VLS_342;
    let __VLS_343;
    let __VLS_344;
    const __VLS_345 = {
        onClick: (__VLS_ctx.handleAssignRoles)
    };
    __VLS_341.slots.default;
    var __VLS_341;
}
var __VLS_253;
const __VLS_346 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_347 = __VLS_asFunctionalComponent(__VLS_346, new __VLS_346({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.copyDialogVisible),
    title: "复制到其它门店",
    width: "760px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_348 = __VLS_347({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.copyDialogVisible),
    title: "复制到其它门店",
    width: "760px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_347));
let __VLS_350;
let __VLS_351;
let __VLS_352;
const __VLS_353 = {
    onClosed: (__VLS_ctx.resetCopyDialog)
};
__VLS_349.slots.default;
const __VLS_354 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_355 = __VLS_asFunctionalComponent(__VLS_354, new __VLS_354({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_356 = __VLS_355({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_355));
__VLS_357.slots.default;
const __VLS_358 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_359 = __VLS_asFunctionalComponent(__VLS_358, new __VLS_358({
    label: "用户",
}));
const __VLS_360 = __VLS_359({
    label: "用户",
}, ...__VLS_functionalComponentArgsRest(__VLS_359));
__VLS_361.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.copiedUser?.realName);
(__VLS_ctx.copiedUser?.phone);
var __VLS_361;
const __VLS_362 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_363 = __VLS_asFunctionalComponent(__VLS_362, new __VLS_362({
    label: "源门店",
}));
const __VLS_364 = __VLS_363({
    label: "源门店",
}, ...__VLS_functionalComponentArgsRest(__VLS_363));
__VLS_365.slots.default;
const __VLS_366 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_367 = __VLS_asFunctionalComponent(__VLS_366, new __VLS_366({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.copySourceStoreId),
    placeholder: "请选择源门店",
    filterable: true,
    ...{ style: {} },
}));
const __VLS_368 = __VLS_367({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.copySourceStoreId),
    placeholder: "请选择源门店",
    filterable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_367));
let __VLS_370;
let __VLS_371;
let __VLS_372;
const __VLS_373 = {
    onChange: (__VLS_ctx.handleCopySourceChange)
};
__VLS_369.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.copySourceStoreOptions))) {
    const __VLS_374 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_375 = __VLS_asFunctionalComponent(__VLS_374, new __VLS_374({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }));
    const __VLS_376 = __VLS_375({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_375));
}
var __VLS_369;
var __VLS_365;
const __VLS_378 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_379 = __VLS_asFunctionalComponent(__VLS_378, new __VLS_378({
    label: "目标门店",
}));
const __VLS_380 = __VLS_379({
    label: "目标门店",
}, ...__VLS_functionalComponentArgsRest(__VLS_379));
__VLS_381.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_382 = {}.ElCheckboxGroup;
/** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
// @ts-ignore
const __VLS_383 = __VLS_asFunctionalComponent(__VLS_382, new __VLS_382({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.copyTargetStoreIds),
}));
const __VLS_384 = __VLS_383({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.copyTargetStoreIds),
}, ...__VLS_functionalComponentArgsRest(__VLS_383));
let __VLS_386;
let __VLS_387;
let __VLS_388;
const __VLS_389 = {
    onChange: (__VLS_ctx.handleCopyTargetChange)
};
__VLS_385.slots.default;
const __VLS_390 = {}.ElSpace;
/** @type {[typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, ]} */ ;
// @ts-ignore
const __VLS_391 = __VLS_asFunctionalComponent(__VLS_390, new __VLS_390({
    wrap: true,
}));
const __VLS_392 = __VLS_391({
    wrap: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_391));
__VLS_393.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.copyTargetStoreOptions))) {
    const __VLS_394 = {}.ElCheckbox;
    /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
    // @ts-ignore
    const __VLS_395 = __VLS_asFunctionalComponent(__VLS_394, new __VLS_394({
        key: (item.value),
        label: (item.value),
    }));
    const __VLS_396 = __VLS_395({
        key: (item.value),
        label: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_395));
    __VLS_397.slots.default;
    (item.label);
    var __VLS_397;
}
var __VLS_393;
var __VLS_385;
var __VLS_381;
var __VLS_357;
{
    const { footer: __VLS_thisSlot } = __VLS_349.slots;
    const __VLS_398 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_399 = __VLS_asFunctionalComponent(__VLS_398, new __VLS_398({
        ...{ 'onClick': {} },
    }));
    const __VLS_400 = __VLS_399({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_399));
    let __VLS_402;
    let __VLS_403;
    let __VLS_404;
    const __VLS_405 = {
        onClick: (...[$event]) => {
            __VLS_ctx.copyDialogVisible = false;
        }
    };
    __VLS_401.slots.default;
    var __VLS_401;
    const __VLS_406 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_407 = __VLS_asFunctionalComponent(__VLS_406, new __VLS_406({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.copying),
    }));
    const __VLS_408 = __VLS_407({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.copying),
    }, ...__VLS_functionalComponentArgsRest(__VLS_407));
    let __VLS_410;
    let __VLS_411;
    let __VLS_412;
    const __VLS_413 = {
        onClick: (__VLS_ctx.handleCopyToStores)
    };
    __VLS_409.slots.default;
    var __VLS_409;
}
var __VLS_349;
const __VLS_414 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_415 = __VLS_asFunctionalComponent(__VLS_414, new __VLS_414({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.bulkCopyDialogVisible),
    title: "批量复制到门店",
    width: "760px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_416 = __VLS_415({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.bulkCopyDialogVisible),
    title: "批量复制到门店",
    width: "760px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_415));
let __VLS_418;
let __VLS_419;
let __VLS_420;
const __VLS_421 = {
    onClosed: (__VLS_ctx.resetBulkCopyDialog)
};
__VLS_417.slots.default;
const __VLS_422 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_423 = __VLS_asFunctionalComponent(__VLS_422, new __VLS_422({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_424 = __VLS_423({
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_423));
__VLS_425.slots.default;
const __VLS_426 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_427 = __VLS_asFunctionalComponent(__VLS_426, new __VLS_426({
    label: "已选用户",
}));
const __VLS_428 = __VLS_427({
    label: "已选用户",
}, ...__VLS_functionalComponentArgsRest(__VLS_427));
__VLS_429.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.selectedUserIds.length);
var __VLS_429;
const __VLS_430 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_431 = __VLS_asFunctionalComponent(__VLS_430, new __VLS_430({
    label: "源门店",
}));
const __VLS_432 = __VLS_431({
    label: "源门店",
}, ...__VLS_functionalComponentArgsRest(__VLS_431));
__VLS_433.slots.default;
const __VLS_434 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_435 = __VLS_asFunctionalComponent(__VLS_434, new __VLS_434({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bulkCopySourceStoreId),
    placeholder: "请选择源门店",
    filterable: true,
    ...{ style: {} },
}));
const __VLS_436 = __VLS_435({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bulkCopySourceStoreId),
    placeholder: "请选择源门店",
    filterable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_435));
let __VLS_438;
let __VLS_439;
let __VLS_440;
const __VLS_441 = {
    onChange: (__VLS_ctx.handleBulkCopySourceChange)
};
__VLS_437.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.bulkCopySourceStoreOptions))) {
    const __VLS_442 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_443 = __VLS_asFunctionalComponent(__VLS_442, new __VLS_442({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }));
    const __VLS_444 = __VLS_443({
        key: (item.value),
        label: (item.label),
        value: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_443));
}
var __VLS_437;
var __VLS_433;
const __VLS_446 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_447 = __VLS_asFunctionalComponent(__VLS_446, new __VLS_446({
    label: "目标门店",
}));
const __VLS_448 = __VLS_447({
    label: "目标门店",
}, ...__VLS_functionalComponentArgsRest(__VLS_447));
__VLS_449.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_450 = {}.ElCheckboxGroup;
/** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
// @ts-ignore
const __VLS_451 = __VLS_asFunctionalComponent(__VLS_450, new __VLS_450({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bulkCopyTargetStoreIds),
}));
const __VLS_452 = __VLS_451({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bulkCopyTargetStoreIds),
}, ...__VLS_functionalComponentArgsRest(__VLS_451));
let __VLS_454;
let __VLS_455;
let __VLS_456;
const __VLS_457 = {
    onChange: (__VLS_ctx.handleBulkCopyTargetChange)
};
__VLS_453.slots.default;
const __VLS_458 = {}.ElSpace;
/** @type {[typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, ]} */ ;
// @ts-ignore
const __VLS_459 = __VLS_asFunctionalComponent(__VLS_458, new __VLS_458({
    wrap: true,
}));
const __VLS_460 = __VLS_459({
    wrap: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_459));
__VLS_461.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.bulkCopyTargetStoreOptions))) {
    const __VLS_462 = {}.ElCheckbox;
    /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
    // @ts-ignore
    const __VLS_463 = __VLS_asFunctionalComponent(__VLS_462, new __VLS_462({
        key: (item.value),
        label: (item.value),
    }));
    const __VLS_464 = __VLS_463({
        key: (item.value),
        label: (item.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_463));
    __VLS_465.slots.default;
    (item.label);
    var __VLS_465;
}
var __VLS_461;
var __VLS_453;
var __VLS_449;
var __VLS_425;
{
    const { footer: __VLS_thisSlot } = __VLS_417.slots;
    const __VLS_466 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_467 = __VLS_asFunctionalComponent(__VLS_466, new __VLS_466({
        ...{ 'onClick': {} },
    }));
    const __VLS_468 = __VLS_467({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_467));
    let __VLS_470;
    let __VLS_471;
    let __VLS_472;
    const __VLS_473 = {
        onClick: (...[$event]) => {
            __VLS_ctx.bulkCopyDialogVisible = false;
        }
    };
    __VLS_469.slots.default;
    var __VLS_469;
    const __VLS_474 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_475 = __VLS_asFunctionalComponent(__VLS_474, new __VLS_474({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bulkCopying),
    }));
    const __VLS_476 = __VLS_475({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bulkCopying),
    }, ...__VLS_functionalComponentArgsRest(__VLS_475));
    let __VLS_478;
    let __VLS_479;
    let __VLS_480;
    const __VLS_481 = {
        onClick: (__VLS_ctx.handleBulkCopyToStores)
    };
    __VLS_477.slots.default;
    var __VLS_477;
}
var __VLS_417;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
// @ts-ignore
var __VLS_97 = __VLS_96;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemPaginationSection: ItemPaginationSection,
            loading: loading,
            scopeLoading: scopeLoading,
            filteredUsers: filteredUsers,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedUserIds: selectedUserIds,
            userTableRef: userTableRef,
            createDialogVisible: createDialogVisible,
            assignDialogVisible: assignDialogVisible,
            copyDialogVisible: copyDialogVisible,
            bulkCopyDialogVisible: bulkCopyDialogVisible,
            submitting: submitting,
            assigning: assigning,
            copying: copying,
            bulkCopying: bulkCopying,
            selectedUser: selectedUser,
            editingUser: editingUser,
            copiedUser: copiedUser,
            editableAssignments: editableAssignments,
            copySourceStoreId: copySourceStoreId,
            copyTargetStoreIds: copyTargetStoreIds,
            bulkCopySourceStoreId: bulkCopySourceStoreId,
            bulkCopyTargetStoreIds: bulkCopyTargetStoreIds,
            createForm: createForm,
            queryForm: queryForm,
            roleOptions: roleOptions,
            userDialogTitle: userDialogTitle,
            pagedUsers: pagedUsers,
            groupScopeOptions: groupScopeOptions,
            storeScopeOptions: storeScopeOptions,
            formatDateTime: formatDateTime,
            copySourceStoreOptions: copySourceStoreOptions,
            bulkCopySourceStoreOptions: bulkCopySourceStoreOptions,
            copyTargetStoreOptions: copyTargetStoreOptions,
            bulkCopyTargetStoreOptions: bulkCopyTargetStoreOptions,
            applyQuery: applyQuery,
            resetQuery: resetQuery,
            resetCreateForm: resetCreateForm,
            openCreateDialog: openCreateDialog,
            openEditDialog: openEditDialog,
            submitUserForm: submitUserForm,
            handleDeleteUser: handleDeleteUser,
            handleBatchDeleteUsers: handleBatchDeleteUsers,
            handleStatusChange: handleStatusChange,
            handleSelectionChange: handleSelectionChange,
            openAssignDialog: openAssignDialog,
            openCopyDialog: openCopyDialog,
            openBulkCopyDialog: openBulkCopyDialog,
            handleCopySourceChange: handleCopySourceChange,
            handleCopyTargetChange: handleCopyTargetChange,
            handleBulkCopySourceChange: handleBulkCopySourceChange,
            handleBulkCopyTargetChange: handleBulkCopyTargetChange,
            handleCopyToStores: handleCopyToStores,
            handleBulkCopyToStores: handleBulkCopyToStores,
            resetCopyDialog: resetCopyDialog,
            resetBulkCopyDialog: resetBulkCopyDialog,
            addAssignmentRow: addAssignmentRow,
            removeAssignmentRow: removeAssignmentRow,
            handleAssignmentRoleChange: handleAssignmentRoleChange,
            handleAssignRoles: handleAssignRoles,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
