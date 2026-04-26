<script setup lang="ts">
import { computed } from 'vue';
import type { CategoryNode, SupplierQuery } from '../types';

const props = defineProps<{
  modelValue: SupplierQuery;
  supplierInfoTree: CategoryNode[];
  statusOptions: string[];
  bindStatusOptions: string[];
  sourceOptions: string[];
  supplyRelationOptions: string[];
}>();

const emit = defineEmits<{
  (event: 'update:modelValue', value: SupplierQuery): void;
  (event: 'search'): void;
  (event: 'reset'): void;
}>();

const fieldModel = (field: keyof SupplierQuery) => computed({
  get: () => props.modelValue[field],
  set: (value: string) => {
    emit('update:modelValue', {
      ...props.modelValue,
      [field]: value,
    });
  },
});

const supplierInfo = fieldModel('supplierInfo');
const status = fieldModel('status');
const bindStatus = fieldModel('bindStatus');
const source = fieldModel('source');
const supplyRelation = fieldModel('supplyRelation');
</script>

<template>
  <el-form :model="modelValue" inline class="filter-bar compact-filter-bar">
    <el-form-item label="供应商信息">
      <el-tree-select
        v-model="supplierInfo"
        :data="supplierInfoTree"
        node-key="id"
        :props="{ label: 'label', children: 'children', value: 'id' }"
        placeholder="请选择供应商"
        clearable
        check-strictly
        style="width: 180px"
      />
    </el-form-item>
    <el-form-item label="启用状态">
      <el-select v-model="status" style="width: 96px">
        <el-option v-for="option in statusOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="绑定状态">
      <el-select v-model="bindStatus" style="width: 96px">
        <el-option v-for="option in bindStatusOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="来源">
      <el-select v-model="source" style="width: 96px">
        <el-option v-for="option in sourceOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item label="供货关系">
      <el-select v-model="supplyRelation" style="width: 96px">
        <el-option v-for="option in supplyRelationOptions" :key="option" :label="option" :value="option" />
      </el-select>
    </el-form-item>
    <el-form-item>
      <el-button type="primary" @click="emit('search')">查询</el-button>
      <el-button @click="emit('reset')">重置</el-button>
    </el-form-item>
  </el-form>
</template>
