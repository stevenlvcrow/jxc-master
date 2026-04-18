import { computed, ref, watch } from 'vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTreeSection from '@/components/CommonTreeSection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';
const props = withDefaults(defineProps(), {
    title: '选择数据',
    width: '980px',
    loading: false,
    treeData: () => [],
    tableData: () => [],
    columns: () => [],
    rowKey: 'id',
    selectedRows: () => [],
    selectedLabelKey: 'name',
    total: 0,
    currentPage: 1,
    pageSize: 10,
    keywordLabel: '关键字',
    keywordPlaceholder: '请输入关键字检索',
    statusLabel: '状态',
    statusValue: '',
    statusOptions: () => [
        { label: '全部', value: '' },
        { label: '否', value: 'N' },
        { label: '是', value: 'Y' },
    ],
    keywordValue: '',
});
const emit = defineEmits();
const keyword = ref(props.keywordValue);
const status = ref(props.statusValue);
const selected = ref([...props.selectedRows]);
watch(() => props.keywordValue, (val) => {
    keyword.value = val;
});
watch(() => props.statusValue, (val) => {
    status.value = val;
});
watch(() => props.selectedRows, (rows) => {
    selected.value = [...rows];
}, { deep: true });
const dialogVisible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val),
});
const selectedCount = computed(() => selected.value.length);
const getRowId = (row) => String(row[props.rowKey] ?? '');
const selectedTags = computed(() => selected.value.map((row) => {
    const label = row[props.selectedLabelKey];
    return {
        id: getRowId(row),
        label: label == null || String(label).trim() === '' ? getRowId(row) : String(label),
    };
}));
const handleSearch = () => {
    emit('search', {
        keyword: keyword.value.trim(),
        status: status.value,
    });
};
const handleNodeClick = (data) => {
    emit('node-change', data);
};
const handleSelectionChange = (rows) => {
    selected.value = rows;
    emit('selection-change', rows);
};
const handleClearSelection = () => {
    selected.value = [];
    emit('clear-selection');
};
const handleRemoveTag = (id) => {
    selected.value = selected.value.filter((row) => getRowId(row) !== id);
    emit('selection-change', selected.value);
};
const handleCancel = () => {
    emit('cancel');
    dialogVisible.value = false;
};
const handleConfirm = () => {
    emit('confirm', selected.value);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    title: '选择数据',
    width: '980px',
    loading: false,
    treeData: () => [],
    tableData: () => [],
    columns: () => [],
    rowKey: 'id',
    selectedRows: () => [],
    selectedLabelKey: 'name',
    total: 0,
    currentPage: 1,
    pageSize: 10,
    keywordLabel: '关键字',
    keywordPlaceholder: '请输入关键字检索',
    statusLabel: '状态',
    statusValue: '',
    statusOptions: () => [
        { label: '全部', value: '' },
        { label: '否', value: 'N' },
        { label: '是', value: 'Y' },
    ],
    keywordValue: '',
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-content']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-tree-panel']} */ ;
// CSS variable injection 
// CSS variable injection end 
const __VLS_0 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.title),
    width: (__VLS_ctx.width),
    top: "8vh",
    ...{ class: "common-selector-dialog standard-form-dialog" },
    destroyOnClose: true,
    appendToBody: true,
}));
const __VLS_2 = __VLS_1({
    modelValue: (__VLS_ctx.dialogVisible),
    title: (__VLS_ctx.title),
    width: (__VLS_ctx.width),
    top: "8vh",
    ...{ class: "common-selector-dialog standard-form-dialog" },
    destroyOnClose: true,
    appendToBody: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
var __VLS_4 = {};
__VLS_3.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selector-query-row" },
});
/** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
    model: ({ keyword: __VLS_ctx.keyword, status: __VLS_ctx.status }),
}));
const __VLS_6 = __VLS_5({
    model: ({ keyword: __VLS_ctx.keyword, status: __VLS_ctx.status }),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    label: (`${__VLS_ctx.keywordLabel}:`),
}));
const __VLS_10 = __VLS_9({
    label: (`${__VLS_ctx.keywordLabel}:`),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_11.slots.default;
const __VLS_12 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    modelValue: (__VLS_ctx.keyword),
    placeholder: (__VLS_ctx.keywordPlaceholder),
    clearable: true,
    ...{ style: {} },
}));
const __VLS_14 = __VLS_13({
    modelValue: (__VLS_ctx.keyword),
    placeholder: (__VLS_ctx.keywordPlaceholder),
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
var __VLS_11;
const __VLS_16 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    label: (`${__VLS_ctx.statusLabel}:`),
}));
const __VLS_18 = __VLS_17({
    label: (`${__VLS_ctx.statusLabel}:`),
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    modelValue: (__VLS_ctx.status),
    ...{ style: {} },
}));
const __VLS_22 = __VLS_21({
    modelValue: (__VLS_ctx.status),
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
__VLS_23.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.statusOptions))) {
    const __VLS_24 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        key: (option.value),
        label: (option.label),
        value: (option.value),
    }));
    const __VLS_26 = __VLS_25({
        key: (option.value),
        label: (option.label),
        value: (option.value),
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
}
var __VLS_23;
var __VLS_19;
const __VLS_28 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({}));
const __VLS_30 = __VLS_29({}, ...__VLS_functionalComponentArgsRest(__VLS_29));
__VLS_31.slots.default;
const __VLS_32 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    ...{ 'onClick': {} },
    type: "primary",
    ...{ class: "selector-search-btn" },
}));
const __VLS_34 = __VLS_33({
    ...{ 'onClick': {} },
    type: "primary",
    ...{ class: "selector-search-btn" },
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
let __VLS_36;
let __VLS_37;
let __VLS_38;
const __VLS_39 = {
    onClick: (__VLS_ctx.handleSearch)
};
__VLS_35.slots.default;
var __VLS_35;
var __VLS_31;
var __VLS_7;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selector-content" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "selector-tree-panel" },
});
/** @type {[typeof CommonTreeSection, ]} */ ;
// @ts-ignore
const __VLS_40 = __VLS_asFunctionalComponent(CommonTreeSection, new CommonTreeSection({
    ...{ 'onNodeClick': {} },
    data: (__VLS_ctx.treeData),
    nodeKey: "id",
    defaultExpandAll: (true),
    expandOnClickNode: (false),
}));
const __VLS_41 = __VLS_40({
    ...{ 'onNodeClick': {} },
    data: (__VLS_ctx.treeData),
    nodeKey: "id",
    defaultExpandAll: (true),
    expandOnClickNode: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_40));
