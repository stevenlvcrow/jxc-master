/// <reference types="../../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginApi } from '@/api/modules/auth';
import { useSessionStore } from '@/stores/session';
const router = useRouter();
const sessionStore = useSessionStore();
const account = ref('13800000000');
const password = ref('123654');
const useRealAuthApi = import.meta.env.VITE_USE_REAL_AUTH_API === '1';
const PROFILE_HOME_PATH = '/profile';
const isMockPlatformAccount = (value) => {
    const normalized = value.trim().toLowerCase();
    return normalized === 'admin' || normalized === '13800000000';
};
const submitLogin = async () => {
    if (!account.value.trim() || !password.value.trim()) {
        ElMessage.warning('请输入账号和密码');
        return;
    }
    if (!useRealAuthApi) {
        const loginAccount = account.value.trim();
        sessionStore.login('李智杰', loginAccount, {
            platformAdminMode: isMockPlatformAccount(loginAccount),
        });
        router.replace(sessionStore.requiresOrgSelection ? '/select-org' : PROFILE_HOME_PATH);
        return;
    }
    try {
        const result = await loginApi({
            account: account.value.trim(),
            password: password.value,
        });
        sessionStore.setAuth({
            accessToken: result.accessToken,
            refreshToken: result.refreshToken,
        });
        const loginAccount = account.value.trim();
        sessionStore.login(result.userName || loginAccount, loginAccount, {
            platformAdminMode: Boolean(result.platformAdmin),
        });
        router.replace(sessionStore.requiresOrgSelection ? '/select-org' : PROFILE_HOME_PATH);
    }
    catch {
        // Global error message handled in http interceptor.
    }
};
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-page" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.section, __VLS_intrinsicElements.section)({
    ...{ class: "login-panel" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-title-wrap" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.h1, __VLS_intrinsicElements.h1)({
    ...{ class: "login-title" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.p, __VLS_intrinsicElements.p)({
    ...{ class: "login-subtitle" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "login-form" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    value: (__VLS_ctx.account),
    ...{ class: "field-input" },
    type: "text",
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.label, __VLS_intrinsicElements.label)({
    ...{ class: "field-row" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "field-label" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.input)({
    ...{ class: "field-input" },
    type: "password",
});
(__VLS_ctx.password);
__VLS_asFunctionalElement(__VLS_intrinsicElements.button, __VLS_intrinsicElements.button)({
    ...{ onClick: (__VLS_ctx.submitLogin) },
    ...{ class: "submit-button" },
});
/** @type {__VLS_StyleScopedClasses['login-page']} */ ;
/** @type {__VLS_StyleScopedClasses['login-panel']} */ ;
/** @type {__VLS_StyleScopedClasses['login-title-wrap']} */ ;
/** @type {__VLS_StyleScopedClasses['login-title']} */ ;
/** @type {__VLS_StyleScopedClasses['login-subtitle']} */ ;
/** @type {__VLS_StyleScopedClasses['login-form']} */ ;
/** @type {__VLS_StyleScopedClasses['field-row']} */ ;
/** @type {__VLS_StyleScopedClasses['field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['field-input']} */ ;
/** @type {__VLS_StyleScopedClasses['field-row']} */ ;
/** @type {__VLS_StyleScopedClasses['field-label']} */ ;
/** @type {__VLS_StyleScopedClasses['field-input']} */ ;
/** @type {__VLS_StyleScopedClasses['submit-button']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            account: account,
            password: password,
            submitLogin: submitLogin,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
