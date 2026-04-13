<script setup lang="ts">
export type ToolbarButton = {
  key: string;
  label: string;
  type?: 'primary' | 'default' | 'success' | 'warning' | 'danger' | 'info';
};

defineProps<{
  buttons: ReadonlyArray<ToolbarButton>;
}>();

const emit = defineEmits<{
  (event: 'action', key: string): void;
}>();
</script>

<template>
  <div class="table-toolbar">
    <el-button
      v-for="button in buttons"
      :key="button.key"
      :type="button.type ?? 'default'"
      @click="emit('action', button.key)"
    >
      {{ button.label }}
    </el-button>
  </div>
</template>
