<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue';

type BreadcrumbNav = {
  key: string;
  label: string;
};

defineProps<{
  navs: BreadcrumbNav[];
  activeKey: string;
  showActions?: boolean;
}>();

const emit = defineEmits<{
  (e: 'back'): void;
  (e: 'save-draft'): void;
  (e: 'save'): void;
  (e: 'navigate', key: string): void;
}>();
</script>

<template>
  <div class="item-create-fixed-head">
    <div class="form-actions-top">
      <el-button text @click="emit('back')">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <div v-if="showActions !== false" class="form-actions-right">
        <el-button @click="emit('save-draft')">保存草稿</el-button>
        <el-button type="primary" @click="emit('save')">保存</el-button>
      </div>
    </div>
    <div class="item-create-breadcrumb-wrap">
      <el-breadcrumb separator=">">
        <el-breadcrumb-item v-for="nav in navs" :key="nav.key">
          <a
            href="#"
            :class="{ 'is-active': activeKey === nav.key }"
            @click.prevent="emit('navigate', nav.key)"
          >
            {{ nav.label }}
          </a>
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>
  </div>
</template>
