/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchCurrentUserRolesApi, logoutApi } from '@/api/modules/auth';
import { useSessionStore } from '@/stores/session';
const router = useRouter();
const sessionStore = useSessionStore();
const currentOrgLabel = computed(() => {
    const org = sessionStore.currentOrg;
    if (!org) {
        return sessionStore.requiresOrgSelection ? '尚未选择机构' : '平台模式';
    }
    const orgTypeLabel = org.type === 'group' ? '集团' : org.type === 'store' ? '门店' : '试店';
    return `${orgTypeLabel} / ${org.name}`;
});
const currentOrgCode = computed(() => sessionStore.currentOrg?.code ?? '-');
const currentOrgMerchant = computed(() => sessionStore.currentOrg?.merchantNo ?? '-');
const currentOrgCity = computed(() => sessionStore.currentOrg?.city ?? '-');
const roleList = ref([]);
const roleText = computed(() => {
    if (!roleList.value.length) {
        return '暂无角色';
    }
    return roleList.value
        .map((item) => `${item.roleName} / ${item.scopeName || '未知机构'}`)
        .join('、');
});
const loadRoles = async () => {
    try {
        roleList.value = await fetchCurrentUserRolesApi(sessionStore.currentOrgId || undefined);
    }
    catch {
        roleList.value = [];
    }
};
const handleSwitchOrg = () => {
    router.push('/select-org');
};
const handleAction = (action) => {
    ElMessage.info(`${action} 功能待接入`);
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
        router.replace('/login');
        ElMessage.success('已退出登录');
    }
};
onMounted(() => {
    void loadRoles();
});
watch(() => sessionStore.currentOrgId, () => {
    void loadRoles();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['profile-meta-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-title']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__action']} */ ;
// CSS variable injection 
// CSS variable injection end 
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "profile-hero" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-hero__copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-name-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "profile-title" },
});
(__VLS_ctx.sessionStore.userName);
const __VLS_0 = {}.ElTag;
/** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    type: "success",
    effect: "light",
    size: "small",
}));
const __VLS_2 = __VLS_1({
    type: "success",
    effect: "light",
    size: "small",
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
(__VLS_ctx.sessionStore.isLoggedIn ? '已登录' : '未登录');
var __VLS_3;
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "profile-account" },
});
(__VLS_ctx.sessionStore.loginAccount || '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "profile-role-line" },
});
(__VLS_ctx.roleText);
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "profile-meta-grid" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "profile-meta-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-value" },
});
(__VLS_ctx.currentOrgLabel);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-sub" },
});
(__VLS_ctx.currentOrgCode);
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "profile-meta-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-value" },
});
(__VLS_ctx.currentOrgMerchant);
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-sub" },
});
(__VLS_ctx.currentOrgCity);
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "profile-meta-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-value" },
});
(__VLS_ctx.sessionStore.platformAdminMode ? '平台管理员' : '集团 / 门店账号');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-meta-sub" },
});
(__VLS_ctx.sessionStore.requiresOrgSelection ? '需要先选择机构' : '无需机构切换');
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "section-block" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "section-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span)({
    ...{ class: "section-title__bar" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-list" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "security-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__main" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__icon security-card__icon--ok" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__desc" },
});
const __VLS_4 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}));
const __VLS_6 = __VLS_5({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
let __VLS_8;
let __VLS_9;
let __VLS_10;
const __VLS_11 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleAction('修改密码');
    }
};
__VLS_7.slots.default;
var __VLS_7;
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "security-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__main" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__icon security-card__icon--ok" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__desc" },
});
(__VLS_ctx.sessionStore.loginAccount ? `${__VLS_ctx.sessionStore.loginAccount.slice(0, 3)}****${__VLS_ctx.sessionStore.loginAccount.slice(-4)}` : '-');
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__desc security-card__desc--muted" },
});
const __VLS_12 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}));
const __VLS_14 = __VLS_13({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
let __VLS_16;
let __VLS_17;
let __VLS_18;
const __VLS_19 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleAction('更换手机号');
    }
};
__VLS_15.slots.default;
var __VLS_15;
__VLS_asFunctionalElement(__VLS_intrinsicElements.article, __VLS_intrinsicElements.article)({
    ...{ class: "security-card" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__main" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__icon security-card__icon--warn" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__copy" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__desc" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "security-card__desc security-card__desc--muted" },
});
(__VLS_ctx.sessionStore.loginAccount || '-');
const __VLS_20 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}));
const __VLS_22 = __VLS_21({
    ...{ 'onClick': {} },
    plain: true,
    ...{ class: "security-card__action" },
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
let __VLS_24;
let __VLS_25;
let __VLS_26;
const __VLS_27 = {
    onClick: (...[$event]) => {
        __VLS_ctx.handleAction('修改账号');
    }
};
__VLS_23.slots.default;
var __VLS_23;
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "profile-notice" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "profile-notice__title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.ul, __VLS_intrinsicElements.ul)({
    ...{ class: "profile-notice__list" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
__VLS_asFunctionalElement(__VLS_intrinsicElements.li, __VLS_intrinsicElements.li)({});
/** @type {__VLS_StyleScopedClasses['profile-page']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-hero']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-hero__copy']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-name-row']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-title']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-account']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-role-line']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-grid']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-card']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-label']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-value']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-card']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-label']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-value']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-card']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-label']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-value']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-meta-sub']} */ ;
/** @type {__VLS_StyleScopedClasses['section-block']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title']} */ ;
/** @type {__VLS_StyleScopedClasses['section-title__bar']} */ ;
/** @type {__VLS_StyleScopedClasses['security-list']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__main']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon--ok']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__copy']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__title']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__action']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__main']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon--ok']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__copy']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__title']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc--muted']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__action']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__main']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__icon--warn']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__copy']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__title']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__desc--muted']} */ ;
/** @type {__VLS_StyleScopedClasses['security-card__action']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-notice']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-notice__title']} */ ;
/** @type {__VLS_StyleScopedClasses['profile-notice__list']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            sessionStore: sessionStore,
            currentOrgLabel: currentOrgLabel,
            currentOrgCode: currentOrgCode,
            currentOrgMerchant: currentOrgMerchant,
            currentOrgCity: currentOrgCity,
            roleText: roleText,
            handleAction: handleAction,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
