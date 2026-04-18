/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { WarningFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
const router = useRouter();
const activeNav = ref('basic');
const basicSectionRef = ref(null);
const contentSectionRef = ref(null);
const navs = [
    { key: 'basic', label: '基础信息' },
    { key: 'content', label: '模板内容' },
];
const form = reactive({
    templateName: '',
    enabled: true,
    remark: '',
});
const stockUnitOptions = ['个', '件', '斤', '袋', '瓶', '包', '箱', '克'];
const itemCatalog = [
    { itemCode: 'MDWP0001', itemName: '皮皮虾', spec: '', category: '海鲜', stockUnit: '克' },
    { itemCode: 'MDWP0002', itemName: '海天蚝油', spec: '', category: '原材料', stockUnit: '瓶' },
    { itemCode: 'MDWP0003', itemName: '咖啡豆', spec: '', category: '原材料', stockUnit: '袋' },
    { itemCode: 'MDWP0004', itemName: '吸管', spec: '', category: '原材料', stockUnit: '包' },
    { itemCode: 'MDWP0005', itemName: '杯托', spec: '', category: '原材料', stockUnit: '包' },
    { itemCode: 'MDWP0006', itemName: '测试', spec: '123', category: '海鲜', stockUnit: '件' },
    { itemCode: 'MDWP0007', itemName: '物品1', spec: '', category: '原材料', stockUnit: '斤' },
    { itemCode: 'MDWP0008', itemName: '测试固定标签', spec: '标准', category: '蔬菜', stockUnit: '斤' },
    { itemCode: 'MDWP0009', itemName: '玉米', spec: '', category: '蔬菜', stockUnit: '斤' },
    { itemCode: 'MDWP0010', itemName: '鸭血', spec: '', category: '海鲜', stockUnit: '克' },
    { itemCode: 'MDWP0011', itemName: '牛肉卷', spec: '500g', category: '原材料', stockUnit: '袋' },
    { itemCode: 'MDWP0012', itemName: '小白菜', spec: '', category: '蔬菜', stockUnit: '斤' },
];
const rowSeed = ref(2);
const rows = ref([
    {
        id: 1,
        itemQuery: '',
        itemCode: '',
        itemName: '',
        spec: '',
        category: '',
        stockUnit: '',
        remark: '',
    },
]);
const dialogVisible = ref(false);
const dialogQuery = reactive({
    keyword: '',
    tag: '',
    addedFilter: '全部',
});
const categoryOptions = ['海鲜', '原材料', '菜品（系统创建）', '蔬菜'];
const selectedCategories = ref([]);
const dialogCurrentPage = ref(1);
const dialogPageSize = ref(100);
const dialogSelectedCodes = ref([]);
const existingCodeSet = computed(() => new Set(rows.value.map((row) => row.itemCode).filter(Boolean)));
const dialogFilteredRows = computed(() => {
    const keyword = dialogQuery.keyword.trim().toLowerCase();
    return itemCatalog.filter((item) => {
        const matchKeyword = !keyword
            || item.itemCode.toLowerCase().includes(keyword)
            || item.itemName.toLowerCase().includes(keyword)
            || item.category.toLowerCase().includes(keyword);
        const matchCategory = !selectedCategories.value.length || selectedCategories.value.includes(item.category);
        const added = existingCodeSet.value.has(item.itemCode);
        const matchAdded = dialogQuery.addedFilter === '全部'
            || (dialogQuery.addedFilter === '已添加' && added)
            || (dialogQuery.addedFilter === '未添加' && !added);
        return matchKeyword && matchCategory && matchAdded;
    });
});
const dialogPagedRows = computed(() => {
    const start = (dialogCurrentPage.value - 1) * dialogPageSize.value;
    return dialogFilteredRows.value.slice(start, start + dialogPageSize.value);
});
const selectedDialogItems = computed(() => itemCatalog.filter((item) => dialogSelectedCodes.value.includes(item.itemCode)));
const handleDialogSelectionChange = (list) => {
    dialogSelectedCodes.value = list.map((item) => item.itemCode);
};
const querySearchItem = (queryString, cb) => {
    const keyword = queryString.trim().toLowerCase();
    const result = itemCatalog
        .filter((item) => !keyword || item.itemCode.toLowerCase().includes(keyword) || item.itemName.toLowerCase().includes(keyword))
        .map((item) => ({
        value: `${item.itemCode} / ${item.itemName}`,
        raw: item,
    }));
    cb(result);
};
const applyItemToRow = (row, matched) => {
    if (!matched) {
        row.itemCode = '';
        row.itemName = '';
        row.spec = '';
        row.category = '';
        if (!row.stockUnit) {
            row.stockUnit = '';
        }
        return;
    }
    row.itemCode = matched.itemCode;
    row.itemName = matched.itemName;
    row.spec = matched.spec;
    row.category = matched.category;
    if (!row.stockUnit) {
        row.stockUnit = matched.stockUnit;
    }
};
const handleSelectItem = (row, selected) => {
    row.itemQuery = selected.value;
    applyItemToRow(row, selected.raw);
};
const handleBlurItemQuery = (row) => {
    const keyword = row.itemQuery.trim().toLowerCase();
    if (!keyword) {
        applyItemToRow(row, undefined);
        return;
    }
    const matched = itemCatalog.find((item) => item.itemCode.toLowerCase() === keyword || item.itemName.toLowerCase() === keyword);
    applyItemToRow(row, matched);
};
const addRow = (index) => {
    const target = typeof index === 'number' ? index + 1 : rows.value.length;
    rows.value.splice(target, 0, {
        id: rowSeed.value,
        itemQuery: '',
        itemCode: '',
        itemName: '',
        spec: '',
        category: '',
        stockUnit: '',
        remark: '',
    });
    rowSeed.value += 1;
};
const removeRow = (index) => {
    if (rows.value.length <= 1) {
        ElMessage.warning('至少保留一条物品');
        return;
    }
    rows.value.splice(index, 1);
};
const openDialog = () => {
    dialogSelectedCodes.value = [];
    dialogVisible.value = true;
};
const clearDialogSelection = () => {
    dialogSelectedCodes.value = [];
};
const confirmDialog = () => {
    if (!dialogSelectedCodes.value.length) {
        ElMessage.warning('请至少选择一个物品');
        return;
    }
    const appendRows = itemCatalog
        .filter((item) => dialogSelectedCodes.value.includes(item.itemCode))
        .filter((item) => !existingCodeSet.value.has(item.itemCode))
        .map((item) => ({
        id: rowSeed.value++,
        itemQuery: `${item.itemCode} / ${item.itemName}`,
        itemCode: item.itemCode,
        itemName: item.itemName,
        spec: item.spec,
        category: item.category,
        stockUnit: item.stockUnit,
        remark: '',
    }));
    if (!appendRows.length) {
        ElMessage.info('所选物品已在模板中');
        dialogVisible.value = false;
        return;
    }
    rows.value.push(...appendRows);
    ElMessage.success(`已添加 ${appendRows.length} 条物品`);
    dialogVisible.value = false;
};
const handleImport = () => {
    const imported = itemCatalog
        .slice(0, 3)
        .filter((item) => !existingCodeSet.value.has(item.itemCode))
        .map((item) => ({
        id: rowSeed.value++,
        itemQuery: `${item.itemCode} / ${item.itemName}`,
        itemCode: item.itemCode,
        itemName: item.itemName,
        spec: item.spec,
        category: item.category,
        stockUnit: item.stockUnit,
        remark: '',
    }));
    rows.value.push(...imported);
    ElMessage.success(`导入完成，新增 ${imported.length} 条`);
};
const scrollToSection = (key) => {
    activeNav.value = key;
    const target = key === 'basic' ? basicSectionRef.value : contentSectionRef.value;
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const handleBack = () => {
    router.push('/inventory/5/1');
};
const handleSaveDraft = () => {
    ElMessage.success('库存模板草稿已保存');
};
const handleSave = () => {
    if (!form.templateName.trim()) {
        ElMessage.warning('请输入模板名称');
        return;
    }
    if (!rows.value.some((row) => row.itemCode)) {
        ElMessage.warning('请添加至少一个物品');
        return;
    }
    ElMessage.success('库存模板保存成功');
    router.push('/inventory/5/1');
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['inventory-template-edit-table']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-content']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-left']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-basic-grid']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-create-page" },
});
/** @type {[typeof FixedActionBreadcrumb, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(FixedActionBreadcrumb, new FixedActionBreadcrumb({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeNav),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.navs),
    activeKey: (__VLS_ctx.activeNav),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
let __VLS_3;
let __VLS_4;
let __VLS_5;
const __VLS_6 = {
    onBack: (__VLS_ctx.handleBack)
};
const __VLS_7 = {
    onSaveDraft: (__VLS_ctx.handleSaveDraft)
};
const __VLS_8 = {
    onSave: (__VLS_ctx.handleSave)
};
const __VLS_9 = {
    onNavigate: (__VLS_ctx.scrollToSection)
};
var __VLS_2;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel form-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-template-tip" },
});
const __VLS_10 = {}.ElIcon;
/** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({}));
const __VLS_12 = __VLS_11({}, ...__VLS_functionalComponentArgsRest(__VLS_11));
__VLS_13.slots.default;
const __VLS_14 = {}.WarningFilled;
/** @type {[typeof __VLS_components.WarningFilled, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({}));
const __VLS_16 = __VLS_15({}, ...__VLS_functionalComponentArgsRest(__VLS_15));
var __VLS_13;
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "basicSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.basicSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
const __VLS_18 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
    labelWidth: "92px",
    ...{ class: "item-create-form inventory-template-create-form" },
}));
const __VLS_20 = __VLS_19({
    labelWidth: "92px",
    ...{ class: "item-create-form inventory-template-create-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
__VLS_21.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid inventory-template-basic-grid" },
});
const __VLS_22 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
    label: "模板名称",
    required: true,
}));
const __VLS_24 = __VLS_23({
    label: "模板名称",
    required: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
__VLS_25.slots.default;
const __VLS_26 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
    modelValue: (__VLS_ctx.form.templateName),
    placeholder: "请输入模板名称",
    maxlength: "30",
}));
const __VLS_28 = __VLS_27({
    modelValue: (__VLS_ctx.form.templateName),
    placeholder: "请输入模板名称",
    maxlength: "30",
}, ...__VLS_functionalComponentArgsRest(__VLS_27));
var __VLS_25;
const __VLS_30 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
    label: "模板状态",
}));
const __VLS_32 = __VLS_31({
    label: "模板状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_31));
__VLS_33.slots.default;
const __VLS_34 = {}.ElSwitch;
/** @type {[typeof __VLS_components.ElSwitch, typeof __VLS_components.elSwitch, ]} */ ;
// @ts-ignore
const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
    modelValue: (__VLS_ctx.form.enabled),
}));
const __VLS_36 = __VLS_35({
    modelValue: (__VLS_ctx.form.enabled),
}, ...__VLS_functionalComponentArgsRest(__VLS_35));
var __VLS_33;
const __VLS_38 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
    label: "备注",
}));
const __VLS_40 = __VLS_39({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_39));
__VLS_41.slots.default;
const __VLS_42 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "在此填写备注信息...",
    maxlength: "100",
}));
const __VLS_44 = __VLS_43({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "在此填写备注信息...",
    maxlength: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
var __VLS_41;
var __VLS_21;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "contentSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.contentSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_46 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
    ...{ 'onClick': {} },
}));
const __VLS_48 = __VLS_47({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_47));
let __VLS_50;
let __VLS_51;
let __VLS_52;
const __VLS_53 = {
    onClick: (__VLS_ctx.openDialog)
};
__VLS_49.slots.default;
var __VLS_49;
const __VLS_54 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    ...{ 'onClick': {} },
}));
const __VLS_56 = __VLS_55({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
let __VLS_58;
let __VLS_59;
let __VLS_60;
const __VLS_61 = {
    onClick: (__VLS_ctx.handleImport)
};
__VLS_57.slots.default;
var __VLS_57;
const __VLS_62 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    data: (__VLS_ctx.rows),
    border: true,
    stripe: true,
    ...{ class: "erp-table inventory-template-edit-table" },
    fit: (false),
}));
const __VLS_64 = __VLS_63({
    data: (__VLS_ctx.rows),
    border: true,
    stripe: true,
    ...{ class: "erp-table inventory-template-edit-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
__VLS_65.slots.default;
const __VLS_66 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_68 = __VLS_67({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
const __VLS_70 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    label: "操作",
    width: "86",
    fixed: "left",
}));
const __VLS_72 = __VLS_71({
    label: "操作",
    width: "86",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
__VLS_73.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_73.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_74 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_76 = __VLS_75({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_75));
    let __VLS_78;
    let __VLS_79;
    let __VLS_80;
    const __VLS_81 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addRow($index);
        }
    };
    __VLS_77.slots.default;
    var __VLS_77;
    const __VLS_82 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_84 = __VLS_83({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_83));
    let __VLS_86;
    let __VLS_87;
    let __VLS_88;
    const __VLS_89 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeRow($index);
        }
    };
    __VLS_85.slots.default;
    var __VLS_85;
}
var __VLS_73;
const __VLS_90 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    label: "物品编码",
    minWidth: "220",
}));
const __VLS_92 = __VLS_91({
    label: "物品编码",
    minWidth: "220",
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
__VLS_93.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_93.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_94 = {}.ElAutocomplete;
    /** @type {[typeof __VLS_components.ElAutocomplete, typeof __VLS_components.elAutocomplete, ]} */ ;
    // @ts-ignore
    const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
        ...{ 'onSelect': {} },
        ...{ 'onBlur': {} },
        modelValue: (row.itemQuery),
        fetchSuggestions: (__VLS_ctx.querySearchItem),
        placeholder: "根据编码名称检索",
        clearable: true,
    }));
    const __VLS_96 = __VLS_95({
        ...{ 'onSelect': {} },
        ...{ 'onBlur': {} },
        modelValue: (row.itemQuery),
        fetchSuggestions: (__VLS_ctx.querySearchItem),
        placeholder: "根据编码名称检索",
        clearable: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_95));
    let __VLS_98;
    let __VLS_99;
    let __VLS_100;
    const __VLS_101 = {
        onSelect: (...[$event]) => {
            __VLS_ctx.handleSelectItem(row, $event);
        }
    };
    const __VLS_102 = {
        onBlur: (...[$event]) => {
            __VLS_ctx.handleBlurItemQuery(row);
        }
    };
    var __VLS_97;
}
var __VLS_93;
const __VLS_103 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_104 = __VLS_asFunctionalComponent(__VLS_103, new __VLS_103({
    label: "物品名称",
    minWidth: "140",
}));
const __VLS_105 = __VLS_104({
    label: "物品名称",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_104));
