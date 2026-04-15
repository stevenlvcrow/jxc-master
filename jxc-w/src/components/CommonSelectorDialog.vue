<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonTreeSection from '@/components/CommonTreeSection.vue';
import CommonTableSection from '@/components/CommonTableSection.vue';

export type SelectorTreeNode = {
  id: string | number;
  label: string;
  children?: SelectorTreeNode[];
};

export type SelectorColumn = {
  prop: string;
  label: string;
  width?: number | string;
  minWidth?: number | string;
};

type StatusOption = {
  label: string;
  value: string;
};

const props = withDefaults(defineProps<{
  modelValue: boolean;
  title?: string;
  width?: string;
  loading?: boolean;
  treeData?: SelectorTreeNode[];
  tableData?: Array<Record<string, unknown>>;
  columns?: SelectorColumn[];
  rowKey?: string;
  selectedRows?: Array<Record<string, unknown>>;
  selectedLabelKey?: string;
  total?: number;
  currentPage?: number;
  pageSize?: number;
  keywordLabel?: string;
  keywordPlaceholder?: string;
  statusLabel?: string;
  statusValue?: string;
  statusOptions?: StatusOption[];
  keywordValue?: string;
}>(), {
  title: '选择数据',
  width: '980px',
  loading: false,
  treeData: () => [],
  tableData: () => [],
  columns: () => [],
  rowKey: 'id',
  selectedRows: () => [],
  selectedLabelKey: 'name',
  total: 0,
  currentPage: 1,
  pageSize: 10,
  keywordLabel: '关键字',
  keywordPlaceholder: '请输入关键字检索',
  statusLabel: '状态',
  statusValue: '',
  statusOptions: () => [
    { label: '全部', value: '' },
    { label: '否', value: 'N' },
    { label: '是', value: 'Y' },
  ],
  keywordValue: '',
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'search', payload: { keyword: string; status: string }): void;
  (e: 'node-change', node: SelectorTreeNode | null): void;
  (e: 'selection-change', rows: Array<Record<string, unknown>>): void;
  (e: 'clear-selection'): void;
  (e: 'page-change', page: number): void;
  (e: 'page-size-change', size: number): void;
  (e: 'confirm', rows: Array<Record<string, unknown>>): void;
  (e: 'cancel'): void;
}>();

const keyword = ref(props.keywordValue);
const status = ref(props.statusValue);
const selected = ref<Array<Record<string, unknown>>>([...props.selectedRows]);

watch(() => props.keywordValue, (val) => {
  keyword.value = val;
});

watch(() => props.statusValue, (val) => {
  status.value = val;
});

watch(() => props.selectedRows, (rows) => {
  selected.value = [...rows];
}, { deep: true });

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

const selectedCount = computed(() => selected.value.length);

const getRowId = (row: Record<string, unknown>) => String(row[props.rowKey] ?? '');

const selectedTags = computed(() => selected.value.map((row) => {
  const label = row[props.selectedLabelKey];
  return {
    id: getRowId(row),
    label: label == null || String(label).trim() === '' ? getRowId(row) : String(label),
  };
}));

const handleSearch = () => {
  emit('search', {
    keyword: keyword.value.trim(),
    status: status.value,
  });
};

const handleNodeClick = (data: SelectorTreeNode) => {
  emit('node-change', data);
};

const handleSelectionChange = (rows: Array<Record<string, unknown>>) => {
  selected.value = rows;
  emit('selection-change', rows);
};

const handleClearSelection = () => {
  selected.value = [];
  emit('clear-selection');
};

const handleRemoveTag = (id: string) => {
  selected.value = selected.value.filter((row) => getRowId(row) !== id);
  emit('selection-change', selected.value);
};

const handleCancel = () => {
  emit('cancel');
  dialogVisible.value = false;
};

