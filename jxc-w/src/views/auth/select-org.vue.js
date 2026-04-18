/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { authStorage } from '@/api/auth-storage';
import { fetchOrgTreeApi } from '@/api/modules/org';
import { logoutApi } from '@/api/modules/auth';
import { useAppStore } from '@/stores/app';
import { useMenuStore } from '@/stores/menu';
import { useSessionStore } from '@/stores/session';
const router = useRouter();
const sessionStore = useSessionStore();
const appStore = useAppStore();
const menuStore = useMenuStore();
const useRealOrgApi = import.meta.env.VITE_USE_REAL_ORG_API === '1';
const PROFILE_HOME_PATH = '/profile';
const topTrialNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'trial'));
const groupNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'group'));
const topStoreNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'store'));
const activeGroups = ref(groupNodes.value.map((item) => item.id));
const chooseOrg = async (orgId) => {
    sessionStore.selectOrg(orgId);
    appStore.resetVisitedTabs();
    if (!authStorage.getAccessToken()) {
        menuStore.clearMenus();
        ElMessage.success('机构选择成功');
        await router.replace(PROFILE_HOME_PATH);
        return;
    }
    try {
        await menuStore.loadMenus(orgId);
    }
    catch {
        menuStore.clearMenus();
    }
    ElMessage.success('机构选择成功');
    await router.replace(PROFILE_HOME_PATH);
};
const handleLogout = async () => {
    try {
        await logoutApi();
    }
    catch {
        // Ignore logout API failure and continue local cleanup.
    }
    finally {
        sessionStore.logout();
        menuStore.clearMenus();
        appStore.resetVisitedTabs();
        await router.replace('/login');
    }
};
onMounted(async () => {
    if (!useRealOrgApi) {
        return;
    }
    try {
        const tree = await fetchOrgTreeApi();
        sessionStore.setOrgTree(tree);
        activeGroups.value = tree.filter((item) => item.type === 'group').map((item) => item.id);
    }
    catch {
        // Global error message handled in http interceptor.
    }
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "select-org-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "select-org-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "select-org-head" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "select-org-title" },
});
const __VLS_0 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ 'onClick': {} },
    text: true,
    type: "danger",
}));
const __VLS_2 = __VLS_1({
    ...{ 'onClick': {} },
    text: true,
    type: "danger",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
let __VLS_4;
let __VLS_5;
let __VLS_6;
const __VLS_7 = {
    onClick: (__VLS_ctx.handleLogout)
};
__VLS_3.slots.default;
var __VLS_3;
for (const [trial] of __VLS_getVForSourceType((__VLS_ctx.topTrialNodes))) {
    const __VLS_8 = {}.ElCard;
    /** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
    // @ts-ignore
    const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
        key: (trial.id),
        ...{ class: "org-card" },
        shadow: "never",
    }));
    const __VLS_10 = __VLS_9({
        key: (trial.id),
        ...{ class: "org-card" },
        shadow: "never",
    }, ...__VLS_functionalComponentArgsRest(__VLS_9));
    __VLS_11.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-row-head" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-title-line" },
    });
    const __VLS_12 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
        type: "success",
        effect: "light",
    }));
    const __VLS_14 = __VLS_13({
        type: "success",
        effect: "light",
    }, ...__VLS_functionalComponentArgsRest(__VLS_13));
    __VLS_15.slots.default;
    var __VLS_15;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (trial.name);
    const __VLS_16 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
        ...{ 'onClick': {} },
        type: "primary",
        size: "small",
    }));
    const __VLS_18 = __VLS_17({
        ...{ 'onClick': {} },
        type: "primary",
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_17));
    let __VLS_20;
    let __VLS_21;
    let __VLS_22;
    const __VLS_23 = {
        onClick: (...[$event]) => {
            __VLS_ctx.chooseOrg(trial.id);
        }
    };
    __VLS_19.slots.default;
    var __VLS_19;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "org-meta" },
    });
    (trial.merchantNo);
    (trial.code);
    var __VLS_11;
}
for (const [store] of __VLS_getVForSourceType((__VLS_ctx.topStoreNodes))) {
    const __VLS_24 = {}.ElCard;
    /** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
    // @ts-ignore
    const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
        key: (store.id),
        ...{ class: "org-card" },
        shadow: "never",
    }));
    const __VLS_26 = __VLS_25({
        key: (store.id),
        ...{ class: "org-card" },
        shadow: "never",
    }, ...__VLS_functionalComponentArgsRest(__VLS_25));
    __VLS_27.slots.default;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-row-head" },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-title-line" },
    });
    const __VLS_28 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
        type: "warning",
        effect: "light",
    }));
    const __VLS_30 = __VLS_29({
        type: "warning",
        effect: "light",
    }, ...__VLS_functionalComponentArgsRest(__VLS_29));
    __VLS_31.slots.default;
    var __VLS_31;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
    (store.name);
    const __VLS_32 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
        ...{ 'onClick': {} },
        type: "primary",
        size: "small",
    }));
    const __VLS_34 = __VLS_33({
        ...{ 'onClick': {} },
        type: "primary",
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_33));
    let __VLS_36;
    let __VLS_37;
    let __VLS_38;
    const __VLS_39 = {
        onClick: (...[$event]) => {
            __VLS_ctx.chooseOrg(store.id);
        }
    };
    __VLS_35.slots.default;
    var __VLS_35;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
        ...{ class: "org-meta" },
    });
    (store.merchantNo);
    (store.code);
    var __VLS_27;
}
const __VLS_40 = {}.ElCollapse;
/** @type {[typeof __VLS_components.ElCollapse, typeof __VLS_components.elCollapse, typeof __VLS_components.ElCollapse, typeof __VLS_components.elCollapse, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    modelValue: (__VLS_ctx.activeGroups),
    ...{ class: "org-collapse" },
}));
const __VLS_42 = __VLS_41({
    modelValue: (__VLS_ctx.activeGroups),
    ...{ class: "org-collapse" },
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
__VLS_43.slots.default;
for (const [group] of __VLS_getVForSourceType((__VLS_ctx.groupNodes))) {
    const __VLS_44 = {}.ElCollapseItem;
    /** @type {[typeof __VLS_components.ElCollapseItem, typeof __VLS_components.elCollapseItem, typeof __VLS_components.ElCollapseItem, typeof __VLS_components.elCollapseItem, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
        key: (group.id),
        name: (group.id),
        ...{ class: "org-collapse-item" },
    }));
    const __VLS_46 = __VLS_45({
        key: (group.id),
        name: (group.id),
        ...{ class: "org-collapse-item" },
    }, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    {
        const { title: __VLS_thisSlot } = __VLS_47.slots;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "org-group-title-wrap" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "org-row-head collapse-head" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "org-title-line" },
        });
        const __VLS_48 = {}.ElTag;
        /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
        // @ts-ignore
        const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
            type: "primary",
            effect: "light",
        }));
        const __VLS_50 = __VLS_49({
            type: "primary",
            effect: "light",
        }, ...__VLS_functionalComponentArgsRest(__VLS_49));
        __VLS_51.slots.default;
        var __VLS_51;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (group.name);
        const __VLS_52 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
            ...{ 'onClick': {} },
            type: "primary",
            size: "small",
        }));
        const __VLS_54 = __VLS_53({
            ...{ 'onClick': {} },
            type: "primary",
            size: "small",
        }, ...__VLS_functionalComponentArgsRest(__VLS_53));
        let __VLS_56;
        let __VLS_57;
        let __VLS_58;
        const __VLS_59 = {
            onClick: (...[$event]) => {
                __VLS_ctx.chooseOrg(group.id);
            }
        };
        __VLS_55.slots.default;
        var __VLS_55;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "org-meta" },
        });
        (group.merchantNo);
        (group.code);
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ class: "org-child-wrap" },
    });
    const __VLS_60 = {}.ElDivider;
    /** @type {[typeof __VLS_components.ElDivider, typeof __VLS_components.elDivider, ]} */ ;
    // @ts-ignore
    const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({}));
    const __VLS_62 = __VLS_61({}, ...__VLS_functionalComponentArgsRest(__VLS_61));
    for (const [child] of __VLS_getVForSourceType((group.children ?? []))) {
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            key: (child.id),
            ...{ class: "org-child-item" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ class: "org-child-main" },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.strong, __VLS_intrinsicElements.strong)({});
        (child.name);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
            ...{ class: "org-meta" },
        });
        (child.merchantNo);
        (child.code);
        const __VLS_64 = {}.ElButton;
        /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
        // @ts-ignore
        const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
            ...{ 'onClick': {} },
            type: "primary",
            size: "small",
        }));
        const __VLS_66 = __VLS_65({
            ...{ 'onClick': {} },
            type: "primary",
            size: "small",
        }, ...__VLS_functionalComponentArgsRest(__VLS_65));
        let __VLS_68;
        let __VLS_69;
        let __VLS_70;
        const __VLS_71 = {
            onClick: (...[$event]) => {
                __VLS_ctx.chooseOrg(child.id);
            }
        };
        __VLS_67.slots.default;
        var __VLS_67;
    }
    var __VLS_47;
}
var __VLS_43;
/** @type {__VLS_StyleScopedClasses['select-org-page']} */ ;
/** @type {__VLS_StyleScopedClasses['select-org-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['select-org-head']} */ ;
/** @type {__VLS_StyleScopedClasses['select-org-title']} */ ;
/** @type {__VLS_StyleScopedClasses['org-card']} */ ;
/** @type {__VLS_StyleScopedClasses['org-row-head']} */ ;
/** @type {__VLS_StyleScopedClasses['org-title-line']} */ ;
/** @type {__VLS_StyleScopedClasses['org-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['org-card']} */ ;
/** @type {__VLS_StyleScopedClasses['org-row-head']} */ ;
/** @type {__VLS_StyleScopedClasses['org-title-line']} */ ;
/** @type {__VLS_StyleScopedClasses['org-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['org-collapse']} */ ;
/** @type {__VLS_StyleScopedClasses['org-collapse-item']} */ ;
/** @type {__VLS_StyleScopedClasses['org-group-title-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['org-row-head']} */ ;
/** @type {__VLS_StyleScopedClasses['collapse-head']} */ ;
/** @type {__VLS_StyleScopedClasses['org-title-line']} */ ;
/** @type {__VLS_StyleScopedClasses['org-meta']} */ ;
/** @type {__VLS_StyleScopedClasses['org-child-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['org-child-item']} */ ;
/** @type {__VLS_StyleScopedClasses['org-child-main']} */ ;
/** @type {__VLS_StyleScopedClasses['org-meta']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            topTrialNodes: topTrialNodes,
            groupNodes: groupNodes,
            topStoreNodes: topStoreNodes,
            activeGroups: activeGroups,
            chooseOrg: chooseOrg,
            handleLogout: handleLogout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
