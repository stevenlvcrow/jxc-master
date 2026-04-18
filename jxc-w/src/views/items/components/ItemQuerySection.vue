<script setup lang="ts">
import type { ItemQuery } from '../types';

const props = defineProps<{
  modelValue: ItemQuery;
  statusOptions: string[];
  itemTypeOptions: string[];
  statTypeOptions: string[];
  storageModeOptions: string[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: ItemQuery): void;
  (event: 'search'): void;
  (event: 'reset'): void;
}>();

const updateQueryField = <K extends keyof ItemQuery>(key: K, value: ItemQuery[K]) => {
  emit('update:modelValue', {
    ...props.modelValue,
    [key]: value,
  });
};

const normalizeTextValue = (value: unknown) => (typeof value === 'string' ? value : '');

const handleKeywordChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('keyword', normalizeTextValue(value));
};

const handleStatusChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('status', normalizeTextValue(value));
};

const handleItemTypeChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('itemType', normalizeTextValue(value));
};

const handleStatTypeChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('statType', normalizeTextValue(value));
};

const handleStorageModeChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('storageMode', normalizeTextValue(value));
};

const handleTagChange = (value: string | number | boolean | null | undefined) => {
  updateQueryField('tag', normalizeTextValue(value));
};
</script>

<template>
  <el-form :model="modelValue" inline class="filter-bar compact-filter-bar">
    <el-form-item label="物品信息">
      <el-input
        :model-value="modelValue.keyword"
        placeholder="编码/名称/规格/第三方编码"
        clearable
        @update:model-value="handleKeywordChange"
      />
    </el-form-item>
    <el-form-item label="状态">
      <el-select
        :model-value="modelValue.status"
        style="width: 96px"
        @update:model-value="handleStatusChange"
      >
        <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="物品类型">
      <el-select
        :model-value="modelValue.itemType"
        style="width: 108px"
        @update:model-value="handleItemTypeChange"
      >
        <el-option v-for="option in itemTypeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="统计类型">
      <el-select
        :model-value="modelValue.statType"
        style="width: 190px"
        @update:model-value="handleStatTypeChange"
      >
        <el-option v-for="option in statTypeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="储存方式">
      <el-select
        :model-value="modelValue.storageMode"
        clearable
        style="width: 92px"
        @update:model-value="handleStorageModeChange"
      >
        <el-option v-for="option in storageModeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="物品标签">
      <el-input
        :model-value="modelValue.tag"
        placeholder="请输入物品标签"
        clearable
        style="width: 120px"
        @update:model-value="handleTagChange"
      />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>
