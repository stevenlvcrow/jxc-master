/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { assignAdminRoleMenusApi, fetchAdminMenusApi, fetchAdminRolesApi, } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const loading = ref(false);
const saving = ref(false);
const roles = ref([]);
const menus = ref([]);
const selectedRoleId = ref();
const checkedMenuIds = ref([]);
const menuTreeRef = ref();
const route = useRoute();
const sessionStore = useSessionStore();
const currentOrgId = computed(() => {
    const orgId = sessionStore.currentOrgId?.trim();
    return orgId ? orgId : undefined;
});
const isGroupMenuPermissionPage = computed(() => route.path.startsWith('/group/'));
const filteredRoles = computed(() => {
    if (!isGroupMenuPermissionPage.value) {
        return roles.value;
    }
    return roles.value.filter((item) => {
        if (item.roleCode === 'GROUP_ADMIN') {
            return false;
        }
        return item.roleType === 'GROUP' || item.roleType === 'STORE';
    });
});
const selectedRole = computed(() => filteredRoles.value.find((item) => item.id === selectedRoleId.value));
const visibleMenus = computed(() => {
    const roleType = selectedRole.value?.roleType;
    if (isGroupMenuPermissionPage.value) {
        if (roleType === 'STORE') {
            return menus.value.filter((item) => item.menuCode.startsWith('STORE_BIZ_'));
        }
        if (roleType === 'GROUP') {
            return menus.value.filter((item) => item.menuCode.startsWith('GROUP_'));
        }
        return [];
    }
    if (roleType === 'STORE') {
        return menus.value.filter((item) => item.menuCode.startsWith('STORE_BIZ_'));
    }
    if (roleType === 'GROUP') {
        return menus.value.filter((item) => !item.menuCode.startsWith('STORE_BIZ_'));
    }
    return menus.value;
});
const menuTreeData = computed(() => {
    const grouped = new Map();
    visibleMenus.value.forEach((item) => {
        const key = item.parentId ?? null;
        const list = grouped.get(key) ?? [];
        list.push(item);
        grouped.set(key, list);
    });
    const build = (parentId) => {
        const children = grouped.get(parentId) ?? [];
        children.sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0));
        return children.map((item) => ({
            id: item.id,
            label: item.menuName,
            menuCode: item.menuCode,
            menuType: item.menuType,
            children: build(item.id),
        }));
    };
    return [{
            id: -1,
            label: '全部菜单',
            menuCode: 'ALL_MENUS',
            menuType: 'ROOT',
            children: build(null),
        }];
});
const refreshData = async () => {
    loading.value = true;
    try {
        const [roleList, menuList] = await Promise.all([
            fetchAdminRolesApi(currentOrgId.value),
            fetchAdminMenusApi(currentOrgId.value),
        ]);
        roles.value = roleList;
        menus.value = menuList;
        if (!selectedRoleId.value && filteredRoles.value.length > 0) {
            selectedRoleId.value = filteredRoles.value[0].id;
        }
    }
    finally {
        loading.value = false;
    }
};
watch(filteredRoles, (nextRoles) => {
    if (!nextRoles.length) {
        selectedRoleId.value = undefined;
        return;
    }
    const exists = nextRoles.some((item) => item.id === selectedRoleId.value);
    if (!exists) {
        selectedRoleId.value = nextRoles[0].id;
    }
}, { immediate: true });
watch(selectedRole, async (role) => {
    const visibleMenuIdSet = new Set(visibleMenus.value.map((item) => item.id));
    checkedMenuIds.value = (role?.menuIds ?? []).filter((id) => visibleMenuIdSet.has(id));
    await nextTick();
    menuTreeRef.value?.setCheckedKeys(checkedMenuIds.value);
}, { immediate: true });
const handleSave = async () => {
    if (!selectedRoleId.value) {
        ElMessage.warning('请选择角色');
        return;
    }
    const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) ?? [];
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() ?? [];
    const visibleMenuIdSet = new Set(visibleMenus.value.map((item) => item.id));
    const merged = Array.from(new Set([...checkedKeys, ...halfCheckedKeys]
        .map((item) => Number(item))
        .filter((id) => Number.isFinite(id) && visibleMenuIdSet.has(id))));
    saving.value = true;
    try {
        await assignAdminRoleMenusApi(selectedRoleId.value, merged);
        ElMessage.success('菜单权限保存成功');
        await refreshData();
    }
    finally {
        saving.value = false;
    }
};
onMounted(() => {
    refreshData();
});
watch(() => sessionStore.currentOrgId, () => {
    refreshData();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-grid single" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "menu-permission-toolbar" },
});
const __VLS_0 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    inline: (true),
    ...{ class: "filter-bar compact-filter-bar" },
}));
const __VLS_2 = __VLS_1({
    inline: (true),
    ...{ class: "filter-bar compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    label: "角色",
}));
const __VLS_6 = __VLS_5({
    label: "角色",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    modelValue: (__VLS_ctx.selectedRoleId),
    placeholder: "请选择角色",
    ...{ style: {} },
    filterable: true,
}));
const __VLS_10 = __VLS_9({
    modelValue: (__VLS_ctx.selectedRoleId),
    placeholder: "请选择角色",
    ...{ style: {} },
    filterable: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_11.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.filteredRoles))) {
    const __VLS_12 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        key: (item.id),
        label: (`${item.roleName}（${item.roleCode}）`),
        value: (item.id),
    }));
    const __VLS_14 = __VLS_13({
        key: (item.id),
        label: (`${item.roleName}（${item.roleCode}）`),
        value: (item.id),
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
}
var __VLS_11;
var __VLS_7;
const __VLS_16 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({}));
const __VLS_18 = __VLS_17({}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    ...{ 'onClick': {} },
    type: "primary",
    loading: (__VLS_ctx.saving),
}));
const __VLS_22 = __VLS_21({
    ...{ 'onClick': {} },
    type: "primary",
    loading: (__VLS_ctx.saving),
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
let __VLS_24;
let __VLS_25;
let __VLS_26;
const __VLS_27 = {
    onClick: (__VLS_ctx.handleSave)
};
__VLS_23.slots.default;
var __VLS_23;
var __VLS_19;
var __VLS_3;
const __VLS_28 = {}.ElTree;
/** @type {[typeof __VLS_components.ElTree, typeof __VLS_components.elTree, typeof __VLS_components.ElTree, typeof __VLS_components.elTree, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    ref: "menuTreeRef",
    nodeKey: "id",
    showCheckbox: true,
    defaultExpandAll: true,
    data: (__VLS_ctx.menuTreeData),
    props: ({ label: 'label', children: 'children' }),
}));
const __VLS_30 = __VLS_29({
    ref: "menuTreeRef",
    nodeKey: "id",
    showCheckbox: true,
    defaultExpandAll: true,
    data: (__VLS_ctx.menuTreeData),
    props: ({ label: 'label', children: 'children' }),
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
/** @type {typeof __VLS_ctx.menuTreeRef} */ ;
var __VLS_32 = {};
__VLS_31.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_31.slots;
    const [{ data }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_34 = {}.ElSpace;
    /** @type {[typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, typeof __VLS_components.ElSpace, typeof __VLS_components.elSpace, ]} */ ;
    // @ts-ignore
    const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({}));
    const __VLS_36 = __VLS_35({}, ...__VLS_functionalComponentArgsRest(__VLS_35));
    __VLS_37.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (data.label);
    const __VLS_38 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
        size: "small",
        type: "info",
    }));
    const __VLS_40 = __VLS_39({
        size: "small",
        type: "info",
    }, ...__VLS_functionalComponentArgsRest(__VLS_39));
    __VLS_41.slots.default;
    (data.menuType);
    var __VLS_41;
    const __VLS_42 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
        size: "small",
    }));
    const __VLS_44 = __VLS_43({
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_43));
    __VLS_45.slots.default;
    (data.menuCode);
    var __VLS_45;
    var __VLS_37;
}
var __VLS_31;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['menu-permission-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
// @ts-ignore
var __VLS_33 = __VLS_32;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            loading: loading,
            saving: saving,
            selectedRoleId: selectedRoleId,
            menuTreeRef: menuTreeRef,
            filteredRoles: filteredRoles,
            menuTreeData: menuTreeData,
            handleSave: handleSave,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
