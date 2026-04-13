<script setup lang="ts">
defineProps<{
  buttons: string[];
}>();

const emit = defineEmits<{
  (event: 'action', action: string): void;
}>();
</script>

<template>
  <div class="table-toolbar">
    <el-button
      v-for="(button, index) in buttons"
      :key="button"
      :type="index === 0 ? 'primary' : 'default'"
      @click="emit('action', button)"
    >
      {{ button }}
    </el-button>
  </div>
</template>
