/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { createUnitApi, deleteUnitApi, fetchUnitsApi, updateUnitApi } from '@/api/modules/unit';
import ItemPaginationSection from '@/views/items/components/ItemPaginationSection.vue';
import { useSessionStore } from '@/stores/session';
const query = reactive({
    keyword: '',
    status: 'ALL',
    unitType: 'ALL',
});
const currentPage = ref(1);
const pageSize = ref(10);
const sessionStore = useSessionStore();
const tableData = ref([]);
const loading = ref(false);
const dialogVisible = ref(false);
const saving = ref(false);
const dialogMode = ref('create');
const editingId = ref(null);
const createForm = reactive({
    code: '',
    name: '',
    type: 'STANDARD',
    status: true,
});
const createRules = computed(() => {
    const rules = {
        name: [{ required: true, message: '请输入单位名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择单位类型', trigger: 'change' }],
    };
    if (dialogMode.value === 'edit') {
        rules.code = [{ required: true, message: '请输入单位编码', trigger: 'blur' }];
    }
    return rules;
});
const createFormRef = ref();
const filteredData = computed(() => {
    return tableData.value;
});
const pagedData = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredData.value.slice(start, start + pageSize.value);
});
const statusLabel = (status) => (status === 'ENABLED' ? '启用' : '停用');
const typeLabel = (type) => (type === 'STANDARD' ? '标准单位' : '辅助单位');
const openCreateDialog = () => {
    dialogMode.value = 'create';
    editingId.value = null;
    createForm.code = '';
    createForm.name = '';
    createForm.type = 'STANDARD';
    createForm.status = true;
    dialogVisible.value = true;
};
const openEditDialog = (row) => {
    dialogMode.value = 'edit';
    editingId.value = row.id;
    createForm.code = row.code;
    createForm.name = row.name;
    createForm.type = row.type;
    createForm.status = row.status === 'ENABLED';
    dialogVisible.value = true;
};
const loadUnits = async () => {
    loading.value = true;
    try {
        const data = await fetchUnitsApi({
            keyword: query.keyword.trim() || undefined,
            status: query.status,
            unitType: query.unitType,
        }, sessionStore.currentOrgId || undefined);
        tableData.value = data.map((item) => ({
            id: item.id,
            code: item.code,
            name: item.name,
            type: item.type,
            status: item.status,
            createdAt: item.createdAt,
        }));
    }
    finally {
        loading.value = false;
    }
};
const handleSearch = () => {
    currentPage.value = 1;
    void loadUnits();
};
const handleReset = () => {
    query.keyword = '';
    query.status = 'ALL';
    query.unitType = 'ALL';
    currentPage.value = 1;
    void loadUnits();
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
const handleDelete = async (row) => {
    await ElMessageBox.confirm(`确定删除单位“${row.name}”吗？`, '删除确认', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
    });
    await deleteUnitApi(row.id, sessionStore.currentOrgId || undefined);
    ElMessage.success('删除成功');
    await loadUnits();
};
const submitCreate = async () => {
    await createFormRef.value?.validate();
    saving.value = true;
    const payload = {
        code: dialogMode.value === 'edit' ? createForm.code.trim() : undefined,
        name: createForm.name.trim(),
        type: createForm.type,
        status: createForm.status ? 'ENABLED' : 'DISABLED',
    };
    try {
        if (dialogMode.value === 'create') {
            await createUnitApi(payload, sessionStore.currentOrgId || undefined);
            ElMessage.success('新增成功');
        }
        else if (editingId.value != null) {
            await updateUnitApi(editingId.value, payload, sessionStore.currentOrgId || undefined);
            ElMessage.success('编辑成功');
        }
        dialogVisible.value = false;
        await loadUnits();
    }
    finally {
        saving.value = false;
    }
};
onMounted(() => {
    void loadUnits();
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
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}));
const __VLS_2 = __VLS_1({
    model: (__VLS_ctx.query),
    inline: true,
    ...{ class: "filter-bar compact-filter-bar" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    label: "单位信息",
}));
const __VLS_6 = __VLS_5({
    label: "单位信息",
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "根据编码或名称检索",
    clearable: true,
}));
const __VLS_10 = __VLS_9({
    modelValue: (__VLS_ctx.query.keyword),
    placeholder: "根据编码或名称检索",
    clearable: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
var __VLS_7;
const __VLS_12 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    label: "状态",
}));
const __VLS_14 = __VLS_13({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}));
const __VLS_18 = __VLS_17({
    modelValue: (__VLS_ctx.query.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    label: "全部",
    value: "ALL",
}));
const __VLS_22 = __VLS_21({
    label: "全部",
    value: "ALL",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
const __VLS_24 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    label: "启用",
    value: "ENABLED",
}));
const __VLS_26 = __VLS_25({
    label: "启用",
    value: "ENABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
const __VLS_28 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    label: "停用",
    value: "DISABLED",
}));
const __VLS_30 = __VLS_29({
    label: "停用",
    value: "DISABLED",
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
var __VLS_19;
var __VLS_15;
const __VLS_32 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    label: "单位类型",
}));
const __VLS_34 = __VLS_33({
    label: "单位类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
__VLS_35.slots.default;
const __VLS_36 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    modelValue: (__VLS_ctx.query.unitType),
    ...{ style: {} },
}));
const __VLS_38 = __VLS_37({
    modelValue: (__VLS_ctx.query.unitType),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
__VLS_39.slots.default;
const __VLS_40 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    label: "标准单位",
    value: "STANDARD",
}));
const __VLS_42 = __VLS_41({
    label: "标准单位",
    value: "STANDARD",
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
const __VLS_44 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
    label: "辅助单位",
    value: "AUXILIARY",
}));
const __VLS_46 = __VLS_45({
    label: "辅助单位",
    value: "AUXILIARY",
}, ...__VLS_functionalComponentArgsRest(__VLS_45));
const __VLS_48 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    label: "全部",
    value: "ALL",
}));
const __VLS_50 = __VLS_49({
    label: "全部",
    value: "ALL",
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
var __VLS_39;
var __VLS_35;
const __VLS_52 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({}));
const __VLS_54 = __VLS_53({}, ...__VLS_functionalComponentArgsRest(__VLS_53));
__VLS_55.slots.default;
const __VLS_56 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_58 = __VLS_57({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_57));
let __VLS_60;
let __VLS_61;
let __VLS_62;
const __VLS_63 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_59.slots.default;
var __VLS_59;
const __VLS_64 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
    ...{ 'onClick': {} },
}));
const __VLS_66 = __VLS_65({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_65));
let __VLS_68;
let __VLS_69;
let __VLS_70;
const __VLS_71 = {
    onClick: (__VLS_ctx.handleReset)
};
__VLS_67.slots.default;
var __VLS_67;
var __VLS_55;
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_72 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_74 = __VLS_73({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_73));
let __VLS_76;
let __VLS_77;
let __VLS_78;
const __VLS_79 = {
    onClick: (__VLS_ctx.openCreateDialog)
};
__VLS_75.slots.default;
var __VLS_75;
const __VLS_80 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
    data: (__VLS_ctx.pagedData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}));
