/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { createGroupStoreApi, deleteGroupStoreApi, fetchAdminGroupsApi, fetchGroupStoresApi, updateGroupStoreApi, } from '@/api/modules/system-admin';
import { useSessionStore } from '@/stores/session';
const sessionStore = useSessionStore();
const loading = ref(false);
const creating = ref(false);
const createDialogVisible = ref(false);
const dialogTitle = ref('新增门店');
const isEdit = ref(false);
const editingStoreId = ref(null);
const groups = ref([]);
const stores = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const selectedGroupId = ref();
const query = reactive({
    keyword: '',
    status: '',
});
const createForm = reactive({
    storeName: '',
    status: 'ENABLED',
    contactName: '',
    contactPhone: '',
    address: '',
    remark: '',
});
const toolbarButtons = [
    { key: 'create', label: '新增门店', type: 'primary' },
];
const resolveParentGroup = (org) => {
    if (!org) {
        return null;
    }
    if (org.type === 'group') {
        return org;
    }
    return sessionStore.rootGroups.find((group) => group.children?.some((child) => child.id === org.id)) ?? null;
};
const filteredStores = computed(() => stores.value.filter((item) => {
    const keyword = query.keyword.trim().toLowerCase();
    const keywordMatched = keyword
        ? `${item.storeCode}${item.storeName}${item.contactName ?? ''}${item.contactPhone ?? ''}`.toLowerCase().includes(keyword)
        : true;
    const statusMatched = query.status ? item.status === query.status : true;
    return keywordMatched && statusMatched;
}));
const pagedStores = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredStores.value.slice(start, start + pageSize.value);
});
const resetCreateForm = () => {
    createForm.storeName = '';
    createForm.status = 'ENABLED';
    createForm.contactName = '';
    createForm.contactPhone = '';
    createForm.address = '';
    createForm.remark = '';
    isEdit.value = false;
    editingStoreId.value = null;
    dialogTitle.value = '新增门店';
};
const loadGroups = async () => {
    groups.value = await fetchAdminGroupsApi();
    if (!groups.value.length) {
        selectedGroupId.value = undefined;
        stores.value = [];
        return;
    }
    const currentGroup = resolveParentGroup(sessionStore.currentOrg);
    if (!selectedGroupId.value) {
        if (currentGroup?.id.startsWith('group-')) {
            const currentId = Number(currentGroup.id.slice('group-'.length));
            if (!Number.isNaN(currentId) && groups.value.some((item) => item.id === currentId)) {
                selectedGroupId.value = currentId;
            }
        }
        if (!selectedGroupId.value) {
            selectedGroupId.value = groups.value[0].id;
        }
    }
};
const loadStores = async () => {
    if (!selectedGroupId.value) {
        stores.value = [];
        return;
    }
    loading.value = true;
    try {
        stores.value = await fetchGroupStoresApi(selectedGroupId.value);
    }
    finally {
        loading.value = false;
    }
};
const refresh = async () => {
    loading.value = true;
    try {
        await loadGroups();
        await loadStores();
    }
    finally {
        loading.value = false;
    }
};
const handleToolbarAction = (key) => {
    if (key === 'create') {
        if (!selectedGroupId.value) {
            ElMessage.warning('请先选择集团');
            return;
        }
        resetCreateForm();
        createDialogVisible.value = true;
        return;
    }
};
const openEditDialog = (row) => {
    isEdit.value = true;
    editingStoreId.value = row.id;
    dialogTitle.value = '编辑门店';
    createForm.storeName = row.storeName;
    createForm.status = row.status;
    createForm.contactName = row.contactName ?? '';
    createForm.contactPhone = row.contactPhone ?? '';
    createForm.address = row.address ?? '';
    createForm.remark = row.remark ?? '';
    createDialogVisible.value = true;
};
const handleDeleteStore = async (row) => {
    if (!selectedGroupId.value) {
        ElMessage.warning('请先选择集团');
        return;
    }
    await ElMessageBox.confirm(`确认删除门店“${row.storeName}”吗？`, '删除确认', { type: 'warning' });
    await deleteGroupStoreApi(selectedGroupId.value, row.id);
    ElMessage.success('门店删除成功');
    await loadStores();
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
const handleCreateStore = async () => {
    if (!selectedGroupId.value) {
        ElMessage.warning('请先选择集团');
        return;
    }
    if (!createForm.storeName.trim()) {
        ElMessage.warning('请填写门店名称');
        return;
    }
    creating.value = true;
    try {
        const payload = {
            storeCode: undefined,
            storeName: createForm.storeName.trim(),
            status: createForm.status,
            contactName: createForm.contactName.trim() || undefined,
            contactPhone: createForm.contactPhone.trim() || undefined,
            address: createForm.address.trim() || undefined,
            remark: createForm.remark.trim() || undefined,
        };
        if (isEdit.value && editingStoreId.value != null) {
            await updateGroupStoreApi(selectedGroupId.value, editingStoreId.value, payload);
            ElMessage.success('门店更新成功');
        }
        else {
            await createGroupStoreApi(selectedGroupId.value, {
                ...payload,
                storeCode: undefined,
            });
            ElMessage.success('门店创建成功');
        }
        createDialogVisible.value = false;
        resetCreateForm();
        await loadStores();
    }
    finally {
        creating.value = false;
    }
};
watch(selectedGroupId, () => {
    currentPage.value = 1;
    loadStores();
});
watch(() => sessionStore.currentOrgId, () => {
    selectedGroupId.value = undefined;
    refresh();
});
watch(() => [query.keyword, query.status], () => {
    currentPage.value = 1;
});
onMounted(() => {
    refresh();
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
    model: (__VLS_ctx.query),
}));
const __VLS_1 = __VLS_0({
    model: (__VLS_ctx.query),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
const __VLS_3 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
    label: "集团",
}));
const __VLS_5 = __VLS_4({
    label: "集团",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.selectedGroupId),
    ...{ style: {} },
    filterable: true,
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.selectedGroupId),
    ...{ style: {} },
    filterable: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
