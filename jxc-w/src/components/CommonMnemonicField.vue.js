/// <reference types="../../node_modules/.vue-global-types/vue_3.5_0_0_0.d.ts" />
import { computed } from 'vue';
import { buildMnemonicCode } from '@/utils/mnemonic';
const props = withDefaults(defineProps(), {
    autoFill: true,
});
const emit = defineEmits();
const sourceModel = computed({
    get: () => props.sourceValue ?? '',
    set: (value) => {
        emit('update:sourceValue', value);
        if (props.autoFill) {
            emit('update:modelValue', buildMnemonicCode(value));
        }
    },
});
const mnemonicModel = computed({
    get: () => props.modelValue ?? '',
    set: (value) => {
        emit('update:modelValue', value);
    },
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_withDefaultsArg = (function (t) { return t; })({
    autoFill: true,
});
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    label: (__VLS_ctx.sourceLabel),
    dataField: (__VLS_ctx.sourceField),
}));
const __VLS_2 = __VLS_1({
    label: (__VLS_ctx.sourceLabel),
    dataField: (__VLS_ctx.sourceField),
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
const __VLS_4 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    modelValue: (__VLS_ctx.sourceModel),
    placeholder: (__VLS_ctx.sourcePlaceholder ?? `请输入${__VLS_ctx.sourceLabel}`),
    maxlength: (__VLS_ctx.sourceMaxlength),
}));
const __VLS_6 = __VLS_5({
    modelValue: (__VLS_ctx.sourceModel),
    placeholder: (__VLS_ctx.sourcePlaceholder ?? `请输入${__VLS_ctx.sourceLabel}`),
    maxlength: (__VLS_ctx.sourceMaxlength),
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
var __VLS_3;
const __VLS_8 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    label: (__VLS_ctx.mnemonicLabel),
    dataField: (__VLS_ctx.mnemonicField),
}));
const __VLS_10 = __VLS_9({
    label: (__VLS_ctx.mnemonicLabel),
    dataField: (__VLS_ctx.mnemonicField),
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_11.slots.default;
const __VLS_12 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    modelValue: (__VLS_ctx.mnemonicModel),
    placeholder: (__VLS_ctx.mnemonicPlaceholder ?? `根据${__VLS_ctx.sourceLabel}自动生成`),
    maxlength: (__VLS_ctx.mnemonicMaxlength),
}));
const __VLS_14 = __VLS_13({
    modelValue: (__VLS_ctx.mnemonicModel),
    placeholder: (__VLS_ctx.mnemonicPlaceholder ?? `根据${__VLS_ctx.sourceLabel}自动生成`),
    maxlength: (__VLS_ctx.mnemonicMaxlength),
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
var __VLS_11;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            sourceModel: sourceModel,
            mnemonicModel: mnemonicModel,
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
