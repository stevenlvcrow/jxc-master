/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from 'vue';
defineOptions({
    name: 'SidebarMenuItem',
});
const props = defineProps();
const hasChildren = computed(() => Boolean(props.item.children?.length));
const resolveIcon = (icon) => (icon ? props.iconMap[icon] : undefined);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
if (__VLS_ctx.hasChildren) {
    const __VLS_0 = {}.ElSubMenu;
    /** @type {[typeof __VLS_components.ElSubMenu, typeof __VLS_components.elSubMenu, typeof __VLS_components.ElSubMenu, typeof __VLS_components.elSubMenu, ]} */ ;
    // @ts-ignore
    const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
        index: (__VLS_ctx.item.key),
        ...{ class: (['menu-node', `menu-level-${__VLS_ctx.level ?? 1}`]) },
    }));
    const __VLS_2 = __VLS_1({
        index: (__VLS_ctx.item.key),
        ...{ class: (['menu-node', `menu-level-${__VLS_ctx.level ?? 1}`]) },
    }, ...__VLS_functionalComponentArgsRest(__VLS_1));
    var __VLS_4 = {};
    __VLS_3.slots.default;
    {
        const { title: __VLS_thisSlot } = __VLS_3.slots;
        if (__VLS_ctx.item.icon && __VLS_ctx.resolveIcon(__VLS_ctx.item.icon)) {
            const __VLS_5 = {}.ElIcon;
            /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
            // @ts-ignore
            const __VLS_6 = __VLS_asFunctionalComponent(__VLS_5, new __VLS_5({}));
            const __VLS_7 = __VLS_6({}, ...__VLS_functionalComponentArgsRest(__VLS_6));
            __VLS_8.slots.default;
            const __VLS_9 = ((__VLS_ctx.resolveIcon(__VLS_ctx.item.icon)));
            // @ts-ignore
            const __VLS_10 = __VLS_asFunctionalComponent(__VLS_9, new __VLS_9({}));
            const __VLS_11 = __VLS_10({}, ...__VLS_functionalComponentArgsRest(__VLS_10));
            var __VLS_8;
        }
        __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
        (__VLS_ctx.item.title);
    }
    for (const [child] of __VLS_getVForSourceType((__VLS_ctx.item.children))) {
        const __VLS_13 = {}.SidebarMenuItem;
        /** @type {[typeof __VLS_components.SidebarMenuItem, ]} */ ;
        // @ts-ignore
        const __VLS_14 = __VLS_asFunctionalComponent(__VLS_13, new __VLS_13({
            key: (child.key),
            item: (child),
            iconMap: (__VLS_ctx.iconMap),
            level: ((__VLS_ctx.level ?? 1) + 1),
        }));
        const __VLS_15 = __VLS_14({
            key: (child.key),
            item: (child),
            iconMap: (__VLS_ctx.iconMap),
            level: ((__VLS_ctx.level ?? 1) + 1),
        }, ...__VLS_functionalComponentArgsRest(__VLS_14));
    }
    var __VLS_3;
}
else {
    const __VLS_17 = {}.ElMenuItem;
    /** @type {[typeof __VLS_components.ElMenuItem, typeof __VLS_components.elMenuItem, typeof __VLS_components.ElMenuItem, typeof __VLS_components.elMenuItem, ]} */ ;
    // @ts-ignore
    const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
        index: (__VLS_ctx.item.path ?? __VLS_ctx.item.key),
        ...{ class: (['menu-node', `menu-level-${__VLS_ctx.level ?? 1}`]) },
    }));
    const __VLS_19 = __VLS_18({
        index: (__VLS_ctx.item.path ?? __VLS_ctx.item.key),
        ...{ class: (['menu-node', `menu-level-${__VLS_ctx.level ?? 1}`]) },
    }, ...__VLS_functionalComponentArgsRest(__VLS_18));
    var __VLS_21 = {};
    __VLS_20.slots.default;
    if (__VLS_ctx.item.icon && __VLS_ctx.resolveIcon(__VLS_ctx.item.icon)) {
        const __VLS_22 = {}.ElIcon;
        /** @type {[typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, typeof __VLS_components.ElIcon, typeof __VLS_components.elIcon, ]} */ ;
        // @ts-ignore
        const __VLS_23 = __VLS_asFunctionalComponent(__VLS_22, new __VLS_22({}));
        const __VLS_24 = __VLS_23({}, ...__VLS_functionalComponentArgsRest(__VLS_23));
        __VLS_25.slots.default;
        const __VLS_26 = ((__VLS_ctx.resolveIcon(__VLS_ctx.item.icon)));
        // @ts-ignore
        const __VLS_27 = __VLS_asFunctionalComponent(__VLS_26, new __VLS_26({}));
        const __VLS_28 = __VLS_27({}, ...__VLS_functionalComponentArgsRest(__VLS_27));
        var __VLS_25;
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    (__VLS_ctx.item.title);
    var __VLS_20;
}
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            hasChildren: hasChildren,
            resolveIcon: resolveIcon,
        };
    },
    __typeProps: {},
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
    __typeProps: {},
});
; /* PartiallyEnd: #4569/main.vue */