const __VLS_82 = __VLS_81({
    data: (__VLS_ctx.pagedData),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_81));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_83.slots.default;
const __VLS_84 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
    label: "序号",
    width: "60",
    align: "center",
}));
const __VLS_86 = __VLS_85({
    label: "序号",
    width: "60",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_85));
__VLS_87.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_87.slots;
    const [scope] = __VLS_getSlotParams(__VLS_thisSlot);
    ((__VLS_ctx.currentPage - 1) * __VLS_ctx.pageSize + scope.$index + 1);
}
var __VLS_87;
const __VLS_88 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
    prop: "code",
    label: "单位编码",
    minWidth: "110",
}));
const __VLS_90 = __VLS_89({
    prop: "code",
    label: "单位编码",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_89));
const __VLS_92 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
    prop: "name",
    label: "单位名称",
    minWidth: "140",
}));
const __VLS_94 = __VLS_93({
    prop: "name",
    label: "单位名称",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_93));
const __VLS_96 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
    label: "单位类型",
    minWidth: "100",
}));
const __VLS_98 = __VLS_97({
    label: "单位类型",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_97));
__VLS_99.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_99.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.typeLabel(row.type));
}
var __VLS_99;
const __VLS_100 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
    label: "状态",
    width: "80",
    align: "center",
}));
const __VLS_102 = __VLS_101({
    label: "状态",
    width: "80",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_101));
