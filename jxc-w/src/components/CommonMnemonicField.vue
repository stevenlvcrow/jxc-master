<script setup lang="ts">
import { computed } from 'vue';
import { buildMnemonicCode } from '@/utils/mnemonic';

const props = withDefaults(defineProps<{
  sourceLabel: string;
  mnemonicLabel: string;
  sourceField?: string;
  mnemonicField?: string;
  sourceValue: string;
  modelValue: string;
  sourcePlaceholder?: string;
  mnemonicPlaceholder?: string;
  sourceMaxlength?: number;
  mnemonicMaxlength?: number;
  autoFill?: boolean;
}>(), {
  autoFill: true,
});

const emit = defineEmits<{
  (event: 'update:sourceValue', value: string): void;
  (event: 'update:modelValue', value: string): void;
}>();

const sourceModel = computed({
  get: () => props.sourceValue ?? '',
  set: (value: string) => {
    emit('update:sourceValue', value);
    if (props.autoFill) {
      emit('update:modelValue', buildMnemonicCode(value));
    }
  },
});

const mnemonicModel = computed({
  get: () => props.modelValue ?? '',
  set: (value: string) => {
    emit('update:modelValue', value);
  },
});
</script>

<template>
  <el-form-item :label="sourceLabel" :data-field="sourceField">
    <el-input
      v-model="sourceModel"
      :placeholder="sourcePlaceholder ?? `请输入${sourceLabel}`"
      :maxlength="sourceMaxlength"
    />
  </el-form-item>
  <el-form-item :label="mnemonicLabel" :data-field="mnemonicField">
    <el-input
      v-model="mnemonicModel"
      :placeholder="mnemonicPlaceholder ?? `根据${sourceLabel}自动生成`"
      :maxlength="mnemonicMaxlength"
    />
  </el-form-item>
</template>
