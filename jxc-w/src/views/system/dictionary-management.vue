<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  createDictionaryApi,
  createDictionaryItemApi,
  deleteDictionaryApi,
  deleteDictionaryItemApi,
  fetchDictionariesApi,
  fetchDictionaryItemsApi,
  updateDictionaryApi,
  updateDictionaryItemApi,
  updateDictionaryItemStatusApi,
  updateDictionaryStatusApi,
  type DictionaryItem,
  type DictionaryStatus,
  type DictionaryTypeItem,
} from '@/api/modules/dictionary';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

const COMMON_STATUS_DICT = 'common.enabled_status';

const { optionsOf, refresh: refreshStatusOptions } = useDictionaryOptions([COMMON_STATUS_DICT]);
const statusOptions = optionsOf(COMMON_STATUS_DICT);

const typeQuery = reactive({
  keyword: '',
  category: '',
});
const typePage = reactive({
  pageNum: 1,
  pageSize: 20,
  total: 0,
});
const typeRows = ref<DictionaryTypeItem[]>([]);
const selectedTypeId = ref<number | null>(null);
const typeLoading = ref(false);
const itemLoading = ref(false);
const itemRows = ref<DictionaryItem[]>([]);

const selectedType = computed(() => (
  typeRows.value.find((item) => item.id === selectedTypeId.value) ?? null
));

const statusLabel = (status: string) => (
  statusOptions.value.find((item) => item.itemCode === status)?.itemLabel ?? status
);

const typeDialogVisible = ref(false);
const typeSaving = ref(false);
const typeDialogMode = ref<'create' | 'edit'>('create');
const editingType = ref<DictionaryTypeItem | null>(null);
const typeFormRef = ref();
const typeForm = reactive({
  dictCode: '',
  dictName: '',
  category: '',
  status: 'ENABLED' as DictionaryStatus,
  sortNo: 100,
  remark: '',
});

const itemDialogVisible = ref(false);
const itemSaving = ref(false);
const itemDialogMode = ref<'create' | 'edit'>('create');
const editingItem = ref<DictionaryItem | null>(null);
const itemFormRef = ref();
const itemForm = reactive({
  parentId: null as number | null,
  itemKey: '',
  itemCode: '',
  itemLabel: '',
  status: 'ENABLED' as DictionaryStatus,
  sortNo: 100,
  extraJson: '',
  remark: '',
});

const typeRules = {
  dictCode: [{ required: true, message: '请输入字典编码', trigger: 'blur' }],
  dictName: [{ required: true, message: '请输入字典名称', trigger: 'blur' }],
};

const itemRules = {
  itemKey: [{ required: true, message: '请输入稳定键', trigger: 'blur' }],
  itemCode: [{ required: true, message: '请输入存储值', trigger: 'blur' }],
  itemLabel: [{ required: true, message: '请输入展示文案', trigger: 'blur' }],
};

const itemParentOptions = computed(() => (
  itemRows.value.filter((item) => item.id !== editingItem.value?.id)
));

const loadTypes = async () => {
  typeLoading.value = true;
  try {
    const data = await fetchDictionariesApi({
      keyword: typeQuery.keyword.trim() || undefined,
      category: typeQuery.category.trim() || undefined,
      pageNum: typePage.pageNum,
      pageSize: typePage.pageSize,
    });
    typeRows.value = data.list ?? [];
    typePage.total = data.total ?? 0;
    if (!typeRows.value.some((item) => item.id === selectedTypeId.value)) {
      selectedTypeId.value = typeRows.value[0]?.id ?? null;
    }
    await loadItems();
  } finally {
    typeLoading.value = false;
  }
};

const loadItems = async () => {
  if (!selectedTypeId.value) {
    itemRows.value = [];
    return;
  }
  itemLoading.value = true;
  try {
    itemRows.value = await fetchDictionaryItemsApi(selectedTypeId.value);
  } finally {
    itemLoading.value = false;
  }
};

const selectType = async (row: DictionaryTypeItem) => {
  selectedTypeId.value = row.id;
  await loadItems();
};

const resetTypeQuery = async () => {
  typeQuery.keyword = '';
  typeQuery.category = '';
  typePage.pageNum = 1;
  await loadTypes();
};

const openCreateType = () => {
  typeDialogMode.value = 'create';
  editingType.value = null;
  Object.assign(typeForm, {
    dictCode: '',
    dictName: '',
    category: '',
    status: 'ENABLED',
    sortNo: 100,
    remark: '',
  });
  typeDialogVisible.value = true;
};

const openEditType = (row: DictionaryTypeItem) => {
  typeDialogMode.value = 'edit';
  editingType.value = row;
  Object.assign(typeForm, {
    dictCode: row.dictCode,
    dictName: row.dictName,
    category: row.category,
    status: row.status,
    sortNo: row.sortNo,
    remark: row.remark ?? '',
  });
  typeDialogVisible.value = true;
};