__VLS_103.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_103.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_104 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
        type: (row.status === 'ENABLED' ? 'success' : 'info'),
        size: "small",
    }));
    const __VLS_106 = __VLS_105({
        type: (row.status === 'ENABLED' ? 'success' : 'info'),
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_105));
    __VLS_107.slots.default;
    (__VLS_ctx.statusLabel(row.status));
    var __VLS_107;
}
var __VLS_103;
const __VLS_108 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "160",
}));
const __VLS_110 = __VLS_109({
    prop: "createdAt",
    label: "创建时间",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
const __VLS_112 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
    label: "操作",
    width: "120",
    align: "center",
    fixed: "right",
}));
const __VLS_114 = __VLS_113({
    label: "操作",
    width: "120",
    align: "center",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_113));
__VLS_115.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_115.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_116 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
        ...{ 'onClick': {} },
        link: true,
        type: "primary",
    }));
    const __VLS_118 = __VLS_117({
        ...{ 'onClick': {} },
        link: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_117));
    let __VLS_120;
    let __VLS_121;
    let __VLS_122;
    const __VLS_123 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openEditDialog(row);
        }
    };
    __VLS_119.slots.default;
    var __VLS_119;
    const __VLS_124 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
        ...{ 'onClick': {} },
        link: true,
        type: "danger",
    }));
    const __VLS_126 = __VLS_125({
        ...{ 'onClick': {} },
        link: true,
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_125));
    let __VLS_128;
    let __VLS_129;
    let __VLS_130;
    const __VLS_131 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDelete(row);
        }
    };
    __VLS_127.slots.default;
    var __VLS_127;
}
var __VLS_115;
var __VLS_83;
/** @type {[typeof ItemPaginationSection, ]} */ ;
// @ts-ignore
const __VLS_132 = __VLS_asFunctionalComponent(ItemPaginationSection, new ItemPaginationSection({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredData.length),
}));
const __VLS_133 = __VLS_132({
    ...{ 'onUpdate:currentPage': {} },
    ...{ 'onUpdate:pageSize': {} },
    selectedCount: (0),
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.filteredData.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_132));
let __VLS_135;
let __VLS_136;
let __VLS_137;
const __VLS_138 = {
    'onUpdate:currentPage': (__VLS_ctx.handlePageChange)
};
const __VLS_139 = {
    'onUpdate:pageSize': (__VLS_ctx.handlePageSizeChange)
};
var __VLS_134;
const __VLS_140 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_141 = __VLS_asFunctionalComponent(__VLS_140, new __VLS_140({
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.dialogMode === 'create' ? '新增单位' : '编辑单位'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}));
const __VLS_142 = __VLS_141({
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.dialogMode === 'create' ? '新增单位' : '编辑单位'),
    width: "520px",
    ...{ class: "standard-form-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_141));
__VLS_143.slots.default;
const __VLS_144 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
    ref: "createFormRef",
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}));
const __VLS_146 = __VLS_145({
    ref: "createFormRef",
    model: (__VLS_ctx.createForm),
    rules: (__VLS_ctx.createRules),
    labelWidth: "90px",
    ...{ class: "standard-dialog-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_145));
/** @type {typeof __VLS_ctx.createFormRef} */ ;
var __VLS_148 = {};
__VLS_147.slots.default;
if (__VLS_ctx.dialogMode === 'edit') {
    const __VLS_150 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
        label: "单位编码",
        prop: "code",
    }));
    const __VLS_152 = __VLS_151({
        label: "单位编码",
        prop: "code",
    }, ...__VLS_functionalComponentArgsRest(__VLS_151));
    __VLS_153.slots.default;
    const __VLS_154 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
        modelValue: (__VLS_ctx.createForm.code),
        clearable: true,
    }));
    const __VLS_156 = __VLS_155({
        modelValue: (__VLS_ctx.createForm.code),
        clearable: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_155));
    var __VLS_153;
}
const __VLS_158 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
    label: "单位名称",
    prop: "name",
}));
const __VLS_160 = __VLS_159({
    label: "单位名称",
    prop: "name",
}, ...__VLS_functionalComponentArgsRest(__VLS_159));
__VLS_161.slots.default;
const __VLS_162 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    modelValue: (__VLS_ctx.createForm.name),
    clearable: true,
}));
const __VLS_164 = __VLS_163({
    modelValue: (__VLS_ctx.createForm.name),
    clearable: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
var __VLS_161;
const __VLS_166 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
    label: "单位类型",
    prop: "type",
}));
const __VLS_168 = __VLS_167({
    label: "单位类型",
    prop: "type",
}, ...__VLS_functionalComponentArgsRest(__VLS_167));
__VLS_169.slots.default;
const __VLS_170 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
    modelValue: (__VLS_ctx.createForm.type),
}));
const __VLS_172 = __VLS_171({
    modelValue: (__VLS_ctx.createForm.type),
}, ...__VLS_functionalComponentArgsRest(__VLS_171));
__VLS_173.slots.default;
const __VLS_174 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_175 = __VLS_asFunctionalComponent(__VLS_174, new __VLS_174({
    value: "STANDARD",
}));
const __VLS_176 = __VLS_175({
    value: "STANDARD",
}, ...__VLS_functionalComponentArgsRest(__VLS_175));
__VLS_177.slots.default;
var __VLS_177;
const __VLS_178 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
    value: "AUXILIARY",
}));
const __VLS_180 = __VLS_179({
    value: "AUXILIARY",
}, ...__VLS_functionalComponentArgsRest(__VLS_179));
__VLS_181.slots.default;
var __VLS_181;
var __VLS_173;
var __VLS_169;
const __VLS_182 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({
    label: "状态",
}));
const __VLS_184 = __VLS_183({
    label: "状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_183));
__VLS_185.slots.default;
const __VLS_186 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
    modelValue: (__VLS_ctx.createForm.status),
    inlinePrompt: true,
    activeText: "启用",
    inactiveText: "停用",
}));
const __VLS_188 = __VLS_187({
    modelValue: (__VLS_ctx.createForm.status),
    inlinePrompt: true,
    activeText: "启用",
    inactiveText: "停用",
}, ...__VLS_functionalComponentArgsRest(__VLS_187));
var __VLS_185;
var __VLS_147;
{
    const { footer: __VLS_thisSlot } = __VLS_143.slots;
    const __VLS_190 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
        ...{ 'onClick': {} },
    }));
    const __VLS_192 = __VLS_191({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_191));
    let __VLS_194;
    let __VLS_195;
    let __VLS_196;
    const __VLS_197 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialogVisible = false;
        }
    };
    __VLS_193.slots.default;
    var __VLS_193;
    const __VLS_198 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.saving),
    }));
    const __VLS_200 = __VLS_199({
        ...{ 'onClick': {} },
        type: "primary",
        loading: (__VLS_ctx.saving),
    }, ...__VLS_functionalComponentArgsRest(__VLS_199));
    let __VLS_202;
    let __VLS_203;
    let __VLS_204;
    const __VLS_205 = {
        onClick: (__VLS_ctx.submitCreate)
    };
    __VLS_201.slots.default;
    var __VLS_201;
}
var __VLS_143;
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
// @ts-ignore
var __VLS_149 = __VLS_148;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ItemPaginationSection: ItemPaginationSection,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            loading: loading,
            dialogVisible: dialogVisible,
            saving: saving,
            dialogMode: dialogMode,
            createForm: createForm,
            createRules: createRules,
            createFormRef: createFormRef,
            filteredData: filteredData,
            pagedData: pagedData,
            statusLabel: statusLabel,
            typeLabel: typeLabel,
            openCreateDialog: openCreateDialog,
            openEditDialog: openEditDialog,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handlePageChange: handlePageChange,
            handlePageSizeChange: handlePageSizeChange,
            handleDelete: handleDelete,
            submitCreate: submitCreate,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
