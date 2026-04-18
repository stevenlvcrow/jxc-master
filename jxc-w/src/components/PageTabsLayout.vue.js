/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, useSlots } from 'vue';
const props = withDefaults(defineProps(), {
    bodyClass: '',
});
const emit = defineEmits();
const slots = useSlots();
const hasActions = computed(() => Boolean(slots.actions));
const hasNotice = computed(() => Boolean(slots.notice));
const handleTabClick = (tab) => {
    if (tab.disabled || tab.key === props.activeTab) {
        return;
    }
    emit('update:activeTab', tab.key);
    emit('change', tab.key);
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    bodyClass: '',
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['is-active']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__header']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__actions']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__body']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "page-tabs-layout" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.header, __VLS_intrinsicElements.header)({
    ...{ class: "page-tabs-layout__header" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-tabs-layout__tabs" },
    role: "tablist",
    'aria-label': "页面页签",
});
for (const [tab] of __VLS_getVForSourceType((__VLS_ctx.tabs))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
        ...{ onClick: (...[$event]) => {
                __VLS_ctx.handleTabClick(tab);
            } },
        key: (tab.key),
        type: "button",
        ...{ class: "page-tabs-layout__tab" },
        ...{ class: ({ 'is-active': tab.key === __VLS_ctx.activeTab }) },
        disabled: (tab.disabled),
        role: "tab",
        'aria-selected': (tab.key === __VLS_ctx.activeTab),
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (tab.label);
}
if (__VLS_ctx.hasActions) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-tabs-layout__actions" },
    });
    var __VLS_0 = {};
}
if (__VLS_ctx.hasNotice) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "page-tabs-layout__notice" },
    });
    var __VLS_2 = {};
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "page-tabs-layout__body" },
    ...{ class: (__VLS_ctx.bodyClass) },
});
var __VLS_4 = {};
/** @type {__VLS_StyleScopedClasses['page-tabs-layout']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__header']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tabs']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__tab']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__actions']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__notice']} */ ;
/** @type {__VLS_StyleScopedClasses['page-tabs-layout__body']} */ ;
// @ts-ignore
var __VLS_1 = __VLS_0, __VLS_3 = __VLS_2, __VLS_5 = __VLS_4;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            hasActions: hasActions,
            hasNotice: hasNotice,
            handleTabClick: handleTabClick,
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
