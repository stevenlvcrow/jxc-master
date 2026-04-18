/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import { useSessionStore } from '@/stores/session';
import { deleteItemRuleApi, fetchItemRulesApi, } from '@/api/modules/warehouse-item-rule';
const router = useRouter();
const sessionStore = useSessionStore();
const loading = ref(false);
const toolbarButtons = [
    { key: '新增', label: '新增', type: 'primary' },
];
const query = reactive({
    ruleCode: '',
    ruleName: '',
});
const tableData = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const resolveGroupId = () => {
    const currentOrg = sessionStore.currentOrg;
    if (!currentOrg) {
        return null;
    }
    if (currentOrg.type === 'group') {
        const groupId = Number(String(currentOrg.id).slice('group-'.length));
        return Number.isNaN(groupId) ? null : groupId;
    }
    if (currentOrg.type === 'store') {
        const parentGroup = sessionStore.rootGroups.find((group) => group.children?.some((child) => child.id === currentOrg.id));
        if (!parentGroup) {
            return null;
        }
        const groupId = Number(String(parentGroup.id).slice('group-'.length));
        return Number.isNaN(groupId) ? null : groupId;
    }
    return null;
};
const formatDateTime = (value) => {
    if (!value)
        return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${d} ${hh}:${mm}:${ss}`;
};
const filteredRows = computed(() => {
    const codeKeyword = query.ruleCode.trim().toLowerCase();
    const nameKeyword = query.ruleName.trim().toLowerCase();
    return tableData.value.filter((row) => {
        const matchCode = !codeKeyword || row.ruleCode.toLowerCase().includes(codeKeyword);
        const matchName = !nameKeyword || row.ruleName.toLowerCase().includes(nameKeyword);
        return matchCode && matchName;
    });
});
const pagedRows = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredRows.value.slice(start, start + pageSize.value);
});
const loadData = async () => {
    const groupId = resolveGroupId();
    if (!groupId) {
        tableData.value = [];
        return;
    }
    loading.value = true;
    try {
        tableData.value = await fetchItemRulesApi(groupId);
    }
    finally {
        loading.value = false;
    }
};
const handleSearch = () => {
    currentPage.value = 1;
};
const handleReset = () => {
    query.ruleCode = '';
    query.ruleName = '';
    currentPage.value = 1;
};
const handleView = (row) => {
    ElMessage.info(`查看功能待接入，规则：${row.ruleName}`);
};
const handleEdit = (row) => {
    ElMessage.info(`编辑功能待接入，规则：${row.ruleName}`);
};
const handleDelete = async (row) => {
    await ElMessageBox.confirm(`确认删除规则「${row.ruleName}」吗？`, '提示', { type: 'warning' });
    await deleteItemRuleApi(row.id);
    ElMessage.success('删除成功');
    await loadData();
};
const statusText = (status) => (status === 'ENABLED' ? '启用' : '停用');
const handleToolbarAction = (action) => {
    if (action === '新增') {
        router.push({ name: 'ItemRuleCreate' });
    }
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
watch(() => sessionStore.currentOrgId, () => {
    currentPage.value = 1;
    loadData();
});
onMounted(loadData);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
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
    label: "规则编码",
}));
const __VLS_5 = __VLS_4({
    label: "规则编码",
}, ...__VLS_functionalComponentArgsRest(__VLS_4));
__VLS_6.slots.default;
const __VLS_7 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_8 = __VLS_asFunctionalComponent(__VLS_7, new __VLS_7({
    modelValue: (__VLS_ctx.query.ruleCode),
    placeholder: "请输入规则编码",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_9 = __VLS_8({
    modelValue: (__VLS_ctx.query.ruleCode),
    placeholder: "请输入规则编码",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_8));
var __VLS_6;
const __VLS_11 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_12 = __VLS_asFunctionalComponent(__VLS_11, new __VLS_11({
    label: "规则名称",
}));
const __VLS_13 = __VLS_12({
    label: "规则名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_12));
__VLS_14.slots.default;
const __VLS_15 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_16 = __VLS_asFunctionalComponent(__VLS_15, new __VLS_15({
    modelValue: (__VLS_ctx.query.ruleName),
    placeholder: "请输入规则名称",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_17 = __VLS_16({
    modelValue: (__VLS_ctx.query.ruleName),
    placeholder: "请输入规则名称",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_16));
var __VLS_14;
const __VLS_19 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_20 = __VLS_asFunctionalComponent(__VLS_19, new __VLS_19({}));
const __VLS_21 = __VLS_20({}, ...__VLS_functionalComponentArgsRest(__VLS_20));
__VLS_22.slots.default;
const __VLS_23 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_24 = __VLS_asFunctionalComponent(__VLS_23, new __VLS_23({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_25 = __VLS_24({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_24));
let __VLS_27;
let __VLS_28;
let __VLS_29;
const __VLS_30 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_26.slots.default;
var __VLS_26;
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
    onClick: (__VLS_ctx.handleReset)
};
__VLS_34.slots.default;
var __VLS_34;
var __VLS_22;
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
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}));
const __VLS_48 = __VLS_47({
    data: (__VLS_ctx.pagedRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table" },
    fit: (false),
    height: (360),
    emptyText: ('当前机构暂无数据'),
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
__VLS_49.slots.default;
const __VLS_50 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    type: "index",
    label: "序号",
    width: "56",
}));
const __VLS_52 = __VLS_51({
    type: "index",
    label: "序号",
    width: "56",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
const __VLS_54 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    prop: "ruleCode",
    label: "规则编码",
    minWidth: "140",
    showOverflowTooltip: true,
}));
const __VLS_56 = __VLS_55({
    prop: "ruleCode",
    label: "规则编码",
    minWidth: "140",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
const __VLS_58 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    prop: "ruleName",
    label: "规则名称",
    minWidth: "220",
    showOverflowTooltip: true,
}));
const __VLS_60 = __VLS_59({
    prop: "ruleName",
    label: "规则名称",
    minWidth: "220",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
const __VLS_62 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    label: "业务管控",
    width: "90",
    align: "center",
}));
const __VLS_64 = __VLS_63({
    label: "业务管控",
    width: "90",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
__VLS_65.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_65.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.businessControl ? '是' : '否');
}
var __VLS_65;
const __VLS_66 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "状态",
    width: "80",
    align: "center",
}));
const __VLS_68 = __VLS_67({
    label: "状态",
    width: "80",
    align: "center",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_69.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.statusText(row.status));
}
var __VLS_69;
const __VLS_70 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    prop: "createdBy",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}));
const __VLS_72 = __VLS_71({
    prop: "createdBy",
    label: "创建人",
    minWidth: "100",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
const __VLS_74 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_76 = __VLS_75({
    label: "创建时间",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_77.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatDateTime(row.createdAt));
}
var __VLS_77;
const __VLS_78 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    label: "最新修改日期",
    minWidth: "170",
    showOverflowTooltip: true,
}));
const __VLS_80 = __VLS_79({
    label: "最新修改日期",
    minWidth: "170",
    showOverflowTooltip: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
__VLS_81.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_81.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (__VLS_ctx.formatDateTime(row.updatedAt));
}
var __VLS_81;
const __VLS_82 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    label: "操作",
    width: "180",
    fixed: "right",
}));
const __VLS_84 = __VLS_83({
    label: "操作",
    width: "180",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
__VLS_85.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_85.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_86 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_88 = __VLS_87({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_87));
    let __VLS_90;
    let __VLS_91;
    let __VLS_92;
    const __VLS_93 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleView(row);
        }
    };
    __VLS_89.slots.default;
    var __VLS_89;
    const __VLS_94 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_96 = __VLS_95({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_95));
    let __VLS_98;
    let __VLS_99;
    let __VLS_100;
    const __VLS_101 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleEdit(row);
        }
    };
    __VLS_97.slots.default;
    var __VLS_97;
    const __VLS_102 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }));
    const __VLS_104 = __VLS_103({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_103));
    let __VLS_106;
    let __VLS_107;
    let __VLS_108;
    const __VLS_109 = {
        onClick: (...[$event]) => {
            __VLS_ctx.handleDelete(row);
        }
    };
    __VLS_105.slots.default;
    var __VLS_105;
}
var __VLS_85;
var __VLS_49;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.filteredRows.length);
const __VLS_110 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}));
const __VLS_112 = __VLS_111({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50]),
    total: (__VLS_ctx.filteredRows.length),
    background: true,
    small: true,
    layout: "total, sizes, prev, pager, next, jumper",
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
let __VLS_114;
let __VLS_115;
let __VLS_116;
const __VLS_117 = {
    onCurrentChange: (__VLS_ctx.handlePageChange)
};
const __VLS_118 = {
    onSizeChange: (__VLS_ctx.handlePageSizeChange)
};
var __VLS_113;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            CommonToolbarSection: CommonToolbarSection,
            loading: loading,
            toolbarButtons: toolbarButtons,
            query: query,
            currentPage: currentPage,
            pageSize: pageSize,
            formatDateTime: formatDateTime,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleView: handleView,
            handleEdit: handleEdit,
            handleDelete: handleDelete,
            statusText: statusText,
            handleToolbarAction: handleToolbarAction,
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
