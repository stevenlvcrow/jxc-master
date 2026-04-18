const props = defineProps();
const emit = defineEmits();
const handleNodeClick = (data) => {
    emit('select', data.label);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.aside, __VLS_intrinsicElements.aside)({
    ...{ class: "panel category-panel" },
});
const __VLS_0 = {}.ElTree;
/** @type {[typeof __VLS_components.ElTree, typeof __VLS_components.elTree, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onNodeClick': {} },
    currentNodeKey: (props.selectedLabel),
    data: (__VLS_ctx.treeData),
    expandOnClickNode: (false),
    defaultExpandAll: true,
    nodeKey: "label",
    ...{ class: "category-tree" },
    highlightCurrent: true,
}));
const __VLS_2 = __VLS_1({
    ...{ 'onNodeClick': {} },
    currentNodeKey: (props.selectedLabel),
    data: (__VLS_ctx.treeData),
    expandOnClickNode: (false),
    defaultExpandAll: true,
    nodeKey: "label",
    ...{ class: "category-tree" },
    highlightCurrent: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onNodeClick: (__VLS_ctx.handleNodeClick)
};
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['panel']} */ ;
/** @type {__VLS_StyleScopedClasses['category-panel']} */ ;
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
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeEmits: {},
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
