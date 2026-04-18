/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
const router = useRouter();
const activeNav = ref('basic');
const basicSectionRef = ref(null);
const itemSectionRef = ref(null);
const pricingCode = `PJ-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-001`;
const itemLookupRows = [
    {
        itemCode: 'ITEM-001',
        itemName: '鸡胸肉',
        spec: '2kg/袋',
        category: '肉类',
        purchaseUnit: '袋',
        spotPrice: '46.00',
        previousPrice: '44.50',
        taxRate: '9',
        priceLimit: '45.00-49.00',
    },
    {
        itemCode: 'ITEM-002',
        itemName: '牛腩',
        spec: '5kg/箱',
        category: '冻品',
        purchaseUnit: '箱',
        spotPrice: '168.00',
        previousPrice: '172.00',
        taxRate: '9',
        priceLimit: '160.00-175.00',
    },
    {
        itemCode: 'ITEM-003',
        itemName: '酸梅汤',
        spec: '500ml*12瓶',
        category: '饮品',
        purchaseUnit: '箱',
        spotPrice: '72.00',
        previousPrice: '70.00',
        taxRate: '13',
        priceLimit: '68.00-75.00',
    },
];
const sectionNavs = [
    { key: 'basic', label: '基础内容' },
    { key: 'items', label: '定价物品' },
];
const pricingTypeOptions = ['通用', '非通用'];
const supplierOptions = ['鲜达食品', '优选农场', '沪上冷链', '盒马包材'];
const attachmentTree = [
    {
        value: 'contract-root',
        label: '合同附件',
        children: [
            { value: 'contract-1', label: '鲜达食品年度合同.pdf' },
            { value: 'contract-2', label: '优选农场定价补充协议.pdf' },
        ],
    },
];
const form = reactive({
    pricingName: '',
    pricingType: '通用',
    supplier: '',
    effectiveDate: '',
    expireDate: '',
    contractAttachment: '',
    remark: '',
});
const itemRows = ref([
    {
        id: 1,
        itemKeyword: '',
        itemCode: '',
        itemName: '',
        spec: '',
        category: '',
        purchaseUnit: '',
        spotPrice: '',
        previousPrice: '',
        quantityRange: '',
        taxIncludedPrice: '',
        taxRate: '',
        taxExcludedPrice: '',
        priceDiffRatio: '',
        priceLimit: '',
        remark: '',
    },
]);
const scrollToSection = (key) => {
    activeNav.value = key;
    const target = key === 'basic' ? basicSectionRef.value : itemSectionRef.value;
    target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};
const handleBack = () => {
    router.push('/purchase/1/1');
};
const handleSaveDraft = () => {
    ElMessage.success('采购定价单草稿已保存');
};
const handleSave = () => {
    ElMessage.success('采购定价单保存成功');
};
const handleToolbarAction = (action) => {
    ElMessage.info(`${action}功能待接入`);
};
const fillDerivedPricingFields = (row) => {
    const keyword = row.itemKeyword.trim().toLowerCase();
    if (!keyword) {
        row.itemCode = '';
        row.itemName = '';
        row.spec = '';
        row.category = '';
        row.purchaseUnit = '';
        row.spotPrice = '';
        row.previousPrice = '';
        row.taxRate = '';
        row.priceLimit = '';
        return;
    }
    const matched = itemLookupRows.find((item) => (item.itemCode.toLowerCase() === keyword || item.itemName.toLowerCase() === keyword));
    if (!matched) {
        ElMessage.warning('未查询到对应物品，请检查物品编码或名称');
        row.itemCode = '';
        row.itemName = '';
        row.spec = '';
        row.category = '';
        row.purchaseUnit = '';
        row.spotPrice = '';
        row.previousPrice = '';
        row.taxRate = '';
        row.priceLimit = '';
        return;
    }
    row.itemCode = matched.itemCode;
    row.itemName = matched.itemName;
    row.spec = matched.spec;
    row.category = matched.category;
    row.purchaseUnit = matched.purchaseUnit;
    row.spotPrice = matched.spotPrice;
    row.previousPrice = matched.previousPrice;
    row.taxRate = matched.taxRate;
    row.priceLimit = matched.priceLimit;
    if (!row.taxIncludedPrice) {
        row.taxIncludedPrice = matched.spotPrice;
    }
    if (!row.taxExcludedPrice) {
        row.taxExcludedPrice = matched.spotPrice;
    }
};
const addItemRow = (index) => {
    itemRows.value.splice(index + 1, 0, {
        id: Date.now() + index,
        itemKeyword: '',
        itemCode: '',
        itemName: '',
        spec: '',
        category: '',
        purchaseUnit: '',
        spotPrice: '',
        previousPrice: '',
        quantityRange: '',
        taxIncludedPrice: '',
        taxRate: '',
        taxExcludedPrice: '',
        priceDiffRatio: '',
        priceLimit: '',
        remark: '',
    });
};
const removeItemRow = (index) => {
    if (itemRows.value.length === 1) {
        ElMessage.warning('至少保留一条定价物品');
        return;
    }
    itemRows.value.splice(index, 1);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__inner']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['el-textarea__inner']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__wrapper']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-table']} */ ;