__VLS_106.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_106.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.itemName || '-');
}
var __VLS_106;
const __VLS_107 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_108 = __VLS_asFunctionalComponent(__VLS_107, new __VLS_107({
    label: "规格型号",
    minWidth: "110",
}));
const __VLS_109 = __VLS_108({
    label: "规格型号",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_108));
__VLS_110.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_110.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.spec || '-');
}
var __VLS_110;
const __VLS_111 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_112 = __VLS_asFunctionalComponent(__VLS_111, new __VLS_111({
    label: "物品类别",
    minWidth: "110",
}));
const __VLS_113 = __VLS_112({
    label: "物品类别",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_112));
__VLS_114.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_114.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    (row.category || '-');
}
var __VLS_114;
const __VLS_115 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_116 = __VLS_asFunctionalComponent(__VLS_115, new __VLS_115({
    label: "库存单位",
    minWidth: "120",
}));
const __VLS_117 = __VLS_116({
    label: "库存单位",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_116));
__VLS_118.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_118.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_119 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_120 = __VLS_asFunctionalComponent(__VLS_119, new __VLS_119({
        modelValue: (row.stockUnit),
        placeholder: "请选择单位",
    }));
    const __VLS_121 = __VLS_120({
        modelValue: (row.stockUnit),
        placeholder: "请选择单位",
    }, ...__VLS_functionalComponentArgsRest(__VLS_120));
    __VLS_122.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.stockUnitOptions))) {
        const __VLS_123 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_124 = __VLS_asFunctionalComponent(__VLS_123, new __VLS_123({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_125 = __VLS_124({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_124));
    }
    var __VLS_122;
}
var __VLS_118;
const __VLS_127 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_128 = __VLS_asFunctionalComponent(__VLS_127, new __VLS_127({
    label: "备注",
    minWidth: "180",
}));
const __VLS_129 = __VLS_128({
    label: "备注",
    minWidth: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_128));
__VLS_130.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_130.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_131 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_132 = __VLS_asFunctionalComponent(__VLS_131, new __VLS_131({
        modelValue: (row.remark),
        placeholder: "最多30个字",
        maxlength: "30",
    }));
    const __VLS_133 = __VLS_132({
        modelValue: (row.remark),
        placeholder: "最多30个字",
        maxlength: "30",
    }, ...__VLS_functionalComponentArgsRest(__VLS_132));
}
var __VLS_130;
var __VLS_65;
const __VLS_135 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_136 = __VLS_asFunctionalComponent(__VLS_135, new __VLS_135({
    modelValue: (__VLS_ctx.dialogVisible),
    title: "选择物品",
    width: "1240px",
    ...{ class: "standard-form-dialog inventory-template-item-dialog" },
    destroyOnClose: true,
}));
const __VLS_137 = __VLS_136({
    modelValue: (__VLS_ctx.dialogVisible),
    title: "选择物品",
    width: "1240px",
    ...{ class: "standard-form-dialog inventory-template-item-dialog" },
    destroyOnClose: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_136));
__VLS_138.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-dialog-toolbar" },
});
const __VLS_139 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_140 = __VLS_asFunctionalComponent(__VLS_139, new __VLS_139({
    inline: true,
    ...{ class: "compact-filter-bar" },
    model: (__VLS_ctx.dialogQuery),
}));
const __VLS_141 = __VLS_140({
    inline: true,
    ...{ class: "compact-filter-bar" },
    model: (__VLS_ctx.dialogQuery),
}, ...__VLS_functionalComponentArgsRest(__VLS_140));
__VLS_142.slots.default;
const __VLS_143 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_144 = __VLS_asFunctionalComponent(__VLS_143, new __VLS_143({
    label: "物品",
}));
const __VLS_145 = __VLS_144({
    label: "物品",
}, ...__VLS_functionalComponentArgsRest(__VLS_144));
__VLS_146.slots.default;
const __VLS_147 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_148 = __VLS_asFunctionalComponent(__VLS_147, new __VLS_147({
    modelValue: (__VLS_ctx.dialogQuery.keyword),
    placeholder: "物品名称/编码/助记码/...",
    ...{ style: {} },
}));
const __VLS_149 = __VLS_148({
    modelValue: (__VLS_ctx.dialogQuery.keyword),
    placeholder: "物品名称/编码/助记码/...",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_148));
