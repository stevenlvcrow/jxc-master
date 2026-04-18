/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { bindGroupAdminApi, createAdminGroupApi, deleteAdminGroupApi, fetchGroupAdminCandidatesApi, fetchAdminGroupsApi, updateAdminGroupApi, updateAdminGroupStatusApi, } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const loading = ref(false);
const submitting = ref(false);
const bindSubmitting = ref(false);
const bindCandidatesLoading = ref(false);
const createDialogVisible = ref(false);
const bindDialogVisible = ref(false);
const editingGroupId = ref(null);
const groups = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const bindingGroup = ref(null);
const bindCandidates = ref([]);
const queryForm = reactive({
    keyword: '',
    status: '',
});
const sessionStore = useSessionStore();
const canManageAllGroups = computed(() => sessionStore.platformAdminMode);
const toolbarButtons = computed(() => (canManageAllGroups.value ? [{ key: 'create', label: '新增集团', type: 'primary' }] : []));
const form = reactive({
    groupCode: '',
    groupName: '',
    status: 'ENABLED',
    remark: '',
});
const bindForm = reactive({
    userId: undefined,
    phone: '',
    realName: '',
});
const resetForm = () => {
    form.groupCode = '';
    form.groupName = '';
    form.status = 'ENABLED';
    form.remark = '';
    editingGroupId.value = null;
};
const resetBindForm = () => {
    bindForm.userId = undefined;
    bindForm.phone = '';
    bindForm.realName = '';
    bindCandidates.value = [];
    bindingGroup.value = null;
};
const filteredGroups = computed(() => {
    const keyword = queryForm.keyword.trim().toLowerCase();
    const status = queryForm.status;
    return groups.value.filter((item) => {
        const keywordMatched = keyword
            ? `${item.groupCode}${item.groupName}${item.remark ?? ''}`.toLowerCase().includes(keyword)
            : true;
        const statusMatched = status ? item.status === status : true;
        return keywordMatched && statusMatched;
    });
});
const pagedGroups = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredGroups.value.slice(start, start + pageSize.value);
});
const resetQuery = () => {
    queryForm.keyword = '';
    queryForm.status = '';
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
const loadGroups = async () => {
    loading.value = true;
    try {
        groups.value = await fetchAdminGroupsApi();
    }
    finally {
        loading.value = false;
    }
};
const openCreateDialog = () => {
    if (!canManageAllGroups.value) {
        return;
    }
    resetForm();
    createDialogVisible.value = true;
};
const openEditDialog = (group) => {
    editingGroupId.value = group.id;
    form.groupCode = group.groupCode;
    form.groupName = group.groupName;
    form.status = group.status;
    form.remark = group.remark ?? '';
    createDialogVisible.value = true;
};
const handleSave = async () => {
    if (!form.groupName.trim()) {
        ElMessage.warning('请填写集团名称');
        return;
    }
    if (editingGroupId.value && !form.groupCode.trim()) {
        ElMessage.warning('请填写集团编码');
        return;
    }
    submitting.value = true;
    try {
        const payload = {
            groupCode: editingGroupId.value ? form.groupCode.trim() : undefined,
            groupName: form.groupName.trim(),
            status: form.status,
            remark: form.remark.trim() || undefined,
        };
        if (editingGroupId.value) {
            await updateAdminGroupApi(editingGroupId.value, payload);
            ElMessage.success('集团更新成功');
        }
        else {
            await createAdminGroupApi(payload);
            ElMessage.success('集团创建成功');
        }
        createDialogVisible.value = false;
        resetForm();
        await loadGroups();
    }
    finally {
        submitting.value = false;
    }
};
const handleStatusChange = async (row, value) => {
    const status = value ? 'ENABLED' : 'DISABLED';
    await updateAdminGroupStatusApi(row.id, status);
    row.status = status;
    ElMessage.success('状态更新成功');
};
const openBindDialog = async (group) => {
    if (!canManageAllGroups.value) {
        return;
    }
    bindingGroup.value = group;
    bindCandidatesLoading.value = true;
    try {
        bindCandidates.value = await fetchGroupAdminCandidatesApi(group.id);
    }
    finally {
        bindCandidatesLoading.value = false;
    }
    bindDialogVisible.value = true;
};
const handleBindCandidateChange = (userId) => {
    if (!userId) {
        return;
    }
    const target = bindCandidates.value.find((item) => item.userId === userId);
    if (!target) {
        return;
    }
    bindForm.phone = target.phone;
    if (!bindForm.realName.trim()) {
        bindForm.realName = target.realName;
    }
};
const handleToolbarAction = (key) => {
    if (key === 'create') {
        openCreateDialog();
    }
};
const handleDeleteGroup = async (group) => {
    if (!canManageAllGroups.value) {
        return;
    }
    try {
        await ElMessageBox.confirm(`确认删除集团“${group.groupName}”吗？`, '删除确认', {
            type: 'warning',
            confirmButtonText: '删除',
            cancelButtonText: '取消',
        });
    }
    catch {
        return;
    }
    await deleteAdminGroupApi(group.id);
    ElMessage.success('集团删除成功');
    await loadGroups();
};
const handleBindAdmin = async () => {
    if (!bindingGroup.value) {
        return;
    }
    if (!bindForm.phone.trim()) {
        ElMessage.warning('请选择门店账号或填写手机号');
        return;
    }
    bindSubmitting.value = true;
    try {
        await bindGroupAdminApi(bindingGroup.value.id, {
            phone: bindForm.phone.trim(),
            realName: bindForm.realName.trim() || undefined,
        });
        ElMessage.success('集团管理员绑定成功');
        bindDialogVisible.value = false;
        resetBindForm();
        await loadGroups();
    }
    finally {
        bindSubmitting.value = false;
    }
};
onMounted(() => {
    loadGroups();
});
watch(() => [queryForm.keyword, queryForm.status], () => {
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
    label: "关键字",
}));
const __VLS_5 = __VLS_4({
    label: "关键字",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.queryForm.keyword),
    placeholder: "集团编码/名称/备注",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.queryForm.keyword),
    placeholder: "集团编码/名称/备注",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "状态",
}));
const __VLS_13 = __VLS_12({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.queryForm.status),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.queryForm.status),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
__VLS_18.slots.default;
const __VLS_19 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_21 = __VLS_20({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
const __VLS_23 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_25 = __VLS_24({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
var __VLS_18;
var __VLS_14;
const __VLS_27 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({}));
const __VLS_29 = __VLS_28({}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    ...{ 'onClick': {} },
}));
const __VLS_33 = __VLS_32({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
let __VLS_35;
let __VLS_36;
let __VLS_37;
const __VLS_38 = {
    onClick: (__VLS_ctx.resetQuery)
};
__VLS_34.slots.default;
var __VLS_34;
var __VLS_30;
var __VLS_2;
if (__VLS_ctx.toolbarButtons.length) {
    /** @type {[typeof CommonToolbarSection, ]} */ ;
    // @ts-ignore
    const __VLS_39 = __VLS_asFunctionalComponent(CommonToolbarSection, new CommonToolbarSection({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }));
    const __VLS_40 = __VLS_39({
        ...{ 'onAction': {} },
        buttons: (__VLS_ctx.toolbarButtons),
    }, ...__VLS_functionalComponentArgsRest(__VLS_39));
    let __VLS_42;
    let __VLS_43;
    let __VLS_44;
    const __VLS_45 = {
        onAction: (__VLS_ctx.handleToolbarAction)
    };
    var __VLS_41;
}
const __VLS_46 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    data: (__VLS_ctx.pagedGroups),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}));
const __VLS_48 = __VLS_47({
    data: (__VLS_ctx.pagedGroups),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_49.slots.default;
const __VLS_50 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    prop: "id",
    label: "ID",
    width: "80",
}));
const __VLS_52 = __VLS_51({
    prop: "id",
    label: "ID",
    width: "80",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
const __VLS_54 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    prop: "groupCode",
    label: "集团编码",
    minWidth: "180",
}));
const __VLS_56 = __VLS_55({
    prop: "groupCode",
    label: "集团编码",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
const __VLS_58 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    prop: "groupName",
    label: "集团名称",
    minWidth: "200",
}));
const __VLS_60 = __VLS_59({
    prop: "groupName",
    label: "集团名称",
    minWidth: "200",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
const __VLS_62 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    prop: "remark",
    label: "备注",
    minWidth: "220",
    showOverflowTooltip: true,
}));
const __VLS_64 = __VLS_63({
    prop: "remark",
    label: "备注",
    minWidth: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
const __VLS_66 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "状态",
    width: "110",
}));
const __VLS_68 = __VLS_67({
    label: "状态",
    width: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_69.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_70 = {}.ElSwitch;
    /** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
    // @ts-ignore
    const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
        ...{ 'onChange': {} },
        modelValue: (row.status === 'ENABLED'),
    }));
    const __VLS_72 = __VLS_71({
        ...{ 'onChange': {} },
        modelValue: (row.status === 'ENABLED'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_71));
    let __VLS_74;
    let __VLS_75;
    let __VLS_76;
    const __VLS_77 = {
        onChange: (...[$event]) => {
            __VLS_ctx.handleStatusChange(row, $event);
        }
    };
    var __VLS_73;
}
var __VLS_69;
const __VLS_78 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    label: "操作",
    width: "230",
    fixed: "right",
}));
const __VLS_80 = __VLS_79({
    label: "操作",
    width: "230",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
__VLS_81.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_81.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_82 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }));
    const __VLS_84 = __VLS_83({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_83));
    let __VLS_86;
    let __VLS_87;
    let __VLS_88;
    const __VLS_89 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_85.slots.default;
    var __VLS_85;
    if (__VLS_ctx.canManageAllGroups) {
        const __VLS_90 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
            ...{ 'onClick': {} },
            type: "danger",
            link: true,
        }));
        const __VLS_92 = __VLS_91({
            ...{ 'onClick': {} },
            type: "danger",
            link: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_91));
        let __VLS_94;
        let __VLS_95;
        let __VLS_96;
        const __VLS_97 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.canManageAllGroups))
                    return;
                __VLS_ctx.handleDeleteGroup(row);
            }
        };
        __VLS_93.slots.default;
        var __VLS_93;
    }
    if (__VLS_ctx.canManageAllGroups) {
        const __VLS_98 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
            ...{ 'onClick': {} },
            type: "primary",
            link: true,
        }));
        const __VLS_100 = __VLS_99({
            ...{ 'onClick': {} },
            type: "primary",
            link: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_99));
        let __VLS_102;
        let __VLS_103;
        let __VLS_104;
        const __VLS_105 = {
            onClick: (...[$event]) => {
                if (!(__VLS_ctx.canManageAllGroups))
                    return;
                __VLS_ctx.openBindDialog(row);
            }
        };
        __VLS_101.slots.default;
        var __VLS_101;
    }
}
var __VLS_81;
var __VLS_49;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_106 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredGroups.length),
}));
const __VLS_107 = __VLS_106({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredGroups.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_106));
let __VLS_109;
let __VLS_110;
let __VLS_111;
const __VLS_112 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_113 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_108;
const __VLS_114 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.editingGroupId ? '修改集团' : '新增集团'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_116 = __VLS_115({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.editingGroupId ? '修改集团' : '新增集团'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_115));
let __VLS_118;
let __VLS_119;
let __VLS_120;
const __VLS_121 = {
    onClosed: (__VLS_ctx.resetForm)
};
__VLS_117.slots.default;
const __VLS_122 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_124 = __VLS_123({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
__VLS_125.slots.default;
if (__VLS_ctx.editingGroupId) {
    const __VLS_126 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
        label: "集团编码",
        required: true,
    }));
    const __VLS_128 = __VLS_127({
        label: "集团编码",
        required: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_127));
    __VLS_129.slots.default;
    const __VLS_130 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
        modelValue: (__VLS_ctx.form.groupCode),
        maxlength: "64",
    }));
    const __VLS_132 = __VLS_131({
        modelValue: (__VLS_ctx.form.groupCode),
        maxlength: "64",
    }, ...__VLS_functionalComponentArgsRest(__VLS_131));
    var __VLS_129;
}
const __VLS_134 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
    label: "集团名称",
    required: true,
}));
const __VLS_136 = __VLS_135({
    label: "集团名称",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_135));
__VLS_137.slots.default;
const __VLS_138 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    modelValue: (__VLS_ctx.form.groupName),
    maxlength: "128",
}));
const __VLS_140 = __VLS_139({
    modelValue: (__VLS_ctx.form.groupName),
    maxlength: "128",
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
var __VLS_137;
const __VLS_142 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    label: "状态",
}));
const __VLS_144 = __VLS_143({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
__VLS_145.slots.default;
const __VLS_146 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    modelValue: (__VLS_ctx.form.status),
    ...{ style: {} },
}));
const __VLS_148 = __VLS_147({
    modelValue: (__VLS_ctx.form.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
const __VLS_150 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_152 = __VLS_151({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_151));
const __VLS_154 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_156 = __VLS_155({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_155));
var __VLS_149;
var __VLS_145;
const __VLS_158 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
    label: "备注",
}));
const __VLS_160 = __VLS_159({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_159));
__VLS_161.slots.default;
const __VLS_162 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    maxlength: "500",
    showWordLimit: true,
}));
const __VLS_164 = __VLS_163({
    modelValue: (__VLS_ctx.form.remark),
    type: "textarea",
    rows: (3),
    maxlength: "500",
    showWordLimit: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
var __VLS_161;
var __VLS_125;
{
    const { footer: __VLS_thisSlot } = __VLS_117.slots;
    const __VLS_166 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
        ...{ 'onClick': {} },
    }));
    const __VLS_168 = __VLS_167({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_167));
    let __VLS_170;
    let __VLS_171;
    let __VLS_172;
    const __VLS_173 = {
        onClick: (...[$event]) => {
            __VLS_ctx.createDialogVisible = false;
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
        loading: (__VLS_ctx.submitting),
    }));
    const __VLS_176 = __VLS_175({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.submitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_175));
    let __VLS_178;
    let __VLS_179;
    let __VLS_180;
    const __VLS_181 = {
        onClick: (__VLS_ctx.handleSave)
    };
    __VLS_177.slots.default;
    var __VLS_177;
}
var __VLS_117;
const __VLS_182 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.bindDialogVisible),
    title: "绑定集团管理员",
    width: "460px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_184 = __VLS_183({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.bindDialogVisible),
    title: "绑定集团管理员",
    width: "460px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
let __VLS_186;
let __VLS_187;
let __VLS_188;
const __VLS_189 = {
    onClosed: (__VLS_ctx.resetBindForm)
};
__VLS_185.slots.default;
const __VLS_190 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_192 = __VLS_191({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_191));
__VLS_193.slots.default;
const __VLS_194 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({
    label: "集团",
}));
const __VLS_196 = __VLS_195({
    label: "集团",
}, ...__VLS_functionalComponentArgsRest(__VLS_195));
__VLS_197.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.bindingGroup?.groupName);
(__VLS_ctx.bindingGroup?.groupCode);
var __VLS_197;
const __VLS_198 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
    label: "门店账号",
}));
const __VLS_200 = __VLS_199({
    label: "门店账号",
}, ...__VLS_functionalComponentArgsRest(__VLS_199));
__VLS_201.slots.default;
const __VLS_202 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bindForm.userId),
    filterable: true,
    clearable: true,
    placeholder: "从门店绑定账号中选择",
    ...{ style: {} },
    loading: (__VLS_ctx.bindCandidatesLoading),
}));
const __VLS_204 = __VLS_203({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.bindForm.userId),
    filterable: true,
    clearable: true,
    placeholder: "从门店绑定账号中选择",
    ...{ style: {} },
    loading: (__VLS_ctx.bindCandidatesLoading),
}, ...__VLS_functionalComponentArgsRest(__VLS_203));
let __VLS_206;
let __VLS_207;
let __VLS_208;
const __VLS_209 = {
    onChange: (...[$event]) => {
        __VLS_ctx.handleBindCandidateChange($event);
    }
};
__VLS_205.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.bindCandidates))) {
    const __VLS_210 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({
        key: (item.userId),
        label: (`${item.realName}（${item.phone}）- ${item.storeName}`),
        value: (item.userId),
    }));
    const __VLS_212 = __VLS_211({
        key: (item.userId),
        label: (`${item.realName}（${item.phone}）- ${item.storeName}`),
        value: (item.userId),
    }, ...__VLS_functionalComponentArgsRest(__VLS_211));
}
var __VLS_205;
var __VLS_201;
const __VLS_214 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({
    label: "手机号",
    required: true,
}));
const __VLS_216 = __VLS_215({
    label: "手机号",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_215));
__VLS_217.slots.default;
const __VLS_218 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
    modelValue: (__VLS_ctx.bindForm.phone),
    maxlength: "20",
}));
const __VLS_220 = __VLS_219({
    modelValue: (__VLS_ctx.bindForm.phone),
    maxlength: "20",
}, ...__VLS_functionalComponentArgsRest(__VLS_219));
var __VLS_217;
const __VLS_222 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_223 = __VLS_asFunctionalComponent(__VLS_222, new __VLS_222({
    label: "姓名",
}));
const __VLS_224 = __VLS_223({
    label: "姓名",
}, ...__VLS_functionalComponentArgsRest(__VLS_223));
__VLS_225.slots.default;
const __VLS_226 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({
    modelValue: (__VLS_ctx.bindForm.realName),
    maxlength: "64",
}));
const __VLS_228 = __VLS_227({
    modelValue: (__VLS_ctx.bindForm.realName),
    maxlength: "64",
}, ...__VLS_functionalComponentArgsRest(__VLS_227));
var __VLS_225;
var __VLS_193;
{
    const { footer: __VLS_thisSlot } = __VLS_185.slots;
    const __VLS_230 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({
        ...{ 'onClick': {} },
    }));
    const __VLS_232 = __VLS_231({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_231));
    let __VLS_234;
    let __VLS_235;
    let __VLS_236;
    const __VLS_237 = {
        onClick: (...[$event]) => {
            __VLS_ctx.bindDialogVisible = false;
        }
    };
    __VLS_233.slots.default;
    var __VLS_233;
    const __VLS_238 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_239 = __VLS_asFunctionalComponent(__VLS_238, new __VLS_238({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bindSubmitting),
    }));
    const __VLS_240 = __VLS_239({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.bindSubmitting),
    }, ...__VLS_functionalComponentArgsRest(__VLS_239));
    let __VLS_242;
    let __VLS_243;
    let __VLS_244;
    const __VLS_245 = {
        onClick: (__VLS_ctx.handleBindAdmin)
    };
    __VLS_241.slots.default;
    var __VLS_241;
}
var __VLS_185;
/** @type {__VLS_StyleScopedClasses['page-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['single']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-dialog-form']} */ ;
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
            bindSubmitting: bindSubmitting,
            bindCandidatesLoading: bindCandidatesLoading,
            createDialogVisible: createDialogVisible,
            bindDialogVisible: bindDialogVisible,
            editingGroupId: editingGroupId,
            currentPage: currentPage,
            pageSize: pageSize,
            bindingGroup: bindingGroup,
            bindCandidates: bindCandidates,
            queryForm: queryForm,
            canManageAllGroups: canManageAllGroups,
            toolbarButtons: toolbarButtons,
            form: form,
            bindForm: bindForm,
            resetForm: resetForm,
            resetBindForm: resetBindForm,
            filteredGroups: filteredGroups,
            pagedGroups: pagedGroups,
            resetQuery: resetQuery,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            openEditDialog: openEditDialog,
            handleSave: handleSave,
            handleStatusChange: handleStatusChange,
            openBindDialog: openBindDialog,
            handleBindCandidateChange: handleBindCandidateChange,
            handleToolbarAction: handleToolbarAction,
            handleDeleteGroup: handleDeleteGroup,
            handleBindAdmin: handleBindAdmin,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