__VLS_10.slots.default;
for (const [group] of __VLS_getVForSourceType((__VLS_ctx.groups))) {
    const __VLS_11 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
        key: (group.id),
        label: (`${group.groupName}（${group.groupCode}）`),
        value: (group.id),
    }));
    const __VLS_13 = __VLS_12({
        key: (group.id),
        label: (`${group.groupName}（${group.groupCode}）`),
        value: (group.id),
    }, ...__VLS_functionalComponentArgsRest(__VLS_12));
}
var __VLS_10;
var __VLS_6;
const __VLS_15 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    label: "关键字",
}));
const __VLS_17 = __VLS_16({
    label: "关键字",
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
__VLS_18.slots.default;
const __VLS_19 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "门店编码/名称/联系方式",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_21 = __VLS_20({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "门店编码/名称/联系方式",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_20));
var __VLS_18;
const __VLS_23 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    label: "状态",
}));
const __VLS_25 = __VLS_24({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
__VLS_26.slots.default;
const __VLS_27 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_28 = __VLS_asFunctionalComponent(__VLS_27, new __VLS_27({
    modelValue: (__VLS_ctx.query.status),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_29 = __VLS_28({
    modelValue: (__VLS_ctx.query.status),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_28));
__VLS_30.slots.default;
const __VLS_31 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_32 = __VLS_asFunctionalComponent(__VLS_31, new __VLS_31({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_33 = __VLS_32({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_32));
const __VLS_35 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_36 = __VLS_asFunctionalComponent(__VLS_35, new __VLS_35({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_37 = __VLS_36({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_36));
var __VLS_30;
var __VLS_26;
var __VLS_2;
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
const __VLS_46 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    data: (__VLS_ctx.pagedStores),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}));
const __VLS_48 = __VLS_47({
    data: (__VLS_ctx.pagedStores),
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
    prop: "storeCode",
    label: "门店编码",
    minWidth: "140",
}));
const __VLS_52 = __VLS_51({
    prop: "storeCode",
    label: "门店编码",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
const __VLS_54 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    prop: "storeName",
    label: "门店名称",
    minWidth: "180",
}));
const __VLS_56 = __VLS_55({
    prop: "storeName",
    label: "门店名称",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
const __VLS_58 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    prop: "contactName",
    label: "联系人",
    width: "120",
}));
const __VLS_60 = __VLS_59({
    prop: "contactName",
    label: "联系人",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
const __VLS_62 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    prop: "contactPhone",
    label: "联系电话",
    width: "140",
}));
const __VLS_64 = __VLS_63({
    prop: "contactPhone",
    label: "联系电话",
    width: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
const __VLS_66 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    prop: "status",
    label: "状态",
    width: "100",
}));
const __VLS_68 = __VLS_67({
    prop: "status",
    label: "状态",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
const __VLS_70 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    prop: "address",
    label: "地址",
    minWidth: "220",
    showOverflowTooltip: true,
}));
const __VLS_72 = __VLS_71({
    prop: "address",
    label: "地址",
    minWidth: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
const __VLS_74 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "操作",
    width: "150",
    fixed: "right",
}));
const __VLS_76 = __VLS_75({
    label: "操作",
    width: "150",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_77.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_78 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }));
    const __VLS_80 = __VLS_79({
        ...{ 'onClick': {} },
        type: "primary",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_79));
    let __VLS_82;
    let __VLS_83;
    let __VLS_84;
    const __VLS_85 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_81.slots.default;
    var __VLS_81;
    const __VLS_86 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }));
    const __VLS_88 = __VLS_87({
        ...{ 'onClick': {} },
        type: "danger",
        link: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_87));
    let __VLS_90;
    let __VLS_91;
    let __VLS_92;
    const __VLS_93 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDeleteStore(row);
        }
    };
    __VLS_89.slots.default;
    var __VLS_89;
}
var __VLS_77;
var __VLS_49;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_94 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredStores.length),
}));
const __VLS_95 = __VLS_94({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredStores.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_94));
let __VLS_97;
let __VLS_98;
let __VLS_99;
const __VLS_100 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_101 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_96;
const __VLS_102 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
}));
const __VLS_104 = __VLS_103({
    ...{ 'onClosed': {} },
    modelValue: (__VLS_ctx.createDialogVisible),
    title: (__VLS_ctx.dialogTitle),
    width: "560px",
    ...{ class: "standard-form-dialog" },
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
let __VLS_106;
let __VLS_107;
let __VLS_108;
const __VLS_109 = {
    onClosed: (__VLS_ctx.resetCreateForm)
};
__VLS_105.slots.default;
const __VLS_110 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_112 = __VLS_111({
    labelWidth: "100px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
__VLS_113.slots.default;
const __VLS_114 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
    label: "门店名称",
    required: true,
}));
const __VLS_116 = __VLS_115({
    label: "门店名称",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_115));
__VLS_117.slots.default;
const __VLS_118 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    modelValue: (__VLS_ctx.createForm.storeName),
    maxlength: "128",
}));
const __VLS_120 = __VLS_119({
    modelValue: (__VLS_ctx.createForm.storeName),
    maxlength: "128",
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
var __VLS_117;
const __VLS_122 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
    label: "状态",
}));
const __VLS_124 = __VLS_123({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_123));
__VLS_125.slots.default;
const __VLS_126 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    modelValue: (__VLS_ctx.createForm.status),
    ...{ style: {} },
}));
const __VLS_128 = __VLS_127({
    modelValue: (__VLS_ctx.createForm.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
__VLS_129.slots.default;
const __VLS_130 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_132 = __VLS_131({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_131));
const __VLS_134 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_136 = __VLS_135({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_135));
var __VLS_129;
var __VLS_125;
const __VLS_138 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
    label: "联系人",
}));
const __VLS_140 = __VLS_139({
    label: "联系人",
}, ...__VLS_functionalComponentArgsRest(__VLS_139));
__VLS_141.slots.default;
const __VLS_142 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    modelValue: (__VLS_ctx.createForm.contactName),
    maxlength: "64",
}));
const __VLS_144 = __VLS_143({
    modelValue: (__VLS_ctx.createForm.contactName),
    maxlength: "64",
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
var __VLS_141;
const __VLS_146 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({
    label: "联系电话",
}));
const __VLS_148 = __VLS_147({
    label: "联系电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_147));
__VLS_149.slots.default;
const __VLS_150 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
    modelValue: (__VLS_ctx.createForm.contactPhone),
    maxlength: "32",
}));
const __VLS_152 = __VLS_151({
    modelValue: (__VLS_ctx.createForm.contactPhone),
    maxlength: "32",
}, ...__VLS_functionalComponentArgsRest(__VLS_151));
var __VLS_149;
const __VLS_154 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
    label: "地址",
}));
const __VLS_156 = __VLS_155({
    label: "地址",
}, ...__VLS_functionalComponentArgsRest(__VLS_155));
__VLS_157.slots.default;
const __VLS_158 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
    modelValue: (__VLS_ctx.createForm.address),
    maxlength: "255",
}));
const __VLS_160 = __VLS_159({
    modelValue: (__VLS_ctx.createForm.address),
    maxlength: "255",
}, ...__VLS_functionalComponentArgsRest(__VLS_159));
var __VLS_157;
const __VLS_162 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    label: "备注",
}));
const __VLS_164 = __VLS_163({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
__VLS_165.slots.default;
const __VLS_166 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
    modelValue: (__VLS_ctx.createForm.remark),
    type: "textarea",
    rows: (3),
    maxlength: "500",
    showWordLimit: true,
}));
const __VLS_168 = __VLS_167({
    modelValue: (__VLS_ctx.createForm.remark),
    type: "textarea",
    rows: (3),
    maxlength: "500",
    showWordLimit: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_167));