const submitType = async () => {
  await typeFormRef.value?.validate();
  typeSaving.value = true;
  try {
    const payload = {
      dictCode: typeForm.dictCode.trim(),
      dictName: typeForm.dictName.trim(),
      category: typeForm.category.trim(),
      status: typeForm.status,
      sortNo: Number(typeForm.sortNo ?? 100),
      remark: typeForm.remark.trim(),
    };
    if (typeDialogMode.value === 'create') {
      await createDictionaryApi(payload);
      ElMessage.success('新增成功');
    } else if (editingType.value) {
      await updateDictionaryApi(editingType.value.id, payload);
      ElMessage.success('保存成功');
    }
    typeDialogVisible.value = false;
    await loadTypes();
    await refreshStatusOptions();
  } finally {
    typeSaving.value = false;
  }
};

const toggleTypeStatus = async (row: DictionaryTypeItem) => {
  const nextStatus: DictionaryStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  await updateDictionaryStatusApi(row.id, nextStatus);
  ElMessage.success('状态已更新');
  await loadTypes();
  await refreshStatusOptions();
};

const removeType = async (row: DictionaryTypeItem) => {
  await ElMessageBox.confirm(`确定删除字典“${row.dictName}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  });
  await deleteDictionaryApi(row.id);
  ElMessage.success('删除成功');
  await loadTypes();
};

const openCreateItem = () => {
  if (!selectedType.value) {
    ElMessage.warning('请先选择字典类型');
    return;
  }
  itemDialogMode.value = 'create';
  editingItem.value = null;
  Object.assign(itemForm, {
    parentId: null,
    itemKey: '',
    itemCode: '',
    itemLabel: '',
    status: 'ENABLED',
    sortNo: 100,
    extraJson: '',
    remark: '',
  });
  itemDialogVisible.value = true;
};

const openEditItem = (row: DictionaryItem) => {
  itemDialogMode.value = 'edit';
  editingItem.value = row;
  Object.assign(itemForm, {
    parentId: row.parentId,
    itemKey: row.itemKey,
    itemCode: row.itemCode,
    itemLabel: row.itemLabel,
    status: row.status,
    sortNo: row.sortNo,
    extraJson: row.extraJson ?? '',
    remark: row.remark ?? '',
  });
  itemDialogVisible.value = true;
};

const submitItem = async () => {
  await itemFormRef.value?.validate();
  if (!selectedTypeId.value) {
    return;
  }
  if (itemForm.extraJson.trim()) {
    try {
      JSON.parse(itemForm.extraJson.trim());
    } catch {
      ElMessage.error('扩展 JSON 格式不正确');
      return;
    }
  }
  itemSaving.value = true;
  try {
    const payload = {
      parentId: itemForm.parentId,
      itemKey: itemForm.itemKey.trim(),
      itemCode: itemForm.itemCode.trim(),
      itemLabel: itemForm.itemLabel.trim(),
      status: itemForm.status,
      sortNo: Number(itemForm.sortNo ?? 100),
      extraJson: itemForm.extraJson.trim(),
      remark: itemForm.remark.trim(),
    };
    if (itemDialogMode.value === 'create') {
      await createDictionaryItemApi(selectedTypeId.value, payload);
      ElMessage.success('新增成功');
    } else if (editingItem.value) {
      await updateDictionaryItemApi(editingItem.value.id, payload);
      ElMessage.success('保存成功');
    }
    itemDialogVisible.value = false;
    await loadItems();
    await refreshStatusOptions();
  } finally {
    itemSaving.value = false;
  }
};

const toggleItemStatus = async (row: DictionaryItem) => {
  const nextStatus: DictionaryStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  await updateDictionaryItemStatusApi(row.id, nextStatus);
  ElMessage.success('状态已更新');
  await loadItems();
  await refreshStatusOptions();
};

const removeItem = async (row: DictionaryItem) => {
  await ElMessageBox.confirm(`确定删除字典项“${row.itemLabel}”吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  });
  await deleteDictionaryItemApi(row.id);
  ElMessage.success('删除成功');
  await loadItems();
};

onMounted(() => {
  void loadTypes();
});
</script>