var __VLS_146;
const __VLS_151 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_152 = __VLS_asFunctionalComponent(__VLS_151, new __VLS_151({
    label: "物品标签",
}));
const __VLS_153 = __VLS_152({
    label: "物品标签",
}, ...__VLS_functionalComponentArgsRest(__VLS_152));
__VLS_154.slots.default;
const __VLS_155 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_156 = __VLS_asFunctionalComponent(__VLS_155, new __VLS_155({
    modelValue: (__VLS_ctx.dialogQuery.tag),
    placeholder: "请选择",
    clearable: true,
    ...{ style: {} },
}));
const __VLS_157 = __VLS_156({
    modelValue: (__VLS_ctx.dialogQuery.tag),
    placeholder: "请选择",
    clearable: true,
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_156));
__VLS_158.slots.default;
const __VLS_159 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_160 = __VLS_asFunctionalComponent(__VLS_159, new __VLS_159({
    label: "海鲜",
    value: "海鲜",
}));
const __VLS_161 = __VLS_160({
    label: "海鲜",
    value: "海鲜",
}, ...__VLS_functionalComponentArgsRest(__VLS_160));
const __VLS_163 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_164 = __VLS_asFunctionalComponent(__VLS_163, new __VLS_163({
    label: "原材料",
    value: "原材料",
}));
const __VLS_165 = __VLS_164({
    label: "原材料",
    value: "原材料",
}, ...__VLS_functionalComponentArgsRest(__VLS_164));
const __VLS_167 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_168 = __VLS_asFunctionalComponent(__VLS_167, new __VLS_167({
    label: "蔬菜",
    value: "蔬菜",
}));
const __VLS_169 = __VLS_168({
    label: "蔬菜",
    value: "蔬菜",
}, ...__VLS_functionalComponentArgsRest(__VLS_168));
var __VLS_158;
var __VLS_154;
const __VLS_171 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_172 = __VLS_asFunctionalComponent(__VLS_171, new __VLS_171({
    label: "是否已添加",
}));
const __VLS_173 = __VLS_172({
    label: "是否已添加",
}, ...__VLS_functionalComponentArgsRest(__VLS_172));
__VLS_174.slots.default;
const __VLS_175 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_176 = __VLS_asFunctionalComponent(__VLS_175, new __VLS_175({
    modelValue: (__VLS_ctx.dialogQuery.addedFilter),
}));
const __VLS_177 = __VLS_176({
    modelValue: (__VLS_ctx.dialogQuery.addedFilter),
}, ...__VLS_functionalComponentArgsRest(__VLS_176));
__VLS_178.slots.default;
const __VLS_179 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_180 = __VLS_asFunctionalComponent(__VLS_179, new __VLS_179({
    label: "全部",
}));
const __VLS_181 = __VLS_180({
    label: "全部",
}, ...__VLS_functionalComponentArgsRest(__VLS_180));
__VLS_182.slots.default;
var __VLS_182;
const __VLS_183 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_184 = __VLS_asFunctionalComponent(__VLS_183, new __VLS_183({
    label: "已添加",
}));
const __VLS_185 = __VLS_184({
    label: "已添加",
}, ...__VLS_functionalComponentArgsRest(__VLS_184));
__VLS_186.slots.default;
var __VLS_186;
const __VLS_187 = {}.ElRadio;
/** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
// @ts-ignore
const __VLS_188 = __VLS_asFunctionalComponent(__VLS_187, new __VLS_187({
    label: "未添加",
}));
const __VLS_189 = __VLS_188({
    label: "未添加",
}, ...__VLS_functionalComponentArgsRest(__VLS_188));
__VLS_190.slots.default;
var __VLS_190;
var __VLS_178;
var __VLS_174;
const __VLS_191 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_192 = __VLS_asFunctionalComponent(__VLS_191, new __VLS_191({}));
const __VLS_193 = __VLS_192({}, ...__VLS_functionalComponentArgsRest(__VLS_192));
__VLS_194.slots.default;
const __VLS_195 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_196 = __VLS_asFunctionalComponent(__VLS_195, new __VLS_195({
    type: "primary",
}));
const __VLS_197 = __VLS_196({
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_196));
__VLS_198.slots.default;
var __VLS_198;
var __VLS_194;
var __VLS_142;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-dialog-content" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "inventory-dialog-left" },
});
const __VLS_199 = {}.ElCheckboxGroup;
/** @type {[typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, typeof __VLS_components.ElCheckboxGroup, typeof __VLS_components.elCheckboxGroup, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    modelValue: (__VLS_ctx.selectedCategories),
}));
const __VLS_201 = __VLS_200({
    modelValue: (__VLS_ctx.selectedCategories),
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
__VLS_202.slots.default;
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.categoryOptions))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (item),
        ...{ class: "inventory-category-item" },
    });
    const __VLS_203 = {}.ElCheckbox;
    /** @type {[typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, typeof __VLS_components.ElCheckbox, typeof __VLS_components.elCheckbox, ]} */ ;
    // @ts-ignore
    const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
        label: (item),
    }));
    const __VLS_205 = __VLS_204({
        label: (item),
    }, ...__VLS_functionalComponentArgsRest(__VLS_204));
    __VLS_206.slots.default;
    (item);
    var __VLS_206;
}
var __VLS_202;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "inventory-dialog-main" },
});
const __VLS_207 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.dialogPagedRows),
    border: true,
    stripe: true,
    height: "430",
    ...{ class: "erp-table" },
}));
const __VLS_209 = __VLS_208({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.dialogPagedRows),
    border: true,
    stripe: true,
    height: "430",
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
let __VLS_211;
let __VLS_212;
let __VLS_213;
const __VLS_214 = {
    onSelectionChange: (__VLS_ctx.handleDialogSelectionChange)
};
__VLS_210.slots.default;
const __VLS_215 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    type: "selection",
    width: "46",
    fixed: "left",
}));
const __VLS_217 = __VLS_216({
    type: "selection",
    width: "46",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
const __VLS_219 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_221 = __VLS_220({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
const __VLS_223 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "130",
}));
const __VLS_225 = __VLS_224({
    prop: "itemCode",
    label: "物品编码",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
const __VLS_227 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
    prop: "itemName",
    label: "物品名称",
    minWidth: "130",
}));
const __VLS_229 = __VLS_228({
    prop: "itemName",
    label: "物品名称",
    minWidth: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_228));
