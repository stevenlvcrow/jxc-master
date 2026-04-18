/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, reactive, ref } from 'vue';
import { ArrowDown, Back, Check, CloseBold, Delete, Plus, RefreshLeft, RefreshRight, Search, Upload, } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout from '@/components/PageTabsLayout.vue';
const router = useRouter();
const tabs = [
    { key: 'item', label: '物品维度' },
    { key: 'rule', label: '规则维度' },
];
const activeTab = ref('item');
const expandFilters = ref(false);
const selectedIds = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const pricingTypeOptions = ['供应商定价', '仓库定价', '活动定价'];
const enabledStatusOptions = ['启用', '停用'];
const effectiveStatusOptions = ['未生效', '生效中', '已失效'];
const orderStatusOptions = ['草稿', '已提交', '已审核'];
const writeResultOptions = ['成功', '失败', '处理中'];
const createModeOptions = ['手工创建', '批量导入', '规则生成'];
const warehouseOptions = ['中央成品仓', '北区原料仓', '南区包材仓'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];
const supplierTree = [
    {
        value: 'supplier-group-a',
        label: '华东供应商组',
        children: [
            { value: 'supplier-1', label: '鲜达食品' },
            { value: 'supplier-2', label: '优选农场' },
        ],
    },
    {
        value: 'supplier-group-b',
        label: '直营供应商组',
        children: [
            { value: 'supplier-3', label: '沪上冷链' },
            { value: 'supplier-4', label: '盒马包材' },
        ],
    },
];
const creatorTree = [
    {
        value: 'dept-pricing',
        label: '定价组',
        children: [
            { value: 'creator-1', label: '张敏' },
            { value: 'creator-2', label: '李娜' },
        ],
    },
    {
        value: 'dept-procurement',
        label: '采购组',
        children: [
            { value: 'creator-3', label: '王磊' },
            { value: 'creator-4', label: '赵晨' },
        ],
    },
];
const query = reactive({
    pricingCode: '',
    pricingName: '',
    pricingType: '',
    supplier: '',
    warehouse: '',
    enabledStatus: '启用',
    effectiveStatus: '',
    orderStatus: '',
    writeResult: '',
    itemName: '',
    creator: '',
    createMode: '',
});
const tableData = [
    {
        id: 1,
        pricingCode: 'PJ-202604-001',
        pricingName: '鲜达食品四月统配定价',
        pricingType: '供应商定价',
        supplier: '鲜达食品',
        effectiveDate: '2026-04-01',
        expireDate: '2026-04-30',
        enabledStatus: '启用',
        warehouse: '中央成品仓',
        orderStatus: '已审核',
        writeResult: '成功',
        updatedBy: '张敏',
        updatedAt: '2026-04-13 10:22:00',
    },
    {
        id: 2,
        pricingCode: 'PJ-202604-002',
        pricingName: '优选农场蔬菜定价',
        pricingType: '供应商定价',
        supplier: '优选农场',
        effectiveDate: '2026-04-08',
        expireDate: '2026-05-08',
        enabledStatus: '启用',
        warehouse: '北区原料仓',
        orderStatus: '已提交',
        writeResult: '处理中',
        updatedBy: '李娜',
        updatedAt: '2026-04-12 16:48:00',
    },
    {
        id: 3,
        pricingCode: 'PJ-202604-003',
        pricingName: '盒马包材季度定价',
        pricingType: '活动定价',
        supplier: '盒马包材',
        effectiveDate: '2026-04-10',
        expireDate: '2026-06-30',
        enabledStatus: '停用',
        warehouse: '南区包材仓',
        orderStatus: '草稿',
        writeResult: '失败',
        updatedBy: '王磊',
        updatedAt: '2026-04-11 09:30:00',
    },
];
const treeLabelMap = (nodes) => {
    const map = new Map();
    const walk = (currentNodes) => {
        currentNodes.forEach((node) => {
            map.set(node.value, node.label);
            if (node.children?.length) {
                walk(node.children);
            }
        });
    };
    walk(nodes);
    return map;
};
const supplierLabelMap = treeLabelMap(supplierTree);
const creatorLabelMap = treeLabelMap(creatorTree);
const filteredRows = computed(() => {
    const pricingCodeKeyword = query.pricingCode.trim().toLowerCase();
    const pricingNameKeyword = query.pricingName.trim().toLowerCase();
    const supplierLabel = query.supplier ? supplierLabelMap.get(query.supplier) ?? query.supplier : '';
    return tableData.filter((row) => {
        const matchedCode = !pricingCodeKeyword || row.pricingCode.toLowerCase().includes(pricingCodeKeyword);
        const matchedName = !pricingNameKeyword || row.pricingName.toLowerCase().includes(pricingNameKeyword);
        const matchedType = !query.pricingType || row.pricingType === query.pricingType;
        const matchedSupplier = !supplierLabel || row.supplier === supplierLabel;
        const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
        const matchedEnabledStatus = !query.enabledStatus || row.enabledStatus === query.enabledStatus;
        const matchedOrderStatus = !query.orderStatus || row.orderStatus === query.orderStatus;
        const matchedWriteResult = !query.writeResult || row.writeResult === query.writeResult;
        const matchedItem = !query.itemName || row.pricingName.includes(query.itemName);
        const matchedCreator = !query.creator || row.updatedBy === (creatorLabelMap.get(query.creator) ?? query.creator);
        const matchedEffectiveStatus = !query.effectiveStatus || query.effectiveStatus === '生效中';
        const matchedCreateMode = !query.createMode || query.createMode === '手工创建';
        return matchedCode
            && matchedName
            && matchedType
            && matchedSupplier
            && matchedWarehouse
            && matchedEnabledStatus
            && matchedOrderStatus
            && matchedWriteResult
            && matchedItem
            && matchedCreator
            && matchedEffectiveStatus
            && matchedCreateMode;
    });
});
const pagedRows = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value;
    return filteredRows.value.slice(start, start + pageSize.value);
});
const handleSearch = () => {
    currentPage.value = 1;
};
const handleReset = () => {
    query.pricingCode = '';
    query.pricingName = '';
    query.pricingType = '';
    query.supplier = '';
    query.warehouse = '';
    query.enabledStatus = '启用';
    query.effectiveStatus = '';
    query.orderStatus = '';
    query.writeResult = '';
    query.itemName = '';
    query.creator = '';
    query.createMode = '';
    currentPage.value = 1;
    expandFilters.value = false;
};
const handleToolbarAction = (action) => {
    if (action === '新增') {
        router.push('/purchase/1/1/create');
        return;
    }
    ElMessage.info(`${action}功能待接入`);
};
const handleSelectionChange = (rows) => {
    selectedIds.value = rows.map((row) => row.id);
};
const handleView = (row) => {
    ElMessage.info(`查看：${row.pricingName}`);
};
const handleEdit = (row) => {
    ElMessage.info(`编辑：${row.pricingName}`);
};
const handlePageChange = (page) => {
    currentPage.value = page;
};
const handlePageSizeChange = (size) => {
    pageSize.value = size;
    currentPage.value = 1;
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "panel item-main-panel" },
});
/** @type {[typeof PageTabsLayout, typeof PageTabsLayout, ]} */ ;
// @ts-ignore
const __VLS_0 = __VLS_asFunctionalComponent(PageTabsLayout, new PageTabsLayout({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
}));
const __VLS_1 = __VLS_0({
    activeTab: (__VLS_ctx.activeTab),
    tabs: (__VLS_ctx.tabs),
}, ...__VLS_functionalComponentArgsRest(__VLS_0));
__VLS_2.slots.default;
if (__VLS_ctx.activeTab === 'item') {
    const __VLS_3 = {}.ElAlert;
    /** @type {[typeof __VLS_components.ElAlert, typeof __VLS_components.elAlert, ]} */ ;
    // @ts-ignore
    const __VLS_4 = __VLS_asFunctionalComponent(__VLS_3, new __VLS_3({
        title: "请至【采购定价明细】中查看相关信息",
        type: "info",
        closable: (false),
        showIcon: true,
    }));
    const __VLS_5 = __VLS_4({
        title: "请至【采购定价明细】中查看相关信息",
        type: "info",
        closable: (false),
        showIcon: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_4));
}
else {
    /** @type {[typeof CommonQuerySection, typeof CommonQuerySection, ]} */ ;
    // @ts-ignore
    const __VLS_7 = __VLS_asFunctionalComponent(CommonQuerySection, new CommonQuerySection({
        model: (__VLS_ctx.query),
    }));
    const __VLS_8 = __VLS_7({
        model: (__VLS_ctx.query),
    }, ...__VLS_functionalComponentArgsRest(__VLS_7));
    __VLS_9.slots.default;
    const __VLS_10 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_11 = __VLS_asFunctionalComponent(__VLS_10, new __VLS_10({
        label: "定价单编号",
    }));
    const __VLS_12 = __VLS_11({
        label: "定价单编号",
    }, ...__VLS_functionalComponentArgsRest(__VLS_11));
    __VLS_13.slots.default;
    const __VLS_14 = {}.ElInput;
    /** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
    // @ts-ignore
    const __VLS_15 = __VLS_asFunctionalComponent(__VLS_14, new __VLS_14({
        modelValue: (__VLS_ctx.query.pricingCode),
        placeholder: "请输入定价单编号",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_16 = __VLS_15({
        modelValue: (__VLS_ctx.query.pricingCode),
        placeholder: "请输入定价单编号",
        clearable: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_15));
    var __VLS_13;
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
        modelValue: (__VLS_ctx.query.pricingName),
        placeholder: "请输入定价单名称",
        clearable: true,
        ...{ style: {} },
    }));
    const __VLS_24 = __VLS_23({
        modelValue: (__VLS_ctx.query.pricingName),
        placeholder: "请输入定价单名称",
        clearable: true,
        ...{ style: {} },
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
    const __VLS_30 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_31 = __VLS_asFunctionalComponent(__VLS_30, new __VLS_30({
        modelValue: (__VLS_ctx.query.pricingType),
        clearable: true,
        placeholder: "请选择定价单类型",
        ...{ style: {} },
    }));
    const __VLS_32 = __VLS_31({
        modelValue: (__VLS_ctx.query.pricingType),
        clearable: true,
        placeholder: "请选择定价单类型",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_31));
    __VLS_33.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.pricingTypeOptions))) {
        const __VLS_34 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_35 = __VLS_asFunctionalComponent(__VLS_34, new __VLS_34({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_36 = __VLS_35({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_35));
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
    const __VLS_42 = {}.ElTreeSelect;
    /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
    // @ts-ignore
    const __VLS_43 = __VLS_asFunctionalComponent(__VLS_42, new __VLS_42({
        modelValue: (__VLS_ctx.query.supplier),
        data: (__VLS_ctx.supplierTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        checkStrictly: true,
        defaultExpandAll: true,
        clearable: true,
        placeholder: "请选择供应商",
        ...{ style: {} },
    }));
    const __VLS_44 = __VLS_43({
        modelValue: (__VLS_ctx.query.supplier),
        data: (__VLS_ctx.supplierTree),
        props: ({ label: 'label', value: 'value', children: 'children' }),
        checkStrictly: true,
        defaultExpandAll: true,
        clearable: true,
        placeholder: "请选择供应商",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_43));
    var __VLS_41;
    const __VLS_46 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_47 = __VLS_asFunctionalComponent(__VLS_46, new __VLS_46({
        label: "仓库",
    }));
    const __VLS_48 = __VLS_47({
        label: "仓库",
    }, ...__VLS_functionalComponentArgsRest(__VLS_47));
    __VLS_49.slots.default;
    const __VLS_50 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_51 = __VLS_asFunctionalComponent(__VLS_50, new __VLS_50({
        modelValue: (__VLS_ctx.query.warehouse),
        clearable: true,
        placeholder: "请选择仓库",
        ...{ style: {} },
    }));
    const __VLS_52 = __VLS_51({
        modelValue: (__VLS_ctx.query.warehouse),
        clearable: true,
        placeholder: "请选择仓库",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_51));
    __VLS_53.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.warehouseOptions))) {
        const __VLS_54 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_55 = __VLS_asFunctionalComponent(__VLS_54, new __VLS_54({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_56 = __VLS_55({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_55));
    }
    var __VLS_53;
    var __VLS_49;
    const __VLS_58 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_59 = __VLS_asFunctionalComponent(__VLS_58, new __VLS_58({
        label: "定价启用状态",
    }));
    const __VLS_60 = __VLS_59({
        label: "定价启用状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_59));
    __VLS_61.slots.default;
    const __VLS_62 = {}.ElSelect;
    /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
    // @ts-ignore
    const __VLS_63 = __VLS_asFunctionalComponent(__VLS_62, new __VLS_62({
        modelValue: (__VLS_ctx.query.enabledStatus),
        clearable: true,
        placeholder: "请选择定价启用状态",
        ...{ style: {} },
    }));
    const __VLS_64 = __VLS_63({
        modelValue: (__VLS_ctx.query.enabledStatus),
        clearable: true,
        placeholder: "请选择定价启用状态",
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_63));
    __VLS_65.slots.default;
    for (const [option] of __VLS_getVForSourceType((__VLS_ctx.enabledStatusOptions))) {
        const __VLS_66 = {}.ElOption;
        /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
        // @ts-ignore
        const __VLS_67 = __VLS_asFunctionalComponent(__VLS_66, new __VLS_66({
            key: (option),
            label: (option),
            value: (option),
        }));
        const __VLS_68 = __VLS_67({
            key: (option),
            label: (option),
            value: (option),
        }, ...__VLS_functionalComponentArgsRest(__VLS_67));
    }
    var __VLS_65;
    var __VLS_61;
    if (__VLS_ctx.expandFilters) {
        const __VLS_70 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_71 = __VLS_asFunctionalComponent(__VLS_70, new __VLS_70({
            label: "定价生效状态",
        }));
        const __VLS_72 = __VLS_71({
            label: "定价生效状态",
        }, ...__VLS_functionalComponentArgsRest(__VLS_71));
        __VLS_73.slots.default;
        const __VLS_74 = {}.ElTreeSelect;
        /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
        // @ts-ignore
        const __VLS_75 = __VLS_asFunctionalComponent(__VLS_74, new __VLS_74({
            modelValue: (__VLS_ctx.query.effectiveStatus),
            data: (__VLS_ctx.effectiveStatusOptions.map((item) => ({ value: item, label: item }))),
            props: ({ label: 'label', value: 'value', children: 'children' }),
            clearable: true,
            checkStrictly: true,
            placeholder: "请选择定价生效状态",
            ...{ style: {} },
        }));
        const __VLS_76 = __VLS_75({
            modelValue: (__VLS_ctx.query.effectiveStatus),
            data: (__VLS_ctx.effectiveStatusOptions.map((item) => ({ value: item, label: item }))),
            props: ({ label: 'label', value: 'value', children: 'children' }),
            clearable: true,
            checkStrictly: true,
            placeholder: "请选择定价生效状态",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_75));
        var __VLS_73;
        const __VLS_78 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_79 = __VLS_asFunctionalComponent(__VLS_78, new __VLS_78({
            label: "定价单状态",
        }));
        const __VLS_80 = __VLS_79({
            label: "定价单状态",
        }, ...__VLS_functionalComponentArgsRest(__VLS_79));
        __VLS_81.slots.default;
        const __VLS_82 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_83 = __VLS_asFunctionalComponent(__VLS_82, new __VLS_82({
            modelValue: (__VLS_ctx.query.orderStatus),
            clearable: true,
            placeholder: "请选择定价单状态",
            ...{ style: {} },
        }));
        const __VLS_84 = __VLS_83({
            modelValue: (__VLS_ctx.query.orderStatus),
            clearable: true,
            placeholder: "请选择定价单状态",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_83));
        __VLS_85.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.orderStatusOptions))) {
            const __VLS_86 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_87 = __VLS_asFunctionalComponent(__VLS_86, new __VLS_86({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_88 = __VLS_87({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_87));
        }
        var __VLS_85;
        var __VLS_81;
        const __VLS_90 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_91 = __VLS_asFunctionalComponent(__VLS_90, new __VLS_90({
            label: "定价写入结果",
        }));
        const __VLS_92 = __VLS_91({
            label: "定价写入结果",
        }, ...__VLS_functionalComponentArgsRest(__VLS_91));
        __VLS_93.slots.default;
        const __VLS_94 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_95 = __VLS_asFunctionalComponent(__VLS_94, new __VLS_94({
            modelValue: (__VLS_ctx.query.writeResult),
            clearable: true,
            placeholder: "请选择定价写入结果",
            ...{ style: {} },
        }));
        const __VLS_96 = __VLS_95({
            modelValue: (__VLS_ctx.query.writeResult),
            clearable: true,
            placeholder: "请选择定价写入结果",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_95));
        __VLS_97.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.writeResultOptions))) {
            const __VLS_98 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_99 = __VLS_asFunctionalComponent(__VLS_98, new __VLS_98({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_100 = __VLS_99({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_99));
        }
        var __VLS_97;
        var __VLS_93;
        const __VLS_102 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_103 = __VLS_asFunctionalComponent(__VLS_102, new __VLS_102({
            label: "物品",
        }));
        const __VLS_104 = __VLS_103({
            label: "物品",
        }, ...__VLS_functionalComponentArgsRest(__VLS_103));
        __VLS_105.slots.default;
        const __VLS_106 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_107 = __VLS_asFunctionalComponent(__VLS_106, new __VLS_106({
            modelValue: (__VLS_ctx.query.itemName),
            clearable: true,
            placeholder: "请选择物品",
            ...{ style: {} },
        }));
        const __VLS_108 = __VLS_107({
            modelValue: (__VLS_ctx.query.itemName),
            clearable: true,
            placeholder: "请选择物品",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_107));
        __VLS_109.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.itemOptions))) {
            const __VLS_110 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_111 = __VLS_asFunctionalComponent(__VLS_110, new __VLS_110({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_112 = __VLS_111({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_111));
        }
        var __VLS_109;
        var __VLS_105;
        const __VLS_114 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_115 = __VLS_asFunctionalComponent(__VLS_114, new __VLS_114({
            label: "创建人",
        }));
        const __VLS_116 = __VLS_115({
            label: "创建人",
        }, ...__VLS_functionalComponentArgsRest(__VLS_115));
        __VLS_117.slots.default;
        const __VLS_118 = {}.ElTreeSelect;
        /** @type {[typeof __VLS_components.ElTreeSelect, typeof __VLS_components.elTreeSelect, ]} */ ;
        // @ts-ignore
        const __VLS_119 = __VLS_asFunctionalComponent(__VLS_118, new __VLS_118({
            modelValue: (__VLS_ctx.query.creator),
            data: (__VLS_ctx.creatorTree),
            props: ({ label: 'label', value: 'value', children: 'children' }),
            clearable: true,
            checkStrictly: true,
            defaultExpandAll: true,
            placeholder: "请选择创建人",
            ...{ style: {} },
        }));
        const __VLS_120 = __VLS_119({
            modelValue: (__VLS_ctx.query.creator),
            data: (__VLS_ctx.creatorTree),
            props: ({ label: 'label', value: 'value', children: 'children' }),
            clearable: true,
            checkStrictly: true,
            defaultExpandAll: true,
            placeholder: "请选择创建人",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_119));
        var __VLS_117;
        const __VLS_122 = {}.ElFormItem;
        /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
        // @ts-ignore
        const __VLS_123 = __VLS_asFunctionalComponent(__VLS_122, new __VLS_122({
            label: "创建方式",
        }));
        const __VLS_124 = __VLS_123({
            label: "创建方式",
        }, ...__VLS_functionalComponentArgsRest(__VLS_123));
        __VLS_125.slots.default;
        const __VLS_126 = {}.ElSelect;
        /** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
        // @ts-ignore
        const __VLS_127 = __VLS_asFunctionalComponent(__VLS_126, new __VLS_126({
            modelValue: (__VLS_ctx.query.createMode),
            clearable: true,
            placeholder: "请选择创建方式",
            ...{ style: {} },
        }));
        const __VLS_128 = __VLS_127({
            modelValue: (__VLS_ctx.query.createMode),
            clearable: true,
            placeholder: "请选择创建方式",
            ...{ style: {} },
        }, ...__VLS_functionalComponentArgsRest(__VLS_127));
        __VLS_129.slots.default;
        for (const [option] of __VLS_getVForSourceType((__VLS_ctx.createModeOptions))) {
            const __VLS_130 = {}.ElOption;
            /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
            // @ts-ignore
            const __VLS_131 = __VLS_asFunctionalComponent(__VLS_130, new __VLS_130({
                key: (option),
                label: (option),
                value: (option),
            }));
            const __VLS_132 = __VLS_131({
                key: (option),
                label: (option),
                value: (option),
            }, ...__VLS_functionalComponentArgsRest(__VLS_131));
        }
        var __VLS_129;
        var __VLS_125;
    }
    const __VLS_134 = {}.ElFormItem;
    /** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
    // @ts-ignore
    const __VLS_135 = __VLS_asFunctionalComponent(__VLS_134, new __VLS_134({}));
    const __VLS_136 = __VLS_135({}, ...__VLS_functionalComponentArgsRest(__VLS_135));
    __VLS_137.slots.default;
    const __VLS_138 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_139 = __VLS_asFunctionalComponent(__VLS_138, new __VLS_138({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_140 = __VLS_139({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_139));
    let __VLS_142;
    let __VLS_143;
    let __VLS_144;
    const __VLS_145 = {
        onClick: (__VLS_ctx.handleSearch)
    };
    __VLS_141.slots.default;
    const __VLS_146 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_147 = __VLS_asFunctionalComponent(__VLS_146, new __VLS_146({}));
    const __VLS_148 = __VLS_147({}, ...__VLS_functionalComponentArgsRest(__VLS_147));
    __VLS_149.slots.default;
    const __VLS_150 = {}.Search;
    /** @type {[typeof __VLS_components.Search, ]} */ ;
    // @ts-ignore
    const __VLS_151 = __VLS_asFunctionalComponent(__VLS_150, new __VLS_150({}));
    const __VLS_152 = __VLS_151({}, ...__VLS_functionalComponentArgsRest(__VLS_151));
    var __VLS_149;
    var __VLS_141;
    const __VLS_154 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_155 = __VLS_asFunctionalComponent(__VLS_154, new __VLS_154({
        ...{ 'onClick': {} },
    }));
    const __VLS_156 = __VLS_155({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_155));
    let __VLS_158;
    let __VLS_159;
    let __VLS_160;
    const __VLS_161 = {
        onClick: (__VLS_ctx.handleReset)
    };
    __VLS_157.slots.default;
    const __VLS_162 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_163 = __VLS_asFunctionalComponent(__VLS_162, new __VLS_162({}));
    const __VLS_164 = __VLS_163({}, ...__VLS_functionalComponentArgsRest(__VLS_163));
    __VLS_165.slots.default;
    const __VLS_166 = {}.RefreshRight;
    /** @type {[typeof __VLS_components.RefreshRight, ]} */ ;
    // @ts-ignore
    const __VLS_167 = __VLS_asFunctionalComponent(__VLS_166, new __VLS_166({}));
    const __VLS_168 = __VLS_167({}, ...__VLS_functionalComponentArgsRest(__VLS_167));
    var __VLS_165;
    var __VLS_157;
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
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.expandFilters = !__VLS_ctx.expandFilters;
        }
    };
    __VLS_173.slots.default;
    const __VLS_178 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_179 = __VLS_asFunctionalComponent(__VLS_178, new __VLS_178({}));
    const __VLS_180 = __VLS_179({}, ...__VLS_functionalComponentArgsRest(__VLS_179));
    __VLS_181.slots.default;
    const __VLS_182 = {}.ArrowDown;
    /** @type {[typeof __VLS_components.ArrowDown, ]} */ ;
    // @ts-ignore
    const __VLS_183 = __VLS_asFunctionalComponent(__VLS_182, new __VLS_182({}));
    const __VLS_184 = __VLS_183({}, ...__VLS_functionalComponentArgsRest(__VLS_183));
    var __VLS_181;
    (__VLS_ctx.expandFilters ? '收起筛选' : '展开筛选');
    var __VLS_173;
    var __VLS_137;
    var __VLS_9;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-toolbar" },
    });
    const __VLS_186 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_187 = __VLS_asFunctionalComponent(__VLS_186, new __VLS_186({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_188 = __VLS_187({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_187));
    let __VLS_190;
    let __VLS_191;
    let __VLS_192;
    const __VLS_193 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('新增');
        }
    };
    __VLS_189.slots.default;
    const __VLS_194 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_195 = __VLS_asFunctionalComponent(__VLS_194, new __VLS_194({}));
    const __VLS_196 = __VLS_195({}, ...__VLS_functionalComponentArgsRest(__VLS_195));
    __VLS_197.slots.default;
    const __VLS_198 = {}.Plus;
    /** @type {[typeof __VLS_components.Plus, ]} */ ;
    // @ts-ignore
    const __VLS_199 = __VLS_asFunctionalComponent(__VLS_198, new __VLS_198({}));
    const __VLS_200 = __VLS_199({}, ...__VLS_functionalComponentArgsRest(__VLS_199));
    var __VLS_197;
    var __VLS_189;
    const __VLS_202 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_203 = __VLS_asFunctionalComponent(__VLS_202, new __VLS_202({
        ...{ 'onClick': {} },
    }));
    const __VLS_204 = __VLS_203({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_203));
    let __VLS_206;
    let __VLS_207;
    let __VLS_208;
    const __VLS_209 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量导入');
        }
    };
    __VLS_205.slots.default;
    const __VLS_210 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_211 = __VLS_asFunctionalComponent(__VLS_210, new __VLS_210({}));
    const __VLS_212 = __VLS_211({}, ...__VLS_functionalComponentArgsRest(__VLS_211));
    __VLS_213.slots.default;
    const __VLS_214 = {}.Upload;
    /** @type {[typeof __VLS_components.Upload, ]} */ ;
    // @ts-ignore
    const __VLS_215 = __VLS_asFunctionalComponent(__VLS_214, new __VLS_214({}));
    const __VLS_216 = __VLS_215({}, ...__VLS_functionalComponentArgsRest(__VLS_215));
    var __VLS_213;
    var __VLS_205;
    const __VLS_218 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_219 = __VLS_asFunctionalComponent(__VLS_218, new __VLS_218({
        ...{ 'onClick': {} },
    }));
    const __VLS_220 = __VLS_219({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_219));
    let __VLS_222;
    let __VLS_223;
    let __VLS_224;
    const __VLS_225 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量提交');
        }
    };
    __VLS_221.slots.default;
    const __VLS_226 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_227 = __VLS_asFunctionalComponent(__VLS_226, new __VLS_226({}));
    const __VLS_228 = __VLS_227({}, ...__VLS_functionalComponentArgsRest(__VLS_227));
    __VLS_229.slots.default;
    const __VLS_230 = {}.Check;
    /** @type {[typeof __VLS_components.Check, ]} */ ;
    // @ts-ignore
    const __VLS_231 = __VLS_asFunctionalComponent(__VLS_230, new __VLS_230({}));
    const __VLS_232 = __VLS_231({}, ...__VLS_functionalComponentArgsRest(__VLS_231));
    var __VLS_229;
    var __VLS_221;
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
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量删除');
        }
    };
    __VLS_237.slots.default;
    const __VLS_242 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_243 = __VLS_asFunctionalComponent(__VLS_242, new __VLS_242({}));
    const __VLS_244 = __VLS_243({}, ...__VLS_functionalComponentArgsRest(__VLS_243));
    __VLS_245.slots.default;
    const __VLS_246 = {}.Delete;
    /** @type {[typeof __VLS_components.Delete, ]} */ ;
    // @ts-ignore
    const __VLS_247 = __VLS_asFunctionalComponent(__VLS_246, new __VLS_246({}));
    const __VLS_248 = __VLS_247({}, ...__VLS_functionalComponentArgsRest(__VLS_247));
    var __VLS_245;
    var __VLS_237;
    const __VLS_250 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_251 = __VLS_asFunctionalComponent(__VLS_250, new __VLS_250({
        ...{ 'onClick': {} },
    }));
    const __VLS_252 = __VLS_251({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_251));
    let __VLS_254;
    let __VLS_255;
    let __VLS_256;
    const __VLS_257 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量撤回');
        }
    };
    __VLS_253.slots.default;
    const __VLS_258 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_259 = __VLS_asFunctionalComponent(__VLS_258, new __VLS_258({}));
    const __VLS_260 = __VLS_259({}, ...__VLS_functionalComponentArgsRest(__VLS_259));
    __VLS_261.slots.default;
    const __VLS_262 = {}.Back;
    /** @type {[typeof __VLS_components.Back, ]} */ ;
    // @ts-ignore
    const __VLS_263 = __VLS_asFunctionalComponent(__VLS_262, new __VLS_262({}));
    const __VLS_264 = __VLS_263({}, ...__VLS_functionalComponentArgsRest(__VLS_263));
    var __VLS_261;
    var __VLS_253;
    const __VLS_266 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_267 = __VLS_asFunctionalComponent(__VLS_266, new __VLS_266({
        ...{ 'onClick': {} },
    }));
    const __VLS_268 = __VLS_267({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_267));
    let __VLS_270;
    let __VLS_271;
    let __VLS_272;
    const __VLS_273 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量启用');
        }
    };
    __VLS_269.slots.default;
    const __VLS_274 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_275 = __VLS_asFunctionalComponent(__VLS_274, new __VLS_274({}));
    const __VLS_276 = __VLS_275({}, ...__VLS_functionalComponentArgsRest(__VLS_275));
    __VLS_277.slots.default;
    const __VLS_278 = {}.Check;
    /** @type {[typeof __VLS_components.Check, ]} */ ;
    // @ts-ignore
    const __VLS_279 = __VLS_asFunctionalComponent(__VLS_278, new __VLS_278({}));
    const __VLS_280 = __VLS_279({}, ...__VLS_functionalComponentArgsRest(__VLS_279));
    var __VLS_277;
    var __VLS_269;
    const __VLS_282 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_283 = __VLS_asFunctionalComponent(__VLS_282, new __VLS_282({
        ...{ 'onClick': {} },
    }));
    const __VLS_284 = __VLS_283({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_283));
    let __VLS_286;
    let __VLS_287;
    let __VLS_288;
    const __VLS_289 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量停用');
        }
    };
    __VLS_285.slots.default;
    const __VLS_290 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_291 = __VLS_asFunctionalComponent(__VLS_290, new __VLS_290({}));
    const __VLS_292 = __VLS_291({}, ...__VLS_functionalComponentArgsRest(__VLS_291));
    __VLS_293.slots.default;
    const __VLS_294 = {}.CloseBold;
    /** @type {[typeof __VLS_components.CloseBold, ]} */ ;
    // @ts-ignore
    const __VLS_295 = __VLS_asFunctionalComponent(__VLS_294, new __VLS_294({}));
    const __VLS_296 = __VLS_295({}, ...__VLS_functionalComponentArgsRest(__VLS_295));
    var __VLS_293;
    var __VLS_285;
    const __VLS_298 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_299 = __VLS_asFunctionalComponent(__VLS_298, new __VLS_298({
        ...{ 'onClick': {} },
    }));
    const __VLS_300 = __VLS_299({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_299));
    let __VLS_302;
    let __VLS_303;
    let __VLS_304;
    const __VLS_305 = {
        onClick: (...[$event]) => {
            if (!!(__VLS_ctx.activeTab === 'item'))
                return;
            __VLS_ctx.handleToolbarAction('批量反审核');
        }
    };
    __VLS_301.slots.default;
    const __VLS_306 = {}.ElIcon;
    /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
    // @ts-ignore
    const __VLS_307 = __VLS_asFunctionalComponent(__VLS_306, new __VLS_306({}));
    const __VLS_308 = __VLS_307({}, ...__VLS_functionalComponentArgsRest(__VLS_307));
    __VLS_309.slots.default;
    const __VLS_310 = {}.RefreshLeft;
    /** @type {[typeof __VLS_components.RefreshLeft, ]} */ ;
    // @ts-ignore
    const __VLS_311 = __VLS_asFunctionalComponent(__VLS_310, new __VLS_310({}));
    const __VLS_312 = __VLS_311({}, ...__VLS_functionalComponentArgsRest(__VLS_311));
    var __VLS_309;
    var __VLS_301;
    const __VLS_314 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_315 = __VLS_asFunctionalComponent(__VLS_314, new __VLS_314({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.pagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }));
    const __VLS_316 = __VLS_315({
        ...{ 'onSelectionChange': {} },
        data: (__VLS_ctx.pagedRows),
        border: true,
        stripe: true,
        ...{ class: "erp-table" },
        fit: (false),
        height: (360),
        emptyText: ('当前机构暂无数据'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_315));
    let __VLS_318;
    let __VLS_319;
    let __VLS_320;
    const __VLS_321 = {
        onSelectionChange: (__VLS_ctx.handleSelectionChange)
    };
    __VLS_317.slots.default;
    const __VLS_322 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_323 = __VLS_asFunctionalComponent(__VLS_322, new __VLS_322({
        type: "selection",
        width: "44",
        fixed: "left",
    }));
    const __VLS_324 = __VLS_323({
        type: "selection",
        width: "44",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_323));
    const __VLS_326 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_327 = __VLS_asFunctionalComponent(__VLS_326, new __VLS_326({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }));
    const __VLS_328 = __VLS_327({
        type: "index",
        label: "序号",
        width: "56",
        fixed: "left",
    }, ...__VLS_functionalComponentArgsRest(__VLS_327));
    const __VLS_330 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_331 = __VLS_asFunctionalComponent(__VLS_330, new __VLS_330({
        prop: "pricingCode",
        label: "定价单编号",
        minWidth: "150",
        showOverflowTooltip: true,
    }));
    const __VLS_332 = __VLS_331({
        prop: "pricingCode",
        label: "定价单编号",
        minWidth: "150",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_331));
    const __VLS_334 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_335 = __VLS_asFunctionalComponent(__VLS_334, new __VLS_334({
        prop: "pricingName",
        label: "定价单名称",
        minWidth: "200",
        showOverflowTooltip: true,
    }));
    const __VLS_336 = __VLS_335({
        prop: "pricingName",
        label: "定价单名称",
        minWidth: "200",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_335));
    const __VLS_338 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_339 = __VLS_asFunctionalComponent(__VLS_338, new __VLS_338({
        prop: "pricingType",
        label: "定价单类型",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_340 = __VLS_339({
        prop: "pricingType",
        label: "定价单类型",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_339));
    const __VLS_342 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_343 = __VLS_asFunctionalComponent(__VLS_342, new __VLS_342({
        prop: "supplier",
        label: "供应商",
        minWidth: "140",
        showOverflowTooltip: true,
    }));
    const __VLS_344 = __VLS_343({
        prop: "supplier",
        label: "供应商",
        minWidth: "140",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_343));
    const __VLS_346 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_347 = __VLS_asFunctionalComponent(__VLS_346, new __VLS_346({
        prop: "effectiveDate",
        label: "价格生效日期",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_348 = __VLS_347({
        prop: "effectiveDate",
        label: "价格生效日期",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_347));
    const __VLS_350 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_351 = __VLS_asFunctionalComponent(__VLS_350, new __VLS_350({
        prop: "expireDate",
        label: "价格失效日期",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_352 = __VLS_351({
        prop: "expireDate",
        label: "价格失效日期",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_351));
    const __VLS_354 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_355 = __VLS_asFunctionalComponent(__VLS_354, new __VLS_354({
        prop: "enabledStatus",
        label: "定价启用状态",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_356 = __VLS_355({
        prop: "enabledStatus",
        label: "定价启用状态",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_355));
    const __VLS_358 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_359 = __VLS_asFunctionalComponent(__VLS_358, new __VLS_358({
        prop: "warehouse",
        label: "适用仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_360 = __VLS_359({
        prop: "warehouse",
        label: "适用仓库",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_359));
    const __VLS_362 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_363 = __VLS_asFunctionalComponent(__VLS_362, new __VLS_362({
        prop: "orderStatus",
        label: "定价单状态",
        minWidth: "110",
        showOverflowTooltip: true,
    }));
    const __VLS_364 = __VLS_363({
        prop: "orderStatus",
        label: "定价单状态",
        minWidth: "110",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_363));
    const __VLS_366 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_367 = __VLS_asFunctionalComponent(__VLS_366, new __VLS_366({
        prop: "writeResult",
        label: "定价写入结果",
        minWidth: "120",
        showOverflowTooltip: true,
    }));
    const __VLS_368 = __VLS_367({
        prop: "writeResult",
        label: "定价写入结果",
        minWidth: "120",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_367));
    const __VLS_370 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_371 = __VLS_asFunctionalComponent(__VLS_370, new __VLS_370({
        prop: "updatedBy",
        label: "最后修改人",
        minWidth: "100",
        showOverflowTooltip: true,
    }));
    const __VLS_372 = __VLS_371({
        prop: "updatedBy",
        label: "最后修改人",
        minWidth: "100",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_371));
    const __VLS_374 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_375 = __VLS_asFunctionalComponent(__VLS_374, new __VLS_374({
        prop: "updatedAt",
        label: "最后修改时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }));
    const __VLS_376 = __VLS_375({
        prop: "updatedAt",
        label: "最后修改时间",
        minWidth: "170",
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_375));
    const __VLS_378 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_379 = __VLS_asFunctionalComponent(__VLS_378, new __VLS_378({
        label: "操作",
        width: "120",
        fixed: "right",
    }));
    const __VLS_380 = __VLS_379({
        label: "操作",
        width: "120",
        fixed: "right",
    }, ...__VLS_functionalComponentArgsRest(__VLS_379));
    __VLS_381.slots.default;
    {
        const { default: __VLS_thisSlot } = __VLS_381.slots;
        const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
        const __VLS_382 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_383 = __VLS_asFunctionalComponent(__VLS_382, new __VLS_382({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }));
        const __VLS_384 = __VLS_383({
            ...{ 'onClick': {} },
            text: true,
            type: "primary",
        }, ...__VLS_functionalComponentArgsRest(__VLS_383));
        let __VLS_386;
        let __VLS_387;
        let __VLS_388;
        const __VLS_389 = {
            onClick: (...[$event]) => {
                if (!!(__VLS_ctx.activeTab === 'item'))
                    return;
                __VLS_ctx.handleView(row);
            }
        };
        __VLS_385.slots.default;
        var __VLS_385;
        const __VLS_390 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_391 = __VLS_asFunctionalComponent(__VLS_390, new __VLS_390({
            ...{ 'onClick': {} },
            text: true,
        }));
        const __VLS_392 = __VLS_391({
            ...{ 'onClick': {} },
            text: true,
        }, ...__VLS_functionalComponentArgsRest(__VLS_391));
        let __VLS_394;
        let __VLS_395;
        let __VLS_396;
        const __VLS_397 = {
            onClick: (...[$event]) => {
                if (!!(__VLS_ctx.activeTab === 'item'))
                    return;
                __VLS_ctx.handleEdit(row);
            }
        };
        __VLS_393.slots.default;
        var __VLS_393;
    }
    var __VLS_381;
    var __VLS_317;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "table-pagination-meta" },
    });
    (__VLS_ctx.selectedIds.length);
    const __VLS_398 = {}.ElPagination;
    /** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
    // @ts-ignore
    const __VLS_399 = __VLS_asFunctionalComponent(__VLS_398, new __VLS_398({
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
    const __VLS_400 = __VLS_399({
        ...{ 'onCurrentChange': {} },
        ...{ 'onSizeChange': {} },
        currentPage: (__VLS_ctx.currentPage),
        pageSize: (__VLS_ctx.pageSize),
        pageSizes: ([10, 20, 50]),
        total: (__VLS_ctx.filteredRows.length),
        background: true,
        small: true,
        layout: "total, sizes, prev, pager, next, jumper",
    }, ...__VLS_functionalComponentArgsRest(__VLS_399));
    let __VLS_402;
    let __VLS_403;
    let __VLS_404;
    const __VLS_405 = {
        onCurrentChange: (__VLS_ctx.handlePageChange)
    };
    const __VLS_406 = {
        onSizeChange: (__VLS_ctx.handlePageSizeChange)
    };
    var __VLS_401;
}
var __VLS_2;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['item-main-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['table-toolbar']} */ ;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination']} */ ;
/** @type {__VLS_StyleScopedClasses['table-pagination-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            ArrowDown: ArrowDown,
            Back: Back,
            Check: Check,
            CloseBold: CloseBold,
            Delete: Delete,
            Plus: Plus,
            RefreshLeft: RefreshLeft,
            RefreshRight: RefreshRight,
            Search: Search,
            Upload: Upload,
            CommonQuerySection: CommonQuerySection,
            PageTabsLayout: PageTabsLayout,
            tabs: tabs,
            activeTab: activeTab,
            expandFilters: expandFilters,
            selectedIds: selectedIds,
            currentPage: currentPage,
            pageSize: pageSize,
            pricingTypeOptions: pricingTypeOptions,
            enabledStatusOptions: enabledStatusOptions,
            effectiveStatusOptions: effectiveStatusOptions,
            orderStatusOptions: orderStatusOptions,
            writeResultOptions: writeResultOptions,
            createModeOptions: createModeOptions,
            warehouseOptions: warehouseOptions,
            itemOptions: itemOptions,
            supplierTree: supplierTree,
            creatorTree: creatorTree,
            query: query,
            filteredRows: filteredRows,
            pagedRows: pagedRows,
            handleSearch: handleSearch,
            handleReset: handleReset,
            handleToolbarAction: handleToolbarAction,
            handleSelectionChange: handleSelectionChange,
            handleView: handleView,
            handleEdit: handleEdit,
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