/** @type {__VLS_StyleScopedClasses['el-input__inner']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-table']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-grid']} */ ;
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
    navs: (__VLS_ctx.sectionNavs),
    activeKey: (__VLS_ctx.activeNav),
}));
const __VLS_1 = __VLS_0({
    ...{ 'onBack': {} },
    ...{ 'onSaveDraft': {} },
    ...{ 'onSave': {} },
    ...{ 'onNavigate': {} },
    navs: (__VLS_ctx.sectionNavs),
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
    ref: "basicSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.basicSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
const __VLS_10 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
    model: (__VLS_ctx.form),
    labelWidth: "96px",
    ...{ class: "item-create-form purchase-pricing-form" },
}));
const __VLS_12 = __VLS_11({
    model: (__VLS_ctx.form),
    labelWidth: "96px",
    ...{ class: "item-create-form purchase-pricing-form" },
}, ...__VLS_functionalComponentArgsRest(__VLS_11));
__VLS_13.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "item-form-grid purchase-pricing-grid" },
});
const __VLS_14 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
    label: "定价单编号",
}));
const __VLS_16 = __VLS_15({
    label: "定价单编号",
}, ...__VLS_functionalComponentArgsRest(__VLS_15));
__VLS_17.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
(__VLS_ctx.pricingCode);
var __VLS_17;
const __VLS_18 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_19 = __VLS_asFunctionalComponent(__VLS_18, new __VLS_18({
    label: "定价单名称",
}));
const __VLS_20 = __VLS_19({
    label: "定价单名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_19));
__VLS_21.slots.default;
const __VLS_22 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({
    modelValue: (__VLS_ctx.form.pricingName),
    placeholder: "请输入定价单名称",
}));
const __VLS_24 = __VLS_23({
    modelValue: (__VLS_ctx.form.pricingName),
    placeholder: "请输入定价单名称",
}, ...__VLS_functionalComponentArgsRest(__VLS_23));
var __VLS_21;
const __VLS_26 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({
    label: "定价单类型",
}));
const __VLS_28 = __VLS_27({
    label: "定价单类型",
}, ...__VLS_functionalComponentArgsRest(__VLS_27));
__VLS_29.slots.default;
const __VLS_30 = {}.ElRadioGroup;
/** @type {[typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, typeof __VLS_components.ElRadioGroup, typeof __VLS_components.elRadioGroup, ]} */ ;
// @ts-ignore
const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
    modelValue: (__VLS_ctx.form.pricingType),
}));
const __VLS_32 = __VLS_31({
    modelValue: (__VLS_ctx.form.pricingType),
}, ...__VLS_functionalComponentArgsRest(__VLS_31));
__VLS_33.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.pricingTypeOptions))) {
    const __VLS_34 = {}.ElRadio;
    /** @type {[typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, typeof __VLS_components.ElRadio, typeof __VLS_components.elRadio, ]} */ ;
    // @ts-ignore
    const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
        key: (option),
        label: (option),
    }));
    const __VLS_36 = __VLS_35({
        key: (option),
        label: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_35));
    __VLS_37.slots.default;
    (option);
    var __VLS_37;
}
var __VLS_33;
var __VLS_29;
const __VLS_38 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_39 = __VLS_asFunctionalComponent(__VLS_38, new __VLS_38({
    label: "供应商",
}));
const __VLS_40 = __VLS_39({
    label: "供应商",
}, ...__VLS_functionalComponentArgsRest(__VLS_39));
__VLS_41.slots.default;
const __VLS_42 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
    modelValue: (__VLS_ctx.form.supplier),
    placeholder: "请选择供应商",
    ...{ style: {} },
}));
const __VLS_44 = __VLS_43({
    modelValue: (__VLS_ctx.form.supplier),
    placeholder: "请选择供应商",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_43));
