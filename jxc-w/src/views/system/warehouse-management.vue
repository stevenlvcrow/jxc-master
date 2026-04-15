<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';
import { useSessionStore } from '@/stores/session';
import {
  fetchWarehousesApi,
  createWarehouseApi,
  updateWarehouseApi,
  deleteWarehouseApi,
  setWarehouseDefaultApi,
  updateWarehouseStatusApi,
  type WarehouseRow,
  type WarehouseCreatePayload,
} from '@/api/modules/warehouse';
import { fetchAdminGroupsApi, type GroupAdminItem } from '@/api/modules/system-admin';

const sessionStore = useSessionStore();

type WarehouseType = '出品及生产部门' | '行政部门' | '普通仓库';

type WarehouseForm = {
  warehouseType: string;
  warehouseCode: string;
  warehouseName: string;
  department: string;
  enabled: boolean;
  contactName: string;
  contactPhone: string;
  region: string[];
  address: string;
  targetGrossMargin: string;
  idealPurchaseSaleRatio: string;
};

const toolbarButtons: ToolbarButton[] = [
  { key: '新增', label: '新增', type: 'primary' },
];

const statusOptions = ['全部', '启用', '停用'] as const;
const warehouseTypeOptions = ['全部', '出品及生产部门', '行政部门', '普通仓库'] as const;
const warehouseFormTypeOptions: WarehouseType[] = ['出品及生产部门', '行政部门', '普通仓库'];
const departmentOptions = ['供应链中心', '采购部', '营运部', '仓储部', '出品部', '生产部'];
const regionOptions = [
  {
    value: '上海市',
    label: '上海市',
    children: [
      { value: '闵行区', label: '闵行区', children: [{ value: '七宝镇', label: '七宝镇' }, { value: '华漕镇', label: '华漕镇' }] },
      { value: '浦东新区', label: '浦东新区', children: [{ value: '川沙新镇', label: '川沙新镇' }, { value: '张江镇', label: '张江镇' }] },
    ],
  },
];

// Dialog state
const dialogVisible = ref(false);
const dialogTitle = ref('新增仓库');
const isEdit = ref(false);
const editingId = ref<number | null>(null);

// View detail dialog state
const viewDialogVisible = ref(false);
const viewingRow = ref<WarehouseRow | null>(null);
const formRef = ref<FormInstance>();
const form = reactive<WarehouseForm>({
  warehouseType: '出品及生产部门',
  warehouseCode: '',
  warehouseName: '',
  department: '',
  enabled: true,
  contactName: '',
  contactPhone: '',
  region: [],
  address: '',
  targetGrossMargin: '',
  idealPurchaseSaleRatio: '',
});
const formRules: FormRules<WarehouseForm> = {
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  region: [{ required: true, message: '请选择区域', trigger: 'change' }],
  targetGrossMargin: [{ required: true, message: '请输入目标毛利率', trigger: 'blur' }],
  idealPurchaseSaleRatio: [{ required: true, message: '请输入理想采销比', trigger: 'blur' }],
};

// Query state
const query = reactive({
  warehouseInfo: '',
  status: '全部' as (typeof statusOptions)[number],
  warehouseType: '全部' as (typeof warehouseTypeOptions)[number],
});

// Group / Table state
const groups = ref<GroupAdminItem[]>([]);
const selectedGroupId = ref<number>();
const tableData = ref<WarehouseRow[]>([]);
const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);
const loading = ref(false);

// Computed (all data shown, server-side filtering)
const totalItems = computed(() => tableData.value.length);
const isStatusEnabled = (status: WarehouseRow['status'] | undefined | null) => status === 'ENABLED';
const formatStatusLabel = (status: WarehouseRow['status'] | undefined | null) => {
  if (!status) {
    return '-';
  }
  return status === 'ENABLED' ? '启用' : '停用';
};

