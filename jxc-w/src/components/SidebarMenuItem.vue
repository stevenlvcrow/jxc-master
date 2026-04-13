<script setup lang="ts">
import { computed, type Component } from 'vue';
import type { AppMenuItem } from '@/config/menu';

defineOptions({
  name: 'SidebarMenuItem',
});

const props = defineProps<{
  item: AppMenuItem;
  iconMap: Record<string, Component>;
}>();

const hasChildren = computed(() => Boolean(props.item.children?.length));

const resolveIcon = (icon?: string) => (icon ? props.iconMap[icon] : undefined);
</script>

<template>
  <el-sub-menu v-if="hasChildren" :index="item.key">
    <template #title>
      <el-icon v-if="item.icon && resolveIcon(item.icon)">
        <component :is="resolveIcon(item.icon)" />
      </el-icon>
      <span>{{ item.title }}</span>
    </template>

    <SidebarMenuItem
      v-for="child in item.children"
      :key="child.key"
      :item="child"
      :icon-map="iconMap"
    />
  </el-sub-menu>

  <el-menu-item v-else :index="item.path ?? item.key">
    <el-icon v-if="item.icon && resolveIcon(item.icon)">
      <component :is="resolveIcon(item.icon)" />
    </el-icon>
    <span>{{ item.title }}</span>
  </el-menu-item>
</template>