__VLS_45.slots.default;
for (const [option] of __VLS_getVForSourceType((__VLS_ctx.supplierOptions))) {
    const __VLS_46 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
        key: (option),
        label: (option),
        value: (option),
    }));
    const __VLS_48 = __VLS_47({
        key: (option),
        label: (option),
        value: (option),
    }, ...__VLS_functionalComponentArgsRest(__VLS_47));
}
var __VLS_45;
var __VLS_41;
const __VLS_50 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
    label: "价格生效日期",
}));
const __VLS_52 = __VLS_51({
    label: "价格生效日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_51));
__VLS_53.slots.default;
const __VLS_54 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
    modelValue: (__VLS_ctx.form.effectiveDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择价格生效日期",
    ...{ style: {} },
}));
const __VLS_56 = __VLS_55({
    modelValue: (__VLS_ctx.form.effectiveDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择价格生效日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_55));
var __VLS_53;
const __VLS_58 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
    label: "价格失效日期",
}));
const __VLS_60 = __VLS_59({
    label: "价格失效日期",
}, ...__VLS_functionalComponentArgsRest(__VLS_59));
__VLS_61.slots.default;
const __VLS_62 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
    modelValue: (__VLS_ctx.form.expireDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择价格失效日期",
    ...{ style: {} },
}));
const __VLS_64 = __VLS_63({
    modelValue: (__VLS_ctx.form.expireDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
    placeholder: "请选择价格失效日期",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_63));
var __VLS_61;
const __VLS_66 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
    label: "定价启用状态",
}));
const __VLS_68 = __VLS_67({
    label: "定价启用状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_67));
__VLS_69.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_69;
const __VLS_70 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
    label: "定价单状态",
}));
const __VLS_72 = __VLS_71({
    label: "定价单状态",
}, ...__VLS_functionalComponentArgsRest(__VLS_71));
__VLS_73.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_73;
const __VLS_74 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
    label: "定价写入结果",
}));
const __VLS_76 = __VLS_75({
    label: "定价写入结果",
}, ...__VLS_functionalComponentArgsRest(__VLS_75));
__VLS_77.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_77;
const __VLS_78 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
    label: "创建方式",
}));
const __VLS_80 = __VLS_79({
    label: "创建方式",
}, ...__VLS_functionalComponentArgsRest(__VLS_79));
__VLS_81.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_81;
const __VLS_82 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
    label: "合同附件",
}));
const __VLS_84 = __VLS_83({
    label: "合同附件",
}, ...__VLS_functionalComponentArgsRest(__VLS_83));
__VLS_85.slots.default;
const __VLS_86 = {}.ElTreeSelect;
/** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
// @ts-ignore
const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
    modelValue: (__VLS_ctx.form.contractAttachment),
    data: (__VLS_ctx.attachmentTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    placeholder: "请选择合同附件",
    ...{ style: {} },
}));
const __VLS_88 = __VLS_87({
    modelValue: (__VLS_ctx.form.contractAttachment),
    data: (__VLS_ctx.attachmentTree),
    props: ({ label: 'label', value: 'value', children: 'children' }),
    checkStrictly: true,
    defaultExpandAll: true,
    placeholder: "请选择合同附件",
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_87));
var __VLS_85;
const __VLS_90 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
    label: "备注",
}));
const __VLS_92 = __VLS_91({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_91));
__VLS_93.slots.default;
const __VLS_94 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "请输入备注",
}));
const __VLS_96 = __VLS_95({
    modelValue: (__VLS_ctx.form.remark),
    placeholder: "请输入备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_95));
var __VLS_93;
const __VLS_98 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
    label: "创建人",
}));
const __VLS_100 = __VLS_99({
    label: "创建人",
}, ...__VLS_functionalComponentArgsRest(__VLS_99));
__VLS_101.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_101;
const __VLS_102 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
    label: "最后修改人",
}));
const __VLS_104 = __VLS_103({
    label: "最后修改人",
}, ...__VLS_functionalComponentArgsRest(__VLS_103));
__VLS_105.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_105;
const __VLS_106 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
    label: "最后修改时间",
}));
const __VLS_108 = __VLS_107({
    label: "最后修改时间",
}, ...__VLS_functionalComponentArgsRest(__VLS_107));
__VLS_109.slots.default;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "readonly-field" },
});
var __VLS_109;
var __VLS_13;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "itemSectionRef",
    ...{ class: "form-section-block" },
});
/** @type {typeof __VLS_ctx.itemSectionRef} */ ;
__VLS_asFunctionalElement(__VLS_intrinsicElements.h3, __VLS_intrinsicElements.h3)({
    ...{ class: "form-section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "table-toolbar" },
});
const __VLS_110 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
    ...{ 'onClick': {} },
}));
const __VLS_112 = __VLS_111({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_111));
let __VLS_114;
let __VLS_115;
let __VLS_116;
const __VLS_117 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量修改时价');
    }
};
__VLS_113.slots.default;
var __VLS_113;
const __VLS_118 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
    ...{ 'onClick': {} },
}));
const __VLS_120 = __VLS_119({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_119));
let __VLS_122;
let __VLS_123;
let __VLS_124;
const __VLS_125 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量修改定价');
    }
};
__VLS_121.slots.default;
var __VLS_121;
const __VLS_126 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
    ...{ 'onClick': {} },
}));
const __VLS_128 = __VLS_127({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_127));
let __VLS_130;
let __VLS_131;
let __VLS_132;
const __VLS_133 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量修改税率');
    }
};
__VLS_129.slots.default;
var __VLS_129;
const __VLS_134 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({
    ...{ 'onClick': {} },
}));
const __VLS_136 = __VLS_135({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_135));
let __VLS_138;
let __VLS_139;
let __VLS_140;
const __VLS_141 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量修改不含税定价');
    }
};
__VLS_137.slots.default;
var __VLS_137;
const __VLS_142 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_143 = __VLS_asFunctionalComponent(__VLS_142, new __VLS_142({
    ...{ 'onClick': {} },
}));
const __VLS_144 = __VLS_143({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_143));
let __VLS_146;
let __VLS_147;
let __VLS_148;
const __VLS_149 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('批量修改单价限制');
    }
};
__VLS_145.slots.default;
var __VLS_145;
const __VLS_150 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({
    ...{ 'onClick': {} },
}));
const __VLS_152 = __VLS_151({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_151));
let __VLS_154;
let __VLS_155;
let __VLS_156;
const __VLS_157 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleToolbarAction('导入');
    }
};
__VLS_153.slots.default;
var __VLS_153;
const __VLS_158 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_159 = __VLS_asFunctionalComponent(__VLS_158, new __VLS_158({
    data: (__VLS_ctx.itemRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table purchase-pricing-table" },
    fit: (false),
}));
const __VLS_160 = __VLS_159({
    data: (__VLS_ctx.itemRows),
    border: true,
    stripe: true,
    ...{ class: "erp-table purchase-pricing-table" },
    fit: (false),
}, ...__VLS_functionalComponentArgsRest(__VLS_159));
__VLS_161.slots.default;
const __VLS_162 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}));
const __VLS_164 = __VLS_163({
    type: "index",
    label: "序号",
    width: "56",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_163));