/** Load all groups and auto-select the one matching currentOrgId */
const loadGroups = async () => {
  groups.value = await fetchAdminGroupsApi();
  if (!groups.value.length) {
    selectedGroupId.value = undefined;
    tableData.value = [];
    return;
  }
  const currentOrgId = String(sessionStore.currentOrgId ?? '').trim().toLowerCase();
  if (!selectedGroupId.value) {
    if (currentOrgId.startsWith('group-')) {
      const currentId = Number(currentOrgId.slice('group-'.length));
      if (!Number.isNaN(currentId) && groups.value.some((item) => item.id === currentId)) {
        selectedGroupId.value = currentId;
      }
    }
    if (!selectedGroupId.value) {
      selectedGroupId.value = groups.value[0].id;
    }
  }
};

/** Load warehouses from backend */
const loadData = async () => {
  const groupId = selectedGroupId.value;
  if (!groupId) {
    tableData.value = [];
    return;
  }
  loading.value = true;
  try {
    const rows = await fetchWarehousesApi(groupId, {
      keyword: query.warehouseInfo.trim() || undefined,
      status: query.status !== '全部' ? query.status : undefined,
      warehouseType: query.warehouseType !== '全部' ? query.warehouseType : undefined,
    });
    tableData.value = rows;
  } catch (err) {
    console.error('[warehouse] loadData error:', err);
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  loading.value = true;
  try {
    await loadGroups();
    await loadData();
  } finally {
    loading.value = false;
  }
});

const handleSearch = () => {
  currentPage.value = 1;
  loadData();
};

const handleReset = () => {
  query.warehouseInfo = '';
  query.status = '全部';
  query.warehouseType = '全部';
  currentPage.value = 1;
  loadData();
};

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    if (!selectedGroupId.value) {
      ElMessage.warning('请先选择集团');
      return;
    }
    openCreateDialog();
  }
};

