/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { createAdminRoleApi, fetchAdminRolesApi, updateAdminRoleApi, } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const loading = ref(false);
const submitting = ref(false);
const dialogVisible = ref(false);
const editingRoleId = ref(null);
const roles = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();
const queryForm = reactive({
    keyword: '',
    roleType: '',
});
const toolbarButtons = [{ key: 'create', label: '新增角色', type: 'primary' }];
const form = reactive({
    roleCode: '',
    roleName: '',
    roleType: 'PLATFORM',
    dataScopeType: 'ALL',
    description: '',
    status: 'ENABLED',
    menuIds: [],
});
const roleTypeOptions = ['PLATFORM', 'GROUP', 'STORE'];
const dataScopeOptions = ['ALL', 'GROUP', 'STORE', 'CUSTOM'];
const isRoleEditable = (role) => role.editable !== false;
const isBuiltinRole = (role) => role.builtin === true;
const roleTypeSelectableOptions = computed(() => (sessionStore.platformAdminMode ? roleTypeOptions : roleTypeOptions.filter((item) => item !== 'PLATFORM')));
const currentOrgId = computed(() => sessionStore.currentOrgId || undefined);
const filteredRoles = computed(() => {
    const keyword = queryForm.keyword.trim().toLowerCase();
    const roleType = queryForm.roleType;
    return roles.value.filter((item) => {
        const matchKeyword = !keyword ||
            item.roleCode.toLowerCase().includes(keyword) ||
            item.roleName.toLowerCase().includes(keyword);
        const matchRoleType = !roleType || item.roleType === roleType;
        return matchKeyword && matchRoleType;
    });
});
const pagedRoles = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    const end = start + pageSize.value;
    return filteredRoles.value.slice(start, end);
});
const resetForm = () => {
    form.roleCode = '';
    form.roleName = '';
    form.roleType = 'PLATFORM';
    form.dataScopeType = 'ALL';
    form.description = '';
    form.status = 'ENABLED';
    form.menuIds = [];
    editingRoleId.value = null;
};
const resetQuery = () => {
    queryForm.keyword = '';
    queryForm.roleType = '';
};
const handleToolbarAction = (key) => {
    if (key === 'create') {
        openCreate();
    }
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
const loadRoles = async () => {
    loading.value = true;
    try {
        roles.value = await fetchAdminRolesApi(currentOrgId.value);
    }
    finally {
        loading.value = false;
    }
};
const openCreate = () => {
    resetForm();
    if (!sessionStore.platformAdminMode) {
        form.roleType = 'GROUP';
        form.dataScopeType = 'GROUP';
    }
    dialogVisible.value = true;
};
const openEdit = (row) => {
    if (!isRoleEditable(row)) {
        ElMessage.warning('内置角色不可编辑');
        return;
    }
    editingRoleId.value = row.id;
    form.roleCode = row.roleCode;
    form.roleName = row.roleName;
    form.roleType = row.roleType;
    form.dataScopeType = row.dataScopeType;
    form.description = row.description ?? '';
    form.status = row.status;
    form.menuIds = row.menuIds ?? [];
    dialogVisible.value = true;
};
const handleSave = async () => {
    if (!form.roleName?.trim()) {
        ElMessage.warning('请填写角色名称');
        return;
    }
    if (editingRoleId.value && !form.roleCode?.trim()) {
        ElMessage.warning('请填写角色编码');
        return;
    }
    submitting.value = true;
    try {
        const roleCode = editingRoleId.value ? form.roleCode?.trim() : undefined;
        const payload = {
            roleCode,
            roleName: form.roleName.trim(),
            roleType: form.roleType ?? 'PLATFORM',
            dataScopeType: form.dataScopeType ?? 'ALL',
            description: form.description?.trim(),
            status: form.status ?? 'ENABLED',
            menuIds: form.menuIds ?? [],
        };
        if (editingRoleId.value) {
            await updateAdminRoleApi(editingRoleId.value, payload, currentOrgId.value);
            ElMessage.success('角色更新成功');
        }
        else {
            await createAdminRoleApi(payload, currentOrgId.value);
            ElMessage.success('角色创建成功');
        }
        dialogVisible.value = false;
        resetForm();
        await loadRoles();
    }
    finally {
        submitting.value = false;
    }
};
onMounted(() => {
    loadRoles();
});
watch(() => sessionStore.currentOrgId, () => {
    loadRoles();
});
watch(() => [queryForm.keyword, queryForm.roleType], () => {
    currentPage.value = 1;
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
/** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
    model: (__VLS_ctx.queryForm),
}));
const __VLS_1 = __VLS_0({
    model: (__VLS_ctx.queryForm),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
const __VLS_3 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
    label: "角色编码",
}));
const __VLS_5 = __VLS_4({
    label: "角色编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.queryForm.keyword),
    placeholder: "请输入角色编码或名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.queryForm.keyword),
    placeholder: "请输入角色编码或名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "角色类型",
}));
const __VLS_13 = __VLS_12({
    label: "角色类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.queryForm.roleType),
    placeholder: "请选择角色类型",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.queryForm.roleType),
    placeholder: "请选择角色类型",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
__VLS_18.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleTypeOptions))) {
    const __VLS_19 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
        key: (item),
        label: (item),
        value: (item),
    }));
    const __VLS_21 = __VLS_20({
        key: (item),
        label: (item),
        value: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_20));
}
var __VLS_18;
var __VLS_14;
const __VLS_23 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({}));
const __VLS_25 = __VLS_24({}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
const __VLS_27 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    ...{ 'onClick': {} },
}));
const __VLS_29 = __VLS_28({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
let __VLS_31;
let __VLS_32;
let __VLS_33;
const __VLS_34 = {
    onClick: (__VLS_ctx.resetQuery)
};
__VLS_30.slots.default;
var __VLS_30;
var __VLS_26;
var __VLS_2;
/** @type {[typeof CommonToolbarSection, ]} */ ;
// @ts-ignore
const __VLS_35 = __VLS_asFunctionalComponent(CommonToolbarSection, new CommonToolbarSection({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.toolbarButtons),
}));
const __VLS_36 = __VLS_35({
    ...{ 'onAction': {} },
    buttons: (__VLS_ctx.toolbarButtons),
}, ...__VLS_functionalComponentArgsRest(__VLS_35));
let __VLS_38;
let __VLS_39;
let __VLS_40;
const __VLS_41 = {
    onAction: (__VLS_ctx.handleToolbarAction)
};
var __VLS_37;
const __VLS_42 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    data: (__VLS_ctx.pagedRoles),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}));
const __VLS_44 = __VLS_43({
    data: (__VLS_ctx.pagedRoles),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_45.slots.default;
const __VLS_46 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    prop: "id",
    label: "ID",
    width: "80",
}));
const __VLS_48 = __VLS_47({
    prop: "id",
    label: "ID",
    width: "80",
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
const __VLS_50 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    prop: "roleCode",
    label: "角色编码",
    minWidth: "200",
}));
const __VLS_52 = __VLS_51({
    prop: "roleCode",
    label: "角色编码",
    minWidth: "200",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
const __VLS_54 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    label: "所属集团",
    minWidth: "180",
}));
const __VLS_56 = __VLS_55({
    label: "所属集团",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
__VLS_57.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_57.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.tenantGroupName || (row.tenantGroupId > 0 ? `集团ID ${row.tenantGroupId}` : '平台'));
}
var __VLS_57;
const __VLS_58 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    prop: "roleName",
    label: "角色名称",
    minWidth: "180",
}));
const __VLS_60 = __VLS_59({
    prop: "roleName",
    label: "角色名称",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
const __VLS_62 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    prop: "roleType",
    label: "角色类型",
    width: "140",
}));
const __VLS_64 = __VLS_63({
    prop: "roleType",
    label: "角色类型",
    width: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
const __VLS_66 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "属性",
    width: "110",
}));
const __VLS_68 = __VLS_67({
    label: "属性",
    width: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_69.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    if (__VLS_ctx.isBuiltinRole(row)) {
        const __VLS_70 = {}.ElTag;
        /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
        // @ts-ignore
        const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
            type: "warning",
            size: "small",
        }));
        const __VLS_72 = __VLS_71({
            type: "warning",
            size: "small",
        }, ...__VLS_functionalComponentArgsRest(__VLS_71));
        __VLS_73.slots.default;
        var __VLS_73;
    }
    else {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    }
}
var __VLS_69;
const __VLS_74 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "操作",
    width: "120",
    fixed: "right",
}));
const __VLS_76 = __VLS_75({
    label: "操作",
    width: "120",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_77.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_78 = {}.ElTooltip;
    /** @type {[typeof __VLS_components.ElTooltip, typeof __VLS_components.elTooltip, typeof __VLS_components.ElTooltip, typeof __VLS_components.elTooltip, ]} */ ;
    // @ts-ignore
    const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
        content: (__VLS_ctx.isRoleEditable(row) ? '' : '内置角色不可编辑'),
        disabled: (__VLS_ctx.isRoleEditable(row)),
        placement: "top",
    }));
    const __VLS_80 = __VLS_79({
        content: (__VLS_ctx.isRoleEditable(row) ? '' : '内置角色不可编辑'),
        disabled: (__VLS_ctx.isRoleEditable(row)),
        placement: "top",
    }, ...__VLS_functionalComponentArgsRest(__VLS_79));
    __VLS_81.slots.default;
    const __VLS_82 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
        disabled: (!__VLS_ctx.isRoleEditable(row)),
    }));
    const __VLS_84 = __VLS_83({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
        disabled: (!__VLS_ctx.isRoleEditable(row)),
    }, ...__VLS_functionalComponentArgsRest(__VLS_83));
    let __VLS_86;
    let __VLS_87;
    let __VLS_88;
    const __VLS_89 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEdit(row);
        }
    };
    __VLS_85.slots.default;
    var __VLS_85;
    var __VLS_81;
}
var __VLS_77;
var __VLS_45;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_90 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredRoles.length),
}));
const __VLS_91 = __VLS_90({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredRoles.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_90));
let __VLS_93;
let __VLS_94;
let __VLS_95;
const __VLS_96 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_97 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_92;
const __VLS_98 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.editingRoleId ? '编辑角色' : '新增角色'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_100 = __VLS_99({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.editingRoleId ? '编辑角色' : '新增角色'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
let __VLS_102;
let __VLS_103;
let __VLS_104;
const __VLS_105 = {
    onClosed: (__VLS_ctx.resetForm)
};
__VLS_101.slots.default;
const __VLS_106 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_108 = __VLS_107({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
__VLS_109.slots.default;
if (__VLS_ctx.editingRoleId) {
    const __VLS_110 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
        label: "角色编码",
        required: true,
    }));
    const __VLS_112 = __VLS_111({
        label: "角色编码",
        required: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_111));
    __VLS_113.slots.default;
    const __VLS_114 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
        modelValue: (__VLS_ctx.form.roleCode),
        disabled: (Boolean(__VLS_ctx.editingRoleId)),
    }));
    const __VLS_116 = __VLS_115({
        modelValue: (__VLS_ctx.form.roleCode),
        disabled: (Boolean(__VLS_ctx.editingRoleId)),
    }, ...__VLS_functionalComponentArgsRest(__VLS_115));
    var __VLS_113;
}
const __VLS_118 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    label: "角色名称",
    required: true,
}));
const __VLS_120 = __VLS_119({
    label: "角色名称",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
__VLS_121.slots.default;
const __VLS_122 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    modelValue: (__VLS_ctx.form.roleName),
}));
const __VLS_124 = __VLS_123({
    modelValue: (__VLS_ctx.form.roleName),
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
var __VLS_121;
const __VLS_126 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    label: "角色类型",
}));
const __VLS_128 = __VLS_127({
    label: "角色类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
__VLS_129.slots.default;
const __VLS_130 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    modelValue: (__VLS_ctx.form.roleType),
    ...{ style: {} },
}));
const __VLS_132 = __VLS_131({
    modelValue: (__VLS_ctx.form.roleType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
__VLS_133.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.roleTypeSelectableOptions))) {
    const __VLS_134 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
        key: (item),
        label: (item),
        value: (item),
    }));
    const __VLS_136 = __VLS_135({
        key: (item),
        label: (item),
        value: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_135));
}
var __VLS_133;
var __VLS_129;
const __VLS_138 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    label: "数据范围",
}));
const __VLS_140 = __VLS_139({
    label: "数据范围",
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
__VLS_141.slots.default;
const __VLS_142 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    modelValue: (__VLS_ctx.form.dataScopeType),
    ...{ style: {} },
}));
const __VLS_144 = __VLS_143({
    modelValue: (__VLS_ctx.form.dataScopeType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
__VLS_145.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.dataScopeOptions))) {
    const __VLS_146 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
        key: (item),
        label: (item),
        value: (item),
    }));
    const __VLS_148 = __VLS_147({
        key: (item),
        label: (item),
        value: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_147));
}
var __VLS_145;
var __VLS_141;
const __VLS_150 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
    label: "状态",
}));
const __VLS_152 = __VLS_151({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_151));
__VLS_153.slots.default;
const __VLS_154 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
    modelValue: (__VLS_ctx.form.status),
    ...{ style: {} },
}));
const __VLS_156 = __VLS_155({
    modelValue: (__VLS_ctx.form.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_155));
__VLS_157.slots.default;
const __VLS_158 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_160 = __VLS_159({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_159));
const __VLS_162 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_164 = __VLS_163({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
var __VLS_157;
var __VLS_153;
const __VLS_166 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
    label: "描述",
}));
const __VLS_168 = __VLS_167({
    label: "描述",
}, ...__VLS_functionalComponentArgsRest(__VLS_167));
__VLS_169.slots.default;
const __VLS_170 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
    modelValue: (__VLS_ctx.form.description),
    type: "textarea",
    rows: (3),
}));
const __VLS_172 = __VLS_171({
    modelValue: (__VLS_ctx.form.description),
    type: "textarea",
    rows: (3),
}, ...__VLS_functionalComponentArgsRest(__VLS_171));
var __VLS_169;
var __VLS_109;
{
    const { footer: __VLS_thisSlot } = __VLS_101.slots;
    const __VLS_174 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
        ...{ 'onClick': {} },
    }));
    const __VLS_176 = __VLS_175({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_175));
    let __VLS_178;
    let __VLS_179;
    let __VLS_180;
    const __VLS_181 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialogVisible = false;
        }
    };
    __VLS_177.slots.default;
    var __VLS_177;
    const __VLS_182 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.submitting),
    }));
    const __VLS_184 = __VLS_183({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.submitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_183));
    let __VLS_186;
    let __VLS_187;
    let __VLS_188;
    const __VLS_189 = {
        onClick: (__VLS_ctx.handleSave)
    };
    __VLS_185.slots.default;
    var __VLS_185;
}
var __VLS_101;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            CommonToolbarSection: CommonToolbarSection,
            ItemPaginationSection: ItemPaginationSection,
            loading: loading,
            submitting: submitting,
            dialogVisible: dialogVisible,
            editingRoleId: editingRoleId,
            currentPage: currentPage,
            pageSize: pageSize,
            queryForm: queryForm,
            toolbarButtons: toolbarButtons,
            form: form,
            roleTypeOptions: roleTypeOptions,
            dataScopeOptions: dataScopeOptions,
            isRoleEditable: isRoleEditable,
            isBuiltinRole: isBuiltinRole,
            roleTypeSelectableOptions: roleTypeSelectableOptions,
            filteredRoles: filteredRoles,
            pagedRoles: pagedRoles,
            resetForm: resetForm,
            resetQuery: resetQuery,
            handleToolbarAction: handleToolbarAction,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            openEdit: openEdit,
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