const __VLS_166 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({
    label: "操作",
    width: "86",
    fixed: "left",
}));
const __VLS_168 = __VLS_167({
    label: "操作",
    width: "86",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_167));
__VLS_169.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_169.slots;
    const [{ $index }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_170 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_171 = __VLS_asFunctionalComponent(__VLS_170, new __VLS_170({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_172 = __VLS_171({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_171));
    let __VLS_174;
    let __VLS_175;
    let __VLS_176;
    const __VLS_177 = {
        onClick: (...[$event]) => {
            __VLS_ctx.addItemRow($index);
        }
    };
    __VLS_173.slots.default;
    var __VLS_173;
    const __VLS_178 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({
        ...{ 'onClick': {} },
        text: true,
    }));
    const __VLS_180 = __VLS_179({
        ...{ 'onClick': {} },
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_179));
    let __VLS_182;
    let __VLS_183;
    let __VLS_184;
    const __VLS_185 = {
        onClick: (...[$event]) => {
            __VLS_ctx.removeItemRow($index);
        }
    };
    __VLS_181.slots.default;
    var __VLS_181;
}
var __VLS_169;
const __VLS_186 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
    label: "物品编码",
    minWidth: "120",
}));
const __VLS_188 = __VLS_187({
    label: "物品编码",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_187));