<template>
  <div class="dictionary-page">
    <section class="panel dictionary-types-panel">
      <el-form :model="typeQuery" inline class="filter-bar compact-filter-bar">
        <el-form-item label="字典">
          <el-input v-model="typeQuery.keyword" clearable placeholder="编码或名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="typeQuery.category" clearable placeholder="分类" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTypes">查询</el-button>
          <el-button @click="resetTypeQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" @click="openCreateType">新增字典</el-button>
      </div>

      <el-table
        v-loading="typeLoading"
        :data="typeRows"
        border
        stripe
        highlight-current-row
        class="erp-table"
        @row-click="selectType"
      >
        <el-table-column prop="dictCode" label="字典编码" min-width="180">
          <template #default="{ row }">
            <span class="code-text">{{ row.dictCode }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="dictName" label="字典名称" min-width="140" />
        <el-table-column prop="category" label="分类" min-width="110" />
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内置" width="78" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.builtin" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="78" align="center" />
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openEditType(row)">编辑</el-button>
            <el-button link type="primary" @click.stop="toggleTypeStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :disabled="row.builtin" @click.stop="removeType(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          v-model:current-page="typePage.pageNum"
          v-model:page-size="typePage.pageSize"
          :total="typePage.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @change="loadTypes"
        />
      </div>
    </section>

    <section class="panel dictionary-items-panel">
      <div class="dictionary-item-header">
        <div>
          <div class="panel-title">{{ selectedType?.dictName ?? '字典项' }}</div>
          <div class="panel-subtitle">{{ selectedType?.dictCode ?? '请选择字典类型' }}</div>
        </div>
        <el-button type="primary" :disabled="!selectedType" @click="openCreateItem">新增字典项</el-button>
      </div>

      <el-table
        v-loading="itemLoading"
        :data="itemRows"
        border
        stripe
        row-key="id"
        class="erp-table"
        :empty-text="selectedType ? '暂无字典项' : '请选择字典类型'"
      >
        <el-table-column prop="itemKey" label="稳定键" min-width="140">
          <template #default="{ row }">
            <span class="code-text">{{ row.itemKey }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="itemCode" label="存储值" min-width="140" />
        <el-table-column prop="itemLabel" label="展示文案" min-width="140" />
        <el-table-column label="父级" min-width="130">
          <template #default="{ row }">
            {{ itemRows.find((item) => item.id === row.parentId)?.itemLabel ?? '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="88" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="内置" width="78" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.builtin" type="warning" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" width="78" align="center" />
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditItem(row)">编辑</el-button>
            <el-button link type="primary" @click="toggleItemStatus(row)">
              {{ row.status === 'ENABLED' ? '停用' : '启用' }}
            </el-button>
            <el-button link type="danger" :disabled="row.builtin" @click="removeItem(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>

  <el-dialog
    v-model="typeDialogVisible"
    :title="typeDialogMode === 'create' ? '新增字典' : '编辑字典'"
    width="560px"
    class="standard-form-dialog"
    destroy-on-close
  >
    <el-form ref="typeFormRef" :model="typeForm" :rules="typeRules" label-width="94px">
      <el-form-item label="字典编码" prop="dictCode">
        <el-input v-model="typeForm.dictCode" :disabled="editingType?.builtin" clearable />
      </el-form-item>
      <el-form-item label="字典名称" prop="dictName">
        <el-input v-model="typeForm.dictName" clearable />
      </el-form-item>
      <el-form-item label="分类">
        <el-input v-model="typeForm.category" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="typeForm.status" style="width: 180px">
          <el-option
            v-for="option in statusOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="typeForm.sortNo" :min="0" :max="999999" controls-position="right" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="typeForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="typeDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="typeSaving" @click="submitType">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="itemDialogVisible"
    :title="itemDialogMode === 'create' ? '新增字典项' : '编辑字典项'"
    width="600px"
    class="standard-form-dialog"
    destroy-on-close
  >
    <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="94px">
      <el-form-item label="父级">
        <el-select v-model="itemForm.parentId" clearable style="width: 220px">
          <el-option
            v-for="option in itemParentOptions"
            :key="option.id"
            :label="option.itemLabel"
            :value="option.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="稳定键" prop="itemKey">
        <el-input v-model="itemForm.itemKey" :disabled="editingItem?.builtin" clearable />
      </el-form-item>
      <el-form-item label="存储值" prop="itemCode">
        <el-input v-model="itemForm.itemCode" clearable />
      </el-form-item>
      <el-form-item label="展示文案" prop="itemLabel">
        <el-input v-model="itemForm.itemLabel" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="itemForm.status" style="width: 180px">
          <el-option
            v-for="option in statusOptions"
            :key="option.itemCode"
            :label="option.itemLabel"
            :value="option.itemCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="itemForm.sortNo" :min="0" :max="999999" controls-position="right" />
      </el-form-item>
      <el-form-item label="扩展 JSON">
        <el-input v-model="itemForm.extraJson" type="textarea" :rows="3" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="itemForm.remark" type="textarea" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="itemDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="itemSaving" @click="submitItem">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dictionary-page {
  display: grid;
  grid-template-columns: minmax(480px, 0.9fr) minmax(520px, 1.1fr);
  gap: 16px;
}

.dictionary-types-panel,
.dictionary-items-panel {
  min-width: 0;
}

.dictionary-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.panel-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.code-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', monospace;
  font-size: 12px;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1180px) {
  .dictionary-page {
    grid-template-columns: 1fr;
  }
}
</style>
