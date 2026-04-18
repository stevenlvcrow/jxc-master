/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
const props = withDefaults(defineProps(), {
    rowKey: 'id',
    loading: false,
    height: 320,
    fit: false,
    border: true,
    stripe: true,
    emptyText: '暂无数据',
});
const emit = defineEmits();
const handleSelectionChange = (rows) => {
    emit('selection-change', rows);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    rowKey: 'id',
    loading: false,
    height: 320,
    fit: false,
    border: true,
    stripe: true,
    emptyText: '暂无数据',
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onSelectionChange': {} },
    data: (props.data),
    rowKey: (props.rowKey),
    loading: (props.loading),
    height: (props.height),
    fit: (props.fit),
    border: (props.border),
    stripe: (props.stripe),
    emptyText: (props.emptyText),
    ...{ class: "erp-table" },
}));
const __VLS_2 = __VLS_1({
    ...{ 'onSelectionChange': {} },
    data: (props.data),
    rowKey: (props.rowKey),
    loading: (props.loading),
    height: (props.height),
    fit: (props.fit),
    border: (props.border),
    stripe: (props.stripe),
    emptyText: (props.emptyText),
    ...{ class: "erp-table" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onSelectionChange: (__VLS_ctx.handleSelectionChange)
};
var __VLS_8 = {};
__VLS_3.slots.default;
var __VLS_9 = {};
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['erp-table']} */ ;
// @ts-ignore
var __VLS_10 = __VLS_9;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            handleSelectionChange: handleSelectionChange,
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