__VLS_189.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_189.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_190 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_191 = __VLS_asFunctionalComponent(__VLS_190, new __VLS_190({
        ...{ 'onChange': {} },
        ...{ 'onBlur': {} },
        modelValue: (row.itemKeyword),
        placeholder: "输入物品编码/名称",
    }));
    const __VLS_192 = __VLS_191({
        ...{ 'onChange': {} },
        ...{ 'onBlur': {} },
        modelValue: (row.itemKeyword),
        placeholder: "输入物品编码/名称",
    }, ...__VLS_functionalComponentArgsRest(__VLS_191));
    let __VLS_194;
    let __VLS_195;
    let __VLS_196;
    const __VLS_197 = {
        onChange: (...[$event]) => {
            __VLS_ctx.fillDerivedPricingFields(row);
        }
    };
    const __VLS_198 = {
        onBlur: (...[$event]) => {
            __VLS_ctx.fillDerivedPricingFields(row);
        }
    };
    var __VLS_193;
}
var __VLS_189;
const __VLS_199 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_200 = __VLS_asFunctionalComponent(__VLS_199, new __VLS_199({
    label: "物品名称",
    minWidth: "140",
}));
const __VLS_201 = __VLS_200({
    label: "物品名称",
    minWidth: "140",
}, ...__VLS_functionalComponentArgsRest(__VLS_200));
__VLS_202.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_202.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.itemName || '-');
}
var __VLS_202;
const __VLS_203 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_204 = __VLS_asFunctionalComponent(__VLS_203, new __VLS_203({
    label: "规格型号",
    minWidth: "120",
}));
const __VLS_205 = __VLS_204({
    label: "规格型号",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_204));
__VLS_206.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_206.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.spec || '-');
}
var __VLS_206;
const __VLS_207 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_208 = __VLS_asFunctionalComponent(__VLS_207, new __VLS_207({
    label: "物品类别",
    minWidth: "120",
}));
const __VLS_209 = __VLS_208({
    label: "物品类别",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_208));
__VLS_210.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_210.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.category || '-');
}
var __VLS_210;
const __VLS_211 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_212 = __VLS_asFunctionalComponent(__VLS_211, new __VLS_211({
    label: "采购单位",
    minWidth: "110",
}));
const __VLS_213 = __VLS_212({
    label: "采购单位",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_212));
__VLS_214.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_214.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.purchaseUnit || '-');
}
var __VLS_214;
const __VLS_215 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_216 = __VLS_asFunctionalComponent(__VLS_215, new __VLS_215({
    label: "时价",
    minWidth: "100",
}));
const __VLS_217 = __VLS_216({
    label: "时价",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_216));
__VLS_218.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_218.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.spotPrice || '-');
}
var __VLS_218;
const __VLS_219 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_220 = __VLS_asFunctionalComponent(__VLS_219, new __VLS_219({
    label: "上期价格",
    minWidth: "100",
}));
const __VLS_221 = __VLS_220({
    label: "上期价格",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_220));
__VLS_222.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_222.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.previousPrice || '-');
}
var __VLS_222;
const __VLS_223 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_224 = __VLS_asFunctionalComponent(__VLS_223, new __VLS_223({
    label: "数量范围",
    minWidth: "110",
}));
const __VLS_225 = __VLS_224({
    label: "数量范围",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_224));
__VLS_226.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_226.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_227 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_228 = __VLS_asFunctionalComponent(__VLS_227, new __VLS_227({
        modelValue: (row.quantityRange),
        placeholder: "请输入数量范围",
    }));
    const __VLS_229 = __VLS_228({
        modelValue: (row.quantityRange),
        placeholder: "请输入数量范围",
    }, ...__VLS_functionalComponentArgsRest(__VLS_228));
}
var __VLS_226;
const __VLS_231 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_232 = __VLS_asFunctionalComponent(__VLS_231, new __VLS_231({
    label: "定价 (含税)",
    minWidth: "110",
}));
const __VLS_233 = __VLS_232({
    label: "定价 (含税)",
    minWidth: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_232));
