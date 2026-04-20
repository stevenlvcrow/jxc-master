<script setup lang="ts">
import { ref, useAttrs, watch } from 'vue';

defineOptions({
  inheritAttrs: false,
});

const props = withDefaults(defineProps<{
  modelValue?: number | null;
  disabled?: boolean;
  readonly?: boolean;
  precision?: number;
  min?: number;
  max?: number;
  placeholder?: string;
  clearable?: boolean;
}>(), {
  modelValue: null,
  disabled: false,
  readonly: false,
  precision: undefined,
  min: undefined,
  max: undefined,
  placeholder: '',
  clearable: false,
});

const emit = defineEmits<{
  (event: 'update:modelValue', value: number | null): void;
  (event: 'change', value: number | null): void;
  (event: 'blur', value: FocusEvent): void;
  (event: 'focus', value: FocusEvent): void;
}>();

const attrs = useAttrs();
const isFocused = ref(false);

const formatValue = (value: number | null | undefined) => {
  if (value == null || !Number.isFinite(Number(value))) {
    return '';
  }
  if (props.precision != null) {
    return Number(value).toFixed(props.precision);
  }
  return String(value);
};

const sanitizeInput = (raw: string) => {
  let next = raw.replace(/[^\d.-]/g, '');
  const hasLeadingMinus = next.startsWith('-');
  next = `${hasLeadingMinus ? '-' : ''}${next.slice(hasLeadingMinus ? 1 : 0).replace(/-/g, '')}`;
  const dotIndex = next.indexOf('.');
  if (dotIndex >= 0) {
    next = `${next.slice(0, dotIndex + 1)}${next.slice(dotIndex + 1).replace(/\./g, '')}`;
  }
  if (props.precision != null && dotIndex >= 0) {
    next = `${next.slice(0, dotIndex + 1)}${next.slice(dotIndex + 1, dotIndex + 1 + props.precision)}`;
  }
  return next;
};

const parseValue = (raw: string) => {
  if (!raw || raw === '-' || raw === '.' || raw === '-.') {
    return null;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
};

const normalizeValue = (value: number | null) => {
  if (value == null) {
    return null;
  }
  let next = value;
  if (props.min != null) {
    next = Math.max(props.min, next);
  }
  if (props.max != null) {
    next = Math.min(props.max, next);
  }
  if (props.precision != null) {
    const factor = 10 ** props.precision;
    next = Math.round(next * factor) / factor;
  }
  return next;
};

const displayValue = ref(formatValue(props.modelValue));

watch(() => props.modelValue, (value) => {
  if (!isFocused.value) {
    displayValue.value = formatValue(value);
  }
});

const handleInput = (value: string) => {
  const sanitized = sanitizeInput(value);
  displayValue.value = sanitized;
  emit('update:modelValue', parseValue(sanitized));
};

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true;
  emit('focus', event);
};

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false;
  const normalized = normalizeValue(parseValue(displayValue.value));
  displayValue.value = formatValue(normalized);
  emit('update:modelValue', normalized);
  emit('change', normalized);
  emit('blur', event);
};
</script>

<template>
  <el-input
    v-bind="attrs"
    :model-value="displayValue"
    :disabled="props.disabled"
    :readonly="props.readonly"
    :placeholder="props.placeholder"
    :clearable="props.clearable"
    class="common-number-input"
    inputmode="decimal"
    @input="handleInput"
    @focus="handleFocus"
    @blur="handleBlur"
  />
</template>

<style scoped>
.common-number-input {
  width: 100%;
}

.common-number-input :deep(.el-input__wrapper) {
  padding-top: 0 !important;
  padding-bottom: 0 !important;
}

.common-number-input :deep(.el-input__inner) {
  text-align: right;
}
</style>
