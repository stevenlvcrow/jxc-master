/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
const props = withDefaults(defineProps(), {
    nodeKey: 'id',
    defaultExpandAll: true,
    expandOnClickNode: false,
    highlightCurrent: false,
    currentNodeKey: '',
});
const emit = defineEmits();
const handleNodeClick = (node) => {
    emit('node-click', node);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    nodeKey: 'id',
    defaultExpandAll: true,
    expandOnClickNode: false,
    highlightCurrent: false,
    currentNodeKey: '',
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElTree;
/** @type {[typeof __VLS_components.ElTree, typeof __VLS_components.elTree, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onNodeClick': {} },
    data: (props.data),
    nodeKey: (props.nodeKey),
    defaultExpandAll: (props.defaultExpandAll),
    expandOnClickNode: (props.expandOnClickNode),
    highlightCurrent: (props.highlightCurrent),
    currentNodeKey: (props.currentNodeKey),
    ...{ class: "category-tree" },
}));
const __VLS_2 = __VLS_1({
    ...{ 'onNodeClick': {} },
    data: (props.data),
    nodeKey: (props.nodeKey),
    defaultExpandAll: (props.defaultExpandAll),
    expandOnClickNode: (props.expandOnClickNode),
    highlightCurrent: (props.highlightCurrent),
    currentNodeKey: (props.currentNodeKey),
    ...{ class: "category-tree" },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onNodeClick: (__VLS_ctx.handleNodeClick)
};
var __VLS_8 = {};
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['category-tree']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            handleNodeClick: handleNodeClick,
        };
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
    props: {},
});
; /* PartiallyEnd: #4569/main.vue */