__VLS_234.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_234.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_235 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_236 = __VLS_asFunctionalComponent(__VLS_235, new __VLS_235({
        modelValue: (row.taxIncludedPrice),
        placeholder: "请输入含税定价",
    }));
    const __VLS_237 = __VLS_236({
        modelValue: (row.taxIncludedPrice),
        placeholder: "请输入含税定价",
    }, ...__VLS_functionalComponentArgsRest(__VLS_236));
}
var __VLS_234;
const __VLS_239 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_240 = __VLS_asFunctionalComponent(__VLS_239, new __VLS_239({
    label: "税率",
    minWidth: "90",
}));
const __VLS_241 = __VLS_240({
    label: "税率",
    minWidth: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_240));
__VLS_242.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_242.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.taxRate || '-');
}
var __VLS_242;
const __VLS_243 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_244 = __VLS_asFunctionalComponent(__VLS_243, new __VLS_243({
    label: "定价 (不含税)",
    minWidth: "120",
}));
const __VLS_245 = __VLS_244({
    label: "定价 (不含税)",
    minWidth: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_244));
__VLS_246.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_246.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_247 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_248 = __VLS_asFunctionalComponent(__VLS_247, new __VLS_247({
        modelValue: (row.taxExcludedPrice),
        placeholder: "请输入不含税定价",
    }));
    const __VLS_249 = __VLS_248({
        modelValue: (row.taxExcludedPrice),
        placeholder: "请输入不含税定价",
    }, ...__VLS_functionalComponentArgsRest(__VLS_248));
}
var __VLS_246;
const __VLS_251 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_252 = __VLS_asFunctionalComponent(__VLS_251, new __VLS_251({
    label: "定价环比",
    minWidth: "100",
}));
const __VLS_253 = __VLS_252({
    label: "定价环比",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_252));
__VLS_254.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_254.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_255 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_256 = __VLS_asFunctionalComponent(__VLS_255, new __VLS_255({
        modelValue: (row.priceDiffRatio),
        placeholder: "请输入定价环比",
    }));
    const __VLS_257 = __VLS_256({
        modelValue: (row.priceDiffRatio),
        placeholder: "请输入定价环比",
    }, ...__VLS_functionalComponentArgsRest(__VLS_256));
}
var __VLS_254;
const __VLS_259 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_260 = __VLS_asFunctionalComponent(__VLS_259, new __VLS_259({
    label: "单价限制",
    minWidth: "100",
}));
const __VLS_261 = __VLS_260({
    label: "单价限制",
    minWidth: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_260));
__VLS_262.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_262.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (row.priceLimit || '-');
}
var __VLS_262;
const __VLS_263 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_264 = __VLS_asFunctionalComponent(__VLS_263, new __VLS_263({
    label: "备注",
    minWidth: "160",
}));
const __VLS_265 = __VLS_264({
    label: "备注",
    minWidth: "160",
}, ...__VLS_functionalComponentArgsRest(__VLS_264));
__VLS_266.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_266.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_267 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_268 = __VLS_asFunctionalComponent(__VLS_267, new __VLS_267({
        modelValue: (row.remark),
        placeholder: "请输入备注",
    }));
    const __VLS_269 = __VLS_268({
        modelValue: (row.remark),
        placeholder: "请输入备注",
    }, ...__VLS_functionalComponentArgsRest(__VLS_268));
}
var __VLS_266;
var __VLS_161;
/** @type {__VLS_StyleScopedClasses['item-create-page']} */ ;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['item-create-form']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-form']} */ ;
/** @type {__VLS_StyleScopedClasses['item-form-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['readonly-field']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['form-section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['purchase-pricing-table']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            FixedActionBreadcrumb: FixedActionBreadcrumb,
            activeNav: activeNav,
            basicSectionRef: basicSectionRef,
            itemSectionRef: itemSectionRef,
            pricingCode: pricingCode,
            sectionNavs: sectionNavs,
            pricingTypeOptions: pricingTypeOptions,
            supplierOptions: supplierOptions,
            attachmentTree: attachmentTree,
            form: form,
            itemRows: itemRows,
            scrollToSection: scrollToSection,
            handleBack: handleBack,
            handleSaveDraft: handleSaveDraft,
            handleSave: handleSave,
            handleToolbarAction: handleToolbarAction,
            fillDerivedPricingFields: fillDerivedPricingFields,
            addItemRow: addItemRow,
            removeItemRow: removeItemRow,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
