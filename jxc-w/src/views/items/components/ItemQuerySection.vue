<script setup lang="ts">
import type { ItemQuery } from '../types';

defineProps<{
  modelValue: ItemQuery;
  statusOptions: string[];
  itemTypeOptions: string[];
  statTypeOptions: string[];
  storageModeOptions: string[];
}>();

const emit = defineEmits<{
  (event: 'search'): void;
  (event: 'reset'): void;
}>();
</script>

<template>
  <el-form :model="modelValue" inline class="filter-bar compact-filter-bar">
    <el-form-item label="物品信息">
      <el-input v-model="modelValue.keyword" placeholder="编码/名称/规格/第三方编码" clearable />
    </el-form-item>
    <el-form-item label="状态">
      <el-select v-model="modelValue.status" style="width: 96px">
        <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="物品类型">
      <el-select v-model="modelValue.itemType" style="width: 108px">
        <el-option v-for="option in itemTypeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="统计类型">
      <el-select v-model="modelValue.statType" style="width: 190px">
        <el-option v-for="option in statTypeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="储存方式">
      <el-select v-model="modelValue.storageMode" clearable style="width: 92px">
        <el-option v-for="option in storageModeOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="物品标签">
      <el-input v-model="modelValue.tag" placeholder="请输入物品标签" clearable style="width: 120px" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>