const handleConfirm = () => {
  emit('confirm', selected.value);
};
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    top="8vh"
    class="common-selector-dialog standard-form-dialog"
    destroy-on-close
    append-to-body
  >
    <div class="selector-query-row">
      <CommonQuerySection :model="{ keyword, status }">
        <el-form-item :label="`${keywordLabel}:`">
          <el-input
            v-model="keyword"
            :placeholder="keywordPlaceholder"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item :label="`${statusLabel}:`">
          <el-select v-model="status" style="width: 120px">
            <el-option
              v-for="option in statusOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="selector-search-btn" @click="handleSearch">检索</el-button>
        </el-form-item>
      </CommonQuerySection>
    </div>

    <div class="selector-content">
      <aside class="selector-tree-panel">
        <CommonTreeSection
          :data="treeData"
          node-key="id"
          :default-expand-all="true"
          :expand-on-click-node="false"
          @node-click="handleNodeClick"
        />
      </aside>

        <section class="selector-table-panel">
          <CommonTableSection
            :data="tableData"
            :height="320"
            :fit="false"
            :border="true"
            :stripe="true"
            :loading="loading"
          @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="46" fixed="left" />
            <el-table-column type="index" label="序号" width="56" fixed="left" />

            <slot name="table-columns">
              <el-table-column
                v-for="column in columns"
                :key="column.prop"
                :prop="column.prop"
                :label="column.label"
                :width="column.width"
                :min-width="column.minWidth"
                show-overflow-tooltip
              />
            </slot>
          </CommonTableSection>

        <div class="table-pagination">
          <div class="table-pagination-meta">共 {{ total }} 条信息</div>
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            background
            small
            layout="prev, pager, next, sizes"
            @current-change="(p:number) => emit('page-change', p)"
            @size-change="(s:number) => emit('page-size-change', s)"
          />
        </div>
      </section>
    </div>

    <div class="selector-selected-panel">
      <div class="selected-head">
        <span>已选择({{ selectedCount }})</span>
        <el-button text class="clear-btn" @click="handleClearSelection">清空</el-button>
      </div>
      <div class="selected-tags">
        <el-tag
          v-for="tag in selectedTags"
          :key="tag.id"
          size="small"
          closable
          @close="handleRemoveTag(tag.id)"
        >
          {{ tag.label }}
        </el-tag>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" class="confirm-btn" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.common-selector-dialog :deep(.el-dialog__header) {
  padding: 10px 14px;
  border-bottom: 1px solid #ebeef5;
}

.common-selector-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.common-selector-dialog :deep(.el-dialog__title) {
  font-size: 12px;
  font-weight: 600;
}

.selector-query-row {
  padding: 8px 10px 6px;
  border-bottom: 1px solid #ebeef5;
}

.selector-search-btn,
.confirm-btn {
  min-width: 96px;
}

.selector-content {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 350px;
  border-bottom: 1px solid #ebeef5;
}

.selector-tree-panel {
  border-right: 1px solid #ebeef5;
  padding: 8px;
  overflow: auto;
}

.selector-table-panel {
  min-width: 0;
}

.selector-selected-panel {
  padding: 8px 10px;
  min-height: 72px;
}

.selected-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 12px;
}

.clear-btn {
  color: #f56c6c;
}

.selected-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  min-height: 22px;
}

.common-selector-dialog :deep(.el-form-item) {
  margin-bottom: 0;
}

.common-selector-dialog :deep(.el-form-item__label) {
  height: 22px;
  line-height: 22px;
  font-size: 10px;
}

.common-selector-dialog :deep(.el-input),
.common-selector-dialog :deep(.el-select) {
  --el-input-height: 22px;
  font-size: 10px;
}

.common-selector-dialog :deep(.el-input__wrapper),
.common-selector-dialog :deep(.el-select__wrapper) {
  min-height: 22px !important;
  height: 22px !important;
}

.common-selector-dialog :deep(.el-button) {
  height: 22px;
  min-height: 22px;
  padding: 0 10px;
  font-size: 10px;
}

.common-selector-dialog :deep(.el-dialog__footer) {
  padding: 8px 10px 10px;
  border-top: 1px solid #ebeef5;
}

@media (max-width: 960px) {
  .selector-content {
    grid-template-columns: 1fr;
  }

  .selector-tree-panel {
    border-right: 0;
    border-bottom: 1px solid #ebeef5;
    max-height: 180px;
  }
}
</style>