var __VLS_165;
var __VLS_113;
{
    const { footer: __VLS_thisSlot } = __VLS_105.slots;
    const __VLS_170 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
        ...{ 'onClick': {} },
    }));
    const __VLS_172 = __VLS_171({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_171));
    let __VLS_174;
    let __VLS_175;
    let __VLS_176;
    const __VLS_177 = {
        onClick: (...[$event]) => {
            __VLS_ctx.createDialogVisible = false;
        }
    };
    __VLS_173.slots.default;
    var __VLS_173;
    const __VLS_178 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.creating),
    }));
    const __VLS_180 = __VLS_179({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.creating),
    }, ...__VLS_functionalComponentArgsRest(__VLS_179));
    let __VLS_182;
    let __VLS_183;
    let __VLS_184;
    const __VLS_185 = {
        onClick: (__VLS_ctx.handleCreateStore)
    };
    __VLS_181.slots.default;
    (__VLS_ctx.isEdit ? '更新' : '保存');
    var __VLS_181;
}
var __VLS_105;
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
            creating: creating,
            createDialogVisible: createDialogVisible,
            dialogTitle: dialogTitle,
            isEdit: isEdit,
            groups: groups,
            currentPage: currentPage,
            pageSize: pageSize,
            selectedGroupId: selectedGroupId,
            query: query,
            createForm: createForm,
            toolbarButtons: toolbarButtons,
            filteredStores: filteredStores,
            pagedStores: pagedStores,
            resetCreateForm: resetCreateForm,
            handleToolbarAction: handleToolbarAction,
            openEditDialog: openEditDialog,
            handleDeleteStore: handleDeleteStore,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            handleCreateStore: handleCreateStore,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