const handleSelectionChange = (rows: WarehouseRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

/** Map UI status to API status */
const toApiStatus = (enabled: boolean): 'ENABLED' | 'DISABLED' => enabled ? 'ENABLED' : 'DISABLED';

/** Build payload from form */
const buildPayload = (): WarehouseCreatePayload => ({
  warehouseCode: form.warehouseCode.trim(),
  warehouseName: form.warehouseName.trim(),
  department: form.department.trim() || undefined,
  status: toApiStatus(form.enabled),
  warehouseType: form.warehouseType as WarehouseType,
  contactName: form.contactName.trim(),
  contactPhone: form.contactPhone.trim(),
  regionPath: form.region.length > 0 ? form.region.join('/') : undefined,
  address: form.address.trim(),
  targetGrossMargin: form.targetGrossMargin.trim(),
  idealPurchaseSaleRatio: form.idealPurchaseSaleRatio.trim(),
});

/** Format datetime for display */
const formatDateTime = (iso: string | undefined | null): string => {
  if (!iso) return '-';
  try {
    const d = new Date(iso);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const h = String(d.getHours()).padStart(2, '0');
    const min = String(d.getMinutes()).padStart(2, '0');
    const s = String(d.getSeconds()).padStart(2, '0');
    return `${y}-${m}-${day} ${h}:${min}:${s}`;
  } catch {
    return String(iso);
  }
};

/** Open create dialog */
const openCreateDialog = () => {
  isEdit.value = false;
  editingId.value = null;
  dialogTitle.value = '新增仓库';
  resetForm();
  dialogVisible.value = true;
};

/** Open edit dialog */
const handleEdit = async (row: WarehouseRow) => {
  isEdit.value = true;
  editingId.value = row.id;
  dialogTitle.value = '编辑仓库';

  form.warehouseType = row.warehouseType || '普通仓库';
  form.warehouseCode = row.warehouseCode;
  form.warehouseName = row.warehouseName;
  form.department = row.department || '';
  form.enabled = isStatusEnabled(row.status);
  form.contactName = row.contactName || '';
  form.contactPhone = row.contactPhone || '';

  // Parse regionPath back into array
  if (row.address) {
    const parts = row.address.split(' ').filter(Boolean);
    // Try to match known region prefixes
    let regionStr = '';
    let addrPart = row.address;
    for (const opt of regionOptions[0].children ?? []) {
      if (row.address.includes(opt.value)) {
        for (const sub of (opt.children ?? [])) {
          if (row.address.includes(sub.value)) {
            regionStr = [regionOptions[0].value, opt.value, sub.value].join(',');
            addrPart = row.address.replace(regionStr.replace(/,/g, '/'), '').trim();
            break;
          }
        }
        break;
      }
    }
    form.region = regionStr ? regionStr.split(',') : [];
    form.address = addrPart;
  } else {
    form.region = [];
    form.address = '';
  }

  form.targetGrossMargin = row.targetGrossMargin?.replace('%', '') || '';
  form.idealPurchaseSaleRatio = row.idealPurchaseSaleRatio || '';

  dialogVisible.value = true;
};

const handleView = (row: WarehouseRow) => {
  viewingRow.value = row;
  viewDialogVisible.value = true;
};

const handleSetDefault = async (row: WarehouseRow) => {
  try {
    await setWarehouseDefaultApi(row.id);
    ElMessage.success('已设为默认');
    loadData();
  } catch {
    // error handled by http-client
  }
};

const handleDelete = async (row: WarehouseRow) => {
  try {
    await ElMessageBox.confirm(`确认删除仓库「${row.warehouseName}」？`, '提示', { type: 'warning' });
    await deleteWarehouseApi(row.id);
    ElMessage.success('删除成功');
    loadData();
  } catch (e: unknown) {
    if ((e as any) !== 'cancel') {
      // error handled by http-client or cancelled
    }
  }
};

const handleToggleStatus = async (row: WarehouseRow) => {
  const newStatus: 'ENABLED' | 'DISABLED' = isStatusEnabled(row.status) ? 'DISABLED' : 'ENABLED';
  try {
    await updateWarehouseStatusApi(row.id, newStatus);
    ElMessage.success(newStatus === 'ENABLED' ? '已启用' : '已停用');
    loadData();
  } catch {
    // error handled by http-client
  }
};

const resetForm = () => {
  form.warehouseType = '出品及生产部门';
  form.warehouseCode = '';
  form.warehouseName = '';
  form.department = '';
  form.enabled = true;
  form.contactName = '';
  form.contactPhone = '';
  form.region = [];
  form.address = '';
  form.targetGrossMargin = '';
  form.idealPurchaseSaleRatio = '';
};

const closeDialog = () => {
  dialogVisible.value = false;
  formRef.value?.clearValidate();
  resetForm();
};

const handleSubmit = async () => {
  if (!formRef.value) {
    ElMessage.warning('表单未就绪，请重试');
    return;
  }
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;

  const groupId = selectedGroupId.value;
  if (!groupId) {
    ElMessage.warning('请先选择集团');
    return;
  }

  try {
    if (isEdit.value && editingId.value != null) {
      await updateWarehouseApi(editingId.value, buildPayload());
      ElMessage.success('编辑成功');
    } else {
      await createWarehouseApi(groupId, buildPayload());
      ElMessage.success('新增成功');
    }
    closeDialog();
    loadData();
  } catch {
    // error handled by http-client
  }
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
};

const handlePageSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="所属集团" v-if="groups.length > 1">
        <el-select v-model="selectedGroupId" style="width: 200px">
          <el-option
            v-for="group in groups"
            :key="group.id"
            :label="`${group.groupName} (${group.groupCode})`"
            :value="group.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库信息">
        <el-input
          v-model="query.warehouseInfo"
          placeholder="请输入仓库编码、名称或联系人"
          clearable
          style="width: 220px"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" style="width: 120px">
          <el-option
            v-for="option in statusOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库类型">
        <el-select v-model="query.warehouseType" style="width: 120px">
          <el-option
            v-for="option in warehouseTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

    <el-table
      :data="tableData"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
      v-loading="loading"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="department" label="所属部门" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-switch
            :model-value="isStatusEnabled(row.status)"
            @change="handleToggleStatus(row)"
          />
        </template>
      </el-table-column>
      <el-table-column prop="warehouseType" label="仓库类型" min-width="110" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
      <el-table-column prop="address" label="详细地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="targetGrossMargin" label="目标毛利率" min-width="100" show-overflow-tooltip>
        <template #default="{ row }">{{ row.targetGrossMargin }}%</template>
      </el-table-column>
      <el-table-column prop="idealPurchaseSaleRatio" label="理想采销比" min-width="100" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="操作时间" min-width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button text :disabled="row.isDefault" @click="handleSetDefault(row)">设为默认</el-button>
          <el-button text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="totalItems"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>

  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="560px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeDialog"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="formRules"
      label-width="90px"
      class="standard-dialog-form"
    >
      <el-form-item label="仓库类型" prop="warehouseType">
        <el-select v-model="form.warehouseType" style="width: 100%">
          <el-option
            v-for="option in warehouseFormTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" :disabled="isEdit" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
      </el-form-item>
      <el-form-item label="所属部门" prop="department">
        <el-select v-model="form.department" clearable placeholder="请选择所属部门" style="width: 100%">
          <el-option
            v-for="option in departmentOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-switch
          v-model="form.enabled"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="form.contactName" placeholder="请输入联系人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="区域" prop="region">
        <el-cascader
          v-model="form.region"
          :options="regionOptions"
          clearable
          style="width: 100%"
          placeholder="请选择区域"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input
          v-model="form.address"
          type="textarea"
          :rows="3"
          placeholder="请输入详细地址"
        />
      </el-form-item>
      <el-form-item label="目标毛利率" prop="targetGrossMargin">
        <el-input v-model="form.targetGrossMargin" placeholder="请输入目标毛利率">
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
      <el-form-item label="理想采销比" prop="idealPurchaseSaleRatio">
        <el-input v-model="form.idealPurchaseSaleRatio" placeholder="请输入理想采销比">
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <!-- 查看详情对话框 -->
  <el-dialog
    v-model="viewDialogVisible"
    title="仓库详情"
    width="480px"
    class="standard-form-dialog"
  >
    <el-descriptions :column="2" border size="small" label-class-name="detail-label">
      <el-descriptions-item label="仓库编码">{{ viewingRow?.warehouseCode }}</el-descriptions-item>
      <el-descriptions-item label="仓库名称">{{ viewingRow?.warehouseName }}</el-descriptions-item>
      <el-descriptions-item label="仓库类型">{{ viewingRow?.warehouseType }}</el-descriptions-item>
      <el-descriptions-item label="所属部门">
        {{ viewingRow?.department || '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="isStatusEnabled(viewingRow?.status) ? 'success' : 'danger'" size="small">
          {{ formatStatusLabel(viewingRow?.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="是否默认">
        <el-tag :type="viewingRow?.isDefault ? '' : 'info'" size="small">
          {{ viewingRow?.isDefault ? '是' : '否' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="联系人">{{ viewingRow?.contactName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="联系电话">{{ viewingRow?.contactPhone || '-' }}</el-descriptions-item>
      <el-descriptions-item label="详细地址" :span="2">{{ viewingRow?.address || '-' }}</el-descriptions-item>
      <el-descriptions-item label="目标毛利率">
        {{ viewingRow?.targetGrossMargin ? `${viewingRow.targetGrossMargin}%` : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="理想采销比">
        {{ viewingRow?.idealPurchaseSaleRatio ? `${viewingRow.idealPurchaseSaleRatio}%` : '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="操作时间" :span="2">{{ formatDateTime(viewingRow?.updatedAt) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="viewDialogVisible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>