const __VLS_231 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
}));
const __VLS_233 = __VLS_232({
    prop: "spec",
    label: "规格型号",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
const __VLS_235 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
    prop: "category",
    label: "物品类别",
    minWidth: "120",
}));
const __VLS_237 = __VLS_236({
    prop: "category",
    label: "物品类别",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_236));
const __VLS_239 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
}));
const __VLS_241 = __VLS_240({
    prop: "stockUnit",
    label: "库存单位",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
var __VLS_210;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-pagination-meta" },
});
(__VLS_ctx.dialogFilteredRows.length);
const __VLS_243 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.dialogCurrentPage),
    pageSize: (__VLS_ctx.dialogPageSize),
    pageSizes: ([20, 50, 100]),
    total: (__VLS_ctx.dialogFilteredRows.length),
    background: true,
    small: true,
    layout: "prev, pager, next, sizes",
}));
const __VLS_245 = __VLS_244({
    ...{ 'onCurrentChange': {} },
    ...{ 'onSizeChange': {} },
    currentPage: (__VLS_ctx.dialogCurrentPage),
    pageSize: (__VLS_ctx.dialogPageSize),
    pageSizes: ([20, 50, 100]),
    total: (__VLS_ctx.dialogFilteredRows.length),
    background: true,
    small: true,
    layout: "prev, pager, next, sizes",
}, ...__VLS_functionalComponentArgsRest(__VLS_244));
let __VLS_247;
let __VLS_248;
let __VLS_249;
const __VLS_250 = {
    onCurrentChange: ((p) => { __VLS_ctx.dialogCurrentPage = p; })
};
const __VLS_251 = {
    onSizeChange: ((s) => { __VLS_ctx.dialogPageSize = s; __VLS_ctx.dialogCurrentPage = 1; })
};
var __VLS_246;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-dialog-selected" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-selected-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
(__VLS_ctx.selectedDialogItems.length);
const __VLS_252 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_253 = __VLS_asFunctionalComponent(__VLS_252, new __VLS_252({
    ...{ 'onClick': {} },
    text: true,
    type: "danger",
}));
const __VLS_254 = __VLS_253({
    ...{ 'onClick': {} },
    text: true,
    type: "danger",
}, ...__VLS_functionalComponentArgsRest(__VLS_253));
let __VLS_256;
let __VLS_257;
let __VLS_258;
const __VLS_259 = {
    onClick: (__VLS_ctx.clearDialogSelection)
};
__VLS_255.slots.default;
var __VLS_255;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "inventory-selected-tags" },
});
for (const [item] of __VLS_getVForSourceType((__VLS_ctx.selectedDialogItems))) {
    const __VLS_260 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_261 = __VLS_asFunctionalComponent(__VLS_260, new __VLS_260({
        key: (item.itemCode),
        size: "small",
    }));
    const __VLS_262 = __VLS_261({
        key: (item.itemCode),
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_261));
    __VLS_263.slots.default;
    (item.itemCode);
    (item.itemName);
    var __VLS_263;
}
{
    const { footer: __VLS_thisSlot } = __VLS_138.slots;
    const __VLS_264 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_265 = __VLS_asFunctionalComponent(__VLS_264, new __VLS_264({
        ...{ 'onClick': {} },
    }));
    const __VLS_266 = __VLS_265({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_265));
    let __VLS_268;
    let __VLS_269;
    let __VLS_270;
    const __VLS_271 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialogVisible = false;
        }
    };
    __VLS_267.slots.default;
    var __VLS_267;
    const __VLS_272 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_273 = __VLS_asFunctionalComponent(__VLS_272, new __VLS_272({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_274 = __VLS_273({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_273));
    let __VLS_276;
    let __VLS_277;
    let __VLS_278;
    const __VLS_279 = {
        onClick: (__VLS_ctx.confirmDialog)
    };
    __VLS_275.slots.default;
    var __VLS_275;
}
var __VLS_138;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-tip']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-basic-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-edit-table']} */ ;
/** @type {__VLS_StyleScopedClasses['standard-form-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-template-item-dialog']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['compact-filter-bar']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-content']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-left']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-category-item']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-main']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-dialog-selected']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-selected-head']} */ ;
/** @type {__VLS_StyleScopedClasses['inventory-selected-tags']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            WarningFilled: WarningFilled,
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            activeNav: activeNav,
            basicSectionRef: basicSectionRef,
            contentSectionRef: contentSectionRef,
            navs: navs,
            form: form,
            stockUnitOptions: stockUnitOptions,
            rows: rows,
            dialogVisible: dialogVisible,
            dialogQuery: dialogQuery,
            categoryOptions: categoryOptions,
            selectedCategories: selectedCategories,
            dialogCurrentPage: dialogCurrentPage,
            dialogPageSize: dialogPageSize,
            dialogFilteredRows: dialogFilteredRows,
            dialogPagedRows: dialogPagedRows,
            selectedDialogItems: selectedDialogItems,
            handleDialogSelectionChange: handleDialogSelectionChange,
            querySearchItem: querySearchItem,
            handleSelectItem: handleSelectItem,
            handleBlurItemQuery: handleBlurItemQuery,
            addRow: addRow,
            removeRow: removeRow,
            openDialog: openDialog,
            clearDialogSelection: clearDialogSelection,
            confirmDialog: confirmDialog,
            handleImport: handleImport,
            scrollToSection: scrollToSection,
            handleBack: handleBack,
            handleSaveDraft: handleSaveDraft,
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