let __VLS_43;
let __VLS_44;
let __VLS_45;
const __VLS_46 = {
    onNodeClick: (__VLS_ctx.handleNodeClick)
};
var __VLS_42;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "selector-table-panel" },
});
/** @type {[typeof CommonTableSection, typeof CommonTableSection, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(CommonTableSection, new CommonTableSection({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    height: (320),
    fit: (false),
    border: (true),
    stripe: (true),
    loading: (__VLS_ctx.loading),
}));
const __VLS_48 = __VLS_47({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.tableData),
    height: (320),
    fit: (false),
    border: (true),
    stripe: (true),
    loading: (__VLS_ctx.loading),
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
let __VLS_50;
let __VLS_51;
let __VLS_52;
const __VLS_53 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_49.slots.default;
const __VLS_54 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    type: "selection",
    width: "46",
    fixed: "left",
}));
const __VLS_56 = __VLS_55({
    type: "selection",
    width: "46",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
const __VLS_58 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_60 = __VLS_59({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
var __VLS_62 = {};
for (const [column] of __VLS_getVForSourceType((__VLS_ctx.columns))) {
    const __VLS_64 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        minWidth: (column.minWidth),
        showOverflowTooltip: true,
    }));
    const __VLS_66 = __VLS_65({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        minWidth: (column.minWidth),
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_65));
}
var __VLS_49;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.total);
const __VLS_68 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50, 100]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "prev, pager, next, sizes",
}));
const __VLS_70 = __VLS_69({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.currentPage),
    pageSize: (__VLS_ctx.pageSize),
    pageSizes: ([10, 20, 50, 100]),
    total: (__VLS_ctx.total),
    background: true,
    small: true,
    layout: "prev, pager, next, sizes",
}, ...__VLS_functionalComponentArgsRest(__VLS_69));
let __VLS_72;
let __VLS_73;
let __VLS_74;
const __VLS_75 = {
    onCurrentChange: ((p) => __VLS_ctx.emit('page-change', p))
};
const __VLS_76 = {
    onSizeChange: ((s) => __VLS_ctx.emit('page-size-change', s))
};
var __VLS_71;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selector-selected-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selected-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.selectedCount);
const __VLS_77 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_78 = __VLS_asFunctionalComponent(__VLS_77, new __VLS_77({
    ...{ 'onClick': {} },
    text: true,
    ...{ class: "clear-btn" },
}));
const __VLS_79 = __VLS_78({
    ...{ 'onClick': {} },
    text: true,
    ...{ class: "clear-btn" },
}, ...__VLS_functionalComponentArgsRest(__VLS_78));
let __VLS_81;
let __VLS_82;
let __VLS_83;
const __VLS_84 = {
    onClick: (__VLS_ctx.handleClearSelection)
};
__VLS_80.slots.default;
var __VLS_80;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "selected-tags" },
});
for (const [tag] of __VLS_getVForSourceType((__VLS_ctx.selectedTags))) {
    const __VLS_85 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_86 = __VLS_asFunctionalComponent(__VLS_85, new __VLS_85({
        ...{ 'onClose': {} },
        key: (tag.id),
        size: "small",
        closable: true,
    }));
    const __VLS_87 = __VLS_86({
        ...{ 'onClose': {} },
        key: (tag.id),
        size: "small",
        closable: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_86));
    let __VLS_89;
    let __VLS_90;
    let __VLS_91;
    const __VLS_92 = {
        onClose: (...[$event]) => {
            __VLS_ctx.handleRemoveTag(tag.id);
        }
    };
    __VLS_88.slots.default;
    (tag.label);
    var __VLS_88;
}
{
    const { footer: __VLS_thisSlot } = __VLS_3.slots;
    const __VLS_93 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_94 = __VLS_asFunctionalComponent(__VLS_93, new __VLS_93({
        ...{ 'onClick': {} },
    }));
    const __VLS_95 = __VLS_94({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_94));
    let __VLS_97;
    let __VLS_98;
    let __VLS_99;
    const __VLS_100 = {
        onClick: (__VLS_ctx.handleCancel)
    };
    __VLS_96.slots.default;
    var __VLS_96;
    const __VLS_101 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_102 = __VLS_asFunctionalComponent(__VLS_101, new __VLS_101({
        ...{ 'onClick': {} },
        type: "primary",
        ...{ class: "confirm-btn" },
    }));
    const __VLS_103 = __VLS_102({
        ...{ 'onClick': {} },
        type: "primary",
        ...{ class: "confirm-btn" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_102));
    let __VLS_105;
    let __VLS_106;
    let __VLS_107;
    const __VLS_108 = {
        onClick: (__VLS_ctx.handleConfirm)
    };
    __VLS_104.slots.default;
    var __VLS_104;
}
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['common-selector-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-query-row']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-search-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-content']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-tree-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-table-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['selector-selected-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['selected-head']} */ ;
/** @type {__VLS_StyleScopedClasses['clear-btn']} */ ;
/** @type {__VLS_StyleScopedClasses['selected-tags']} */ ;
/** @type {__VLS_StyleScopedClasses['confirm-btn']} */ ;
// @ts-ignore
var __VLS_63 = __VLS_62;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            CommonQuerySection: CommonQuerySection,
            CommonTreeSection: CommonTreeSection,
            CommonTableSection: CommonTableSection,
            emit: emit,
            keyword: keyword,
            status: status,
            dialogVisible: dialogVisible,
            selectedCount: selectedCount,
            selectedTags: selectedTags,
            handleSearch: handleSearch,
            handleNodeClick: handleNodeClick,
            handleSelectionChange: handleSelectionChange,
            handleClearSelection: handleClearSelection,
            handleRemoveTag: handleRemoveTag,
            handleCancel: handleCancel,
            handleConfirm: handleConfirm,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
const __VLS_component = (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default {};
; /* PartiallyEnd: #4569/main.vue */
