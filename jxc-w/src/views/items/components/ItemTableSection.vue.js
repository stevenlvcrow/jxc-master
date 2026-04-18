/// <reference types="../../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
const __VLS_props = defineProps();
const emit = defineEmits();
const handleSelectionChange = (rows) => {
    emit('selection-change', rows);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.data),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    rowKey: "id",
    height: (__VLS_ctx.height),
    emptyText: (__VLS_ctx.emptyText),
}));
const __VLS_2 = __VLS_1({
    ...{ 'onSelectionChange': {} },
    data: (__VLS_ctx.data),
    fit: (false),
    border: true,
    stripe: true,
    scrollbarAlwaysOn: true,
    ...{ class: "erp-table" },
    rowKey: "id",
    height: (__VLS_ctx.height),
    emptyText: (__VLS_ctx.emptyText),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
__VLS_asFunctionalDirective(__VLS_directives.vLoading)(null, { ...__VLS_directiveBindingRestFields, value: (__VLS_ctx.loading) }, null, null);
var __VLS_8 = {};
__VLS_3.slots.default;
const __VLS_9 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_10 = __VLS_asFunctionalComponent(__VLS_9, new __VLS_9({
    type: "selection",
    width: "44",
    fixed: "left",
}));
const __VLS_11 = __VLS_10({
    type: "selection",
    width: "44",
    fixed: "left",
}, ...__VLS_functionalComponentArgsRest(__VLS_10));
for (const [column] of __VLS_getVForSourceType((__VLS_ctx.columns))) {
    const __VLS_13 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_14 = __VLS_asFunctionalComponent(__VLS_13, new __VLS_13({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        fixed: (column.fixed),
        showOverflowTooltip: true,
    }));
    const __VLS_15 = __VLS_14({
        key: (column.prop),
        prop: (column.prop),
        label: (column.label),
        width: (column.width),
        fixed: (column.fixed),
        showOverflowTooltip: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_14));
}
const __VLS_17 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    label: "操作",
    width: "210",
    fixed: "right",
}));
const __VLS_19 = __VLS_18({
    label: "操作",
    width: "210",
    fixed: "right",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
__VLS_20.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_20.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_21 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }));
    const __VLS_23 = __VLS_22({
        ...{ 'onClick': {} },
        text: true,
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_22));
    let __VLS_25;
    let __VLS_26;
    let __VLS_27;
    const __VLS_28 = {
        onClick: (...[$event]) => {
            __VLS_ctx.emit('edit', row);
        }
    };
    __VLS_24.slots.default;
    var __VLS_24;
    const __VLS_29 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_30 = __VLS_asFunctionalComponent(__VLS_29, new __VLS_29({
        text: true,
    }));
    const __VLS_31 = __VLS_30({
        text: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_30));
    __VLS_32.slots.default;
    var __VLS_32;
    const __VLS_33 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_34 = __VLS_asFunctionalComponent(__VLS_33, new __VLS_33({
        ...{ 'onClick': {} },
        text: true,
        type: "warning",
    }));
    const __VLS_35 = __VLS_34({
        ...{ 'onClick': {} },
        text: true,
        type: "warning",
    }, ...__VLS_functionalComponentArgsRest(__VLS_34));
    let __VLS_37;
    let __VLS_38;
    let __VLS_39;
    const __VLS_40 = {
        onClick: (...[$event]) => {
            __VLS_ctx.emit('toggle-status', row);
        }
    };
    __VLS_36.slots.default;
    (row.status === '启用' ? '停用' : '启用');
    var __VLS_36;
    const __VLS_41 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_42 = __VLS_asFunctionalComponent(__VLS_41, new __VLS_41({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }));
    const __VLS_43 = __VLS_42({
        ...{ 'onClick': {} },
        text: true,
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_42));
    let __VLS_45;
    let __VLS_46;
    let __VLS_47;
    const __VLS_48 = {
        onClick: (...[$event]) => {
            __VLS_ctx.emit('delete', row);
        }
    };
    __VLS_44.slots.default;
    var __VLS_44;
}
var __VLS_20;
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            emit: emit,
            handleSelectionChange: handleSelectionChange,
        };
    },
    __typeEmits: {},
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
