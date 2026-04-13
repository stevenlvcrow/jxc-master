<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';

type WarehouseStatus = '启用' | '停用';
type WarehouseType = '出品及生产部门' | '行政部门' | '普通仓库';
type WarehouseRow = {
  id: number;
  warehouseCode: string;
  warehouseName: string;
  department: string;
  status: WarehouseStatus;
  warehouseType: WarehouseType;
  contactName: string;
  contactPhone: string;
  address: string;
  targetGrossMargin: string;
  idealPurchaseSaleRatio: string;
  operatedAt: string;
};
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
      {
        value: '闵行区',
        label: '闵行区',
        children: [
          { value: '七宝镇', label: '七宝镇' },
          { value: '华漕镇', label: '华漕镇' },
        ],
      },
      {
        value: '浦东新区',
        label: '浦东新区',
        children: [
          { value: '川沙新镇', label: '川沙新镇' },
          { value: '张江镇', label: '张江镇' },
        ],
      },
    ],
  },
];
const createDialogVisible = ref(false);
const createFormRef = ref<FormInstance>();
const createForm = reactive<WarehouseForm>({
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
const createFormRules: FormRules<WarehouseForm> = {
  warehouseType: [{ required: true, message: '请选择仓库类型', trigger: 'change' }],
  warehouseCode: [{ required: true, message: '请输入仓库编码', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  region: [{ required: true, message: '请选择区域', trigger: 'change' }],
  targetGrossMargin: [{ required: true, message: '请输入目标毛利率', trigger: 'blur' }],
  idealPurchaseSaleRatio: [{ required: true, message: '请输入理想采销比', trigger: 'blur' }],
};

const query = reactive({
  warehouseInfo: '',
  status: '全部' as (typeof statusOptions)[number],
  warehouseType: '全部' as (typeof warehouseTypeOptions)[number],
});

const tableData = ref<WarehouseRow[]>([
  {
    id: 1,
    warehouseCode: 'WH-001',
    warehouseName: '中央成品仓',
    department: '供应链中心',
    status: '启用',
    warehouseType: '出品及生产部门',
    contactName: '张敏',
    contactPhone: '13800138001',
    address: '上海市闵行区申昆路 88 号',
    targetGrossMargin: '58%',
    idealPurchaseSaleRatio: '1:2.4',
    operatedAt: '2026-04-13 10:26:00',
  },
  {
    id: 2,
    warehouseCode: 'WH-002',
    warehouseName: '北区原料仓',
    department: '采购部',
    status: '启用',
    warehouseType: '普通仓库',
    contactName: '王磊',
    contactPhone: '13800138002',
    address: '上海市嘉定区安亭镇安虹路 16 号',
    targetGrossMargin: '52%',
    idealPurchaseSaleRatio: '1:2.1',
    operatedAt: '2026-04-12 16:18:00',
  },
  {
    id: 3,
    warehouseCode: 'WH-003',
    warehouseName: '南区包材仓',
    department: '营运部',
    status: '启用',
    warehouseType: '普通仓库',
    contactName: '李娜',
    contactPhone: '13800138003',
    address: '上海市松江区九亭镇伴亭路 29 号',
    targetGrossMargin: '48%',
    idealPurchaseSaleRatio: '1:1.8',
    operatedAt: '2026-04-11 14:40:00',
  },
  {
    id: 4,
    warehouseCode: 'WH-004',
    warehouseName: '东区冷藏仓',
    department: '仓储部',
    status: '停用',
    warehouseType: '行政部门',
    contactName: '周凯',
    contactPhone: '13800138004',
    address: '上海市浦东新区川沙路 168 号',
    targetGrossMargin: '55%',
    idealPurchaseSaleRatio: '1:2.2',
    operatedAt: '2026-04-10 09:12:00',
  },
  {
    id: 5,
    warehouseCode: 'WH-005',
    warehouseName: '西区原料仓',
    department: '供应链中心',
    status: '启用',
    warehouseType: '普通仓库',
    contactName: '赵晨',
    contactPhone: '13800138005',
    address: '上海市青浦区崧华路 56 号',
    targetGrossMargin: '51%',
    idealPurchaseSaleRatio: '1:2.0',
    operatedAt: '2026-04-09 11:35:00',
  },
]);

const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const filteredRows = computed(() => {
  const keyword = query.warehouseInfo.trim().toLowerCase();
  return tableData.value.filter((row) => {
    const matchedKeyword = !keyword
      || row.warehouseCode.toLowerCase().includes(keyword)
      || row.warehouseName.toLowerCase().includes(keyword)
      || row.contactName.toLowerCase().includes(keyword);
    const matchedStatus = query.status === '全部' || row.status === query.status;
    const matchedType = query.warehouseType === '全部' || row.warehouseType === query.warehouseType;
    return matchedKeyword && matchedStatus && matchedType;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.warehouseInfo = '';
  query.status = '全部';
  query.warehouseType = '全部';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    createDialogVisible.value = true;
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: WarehouseRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleEdit = (row: WarehouseRow) => {
  ElMessage.info(`编辑：${row.warehouseName}`);
};

const handleView = (row: WarehouseRow) => {
  ElMessage.info(`查看：${row.warehouseName}`);
};

const handleSetDefault = (row: WarehouseRow) => {
  ElMessage.info(`设为默认：${row.warehouseName}`);
};

const handleDelete = (row: WarehouseRow) => {
  ElMessage.info(`删除：${row.warehouseName}`);
};

const resetCreateForm = () => {
  createForm.warehouseType = '出品及生产部门';
  createForm.warehouseCode = '';
  createForm.warehouseName = '';
  createForm.department = '';
  createForm.enabled = true;
  createForm.contactName = '';
  createForm.contactPhone = '';
  createForm.region = [];
  createForm.address = '';
  createForm.targetGrossMargin = '';
  createForm.idealPurchaseSaleRatio = '';
};

const closeCreateDialog = () => {
  createDialogVisible.value = false;
  createFormRef.value?.clearValidate();
  resetCreateForm();
};

const handleCreateSubmit = async () => {
  if (!createFormRef.value) {
    return;
  }
  const valid = await createFormRef.value.validate().catch(() => false);
  if (!valid) {
    return;
  }

  tableData.value.unshift({
    id: Date.now(),
    warehouseCode: createForm.warehouseCode.trim(),
    warehouseName: createForm.warehouseName.trim(),
    department: createForm.department.trim() || '-',
    status: createForm.enabled ? '启用' : '停用',
    warehouseType: createForm.warehouseType as WarehouseType,
    contactName: createForm.contactName.trim(),
    contactPhone: createForm.contactPhone.trim(),
    address: [createForm.region.join(' / '), createForm.address.trim()].filter(Boolean).join(' '),
    targetGrossMargin: `${createForm.targetGrossMargin.trim()}%`,
    idealPurchaseSaleRatio: `${createForm.idealPurchaseSaleRatio.trim()}%`,
    operatedAt: new Date().toLocaleString('sv-SE').replace('T', ' '),
  });
  currentPage.value = 1;
  ElMessage.success('新增仓库成功');
  closeCreateDialog();
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
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </CommonQuerySection>

    <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

    <el-table
      :data="pagedRows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      :empty-text="'当前机构暂无数据'"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="warehouseCode" label="仓库编码" min-width="120" show-overflow-tooltip />
      <el-table-column prop="warehouseName" label="仓库名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="department" label="所属部门" min-width="120" show-overflow-tooltip />
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === '启用' ? 'success' : 'info'" size="small">
            {{ row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="warehouseType" label="仓库类型" min-width="110" show-overflow-tooltip />
      <el-table-column prop="contactName" label="联系人" min-width="100" show-overflow-tooltip />
      <el-table-column prop="contactPhone" label="联系电话" min-width="130" show-overflow-tooltip />
      <el-table-column prop="address" label="详细地址" min-width="220" show-overflow-tooltip />
      <el-table-column prop="targetGrossMargin" label="目标毛利率" min-width="100" show-overflow-tooltip />
      <el-table-column prop="idealPurchaseSaleRatio" label="理想采销比" min-width="100" show-overflow-tooltip />
      <el-table-column prop="operatedAt" label="操作时间" min-width="170" show-overflow-tooltip />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="handleView(row)">查看</el-button>
          <el-button text type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button text @click="handleSetDefault(row)">设为默认</el-button>
          <el-button text @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
      <div class="table-pagination-meta">已选 {{ selectedIds.length }} 条</div>
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="filteredRows.length"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </div>
  </section>

  <el-dialog
    v-model="createDialogVisible"
    title="新增仓库"
    width="560px"
    class="standard-form-dialog"
    destroy-on-close
    @closed="closeCreateDialog"
  >
    <el-form
      ref="createFormRef"
      :model="createForm"
      :rules="createFormRules"
      label-width="90px"
      class="standard-dialog-form"
    >
      <el-form-item label="仓库类型" prop="warehouseType">
        <el-select v-model="createForm.warehouseType" style="width: 100%">
          <el-option
            v-for="option in warehouseFormTypeOptions"
            :key="option"
            :label="option"
            :value="option"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="createForm.warehouseCode" placeholder="请输入仓库编码" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="createForm.warehouseName" placeholder="请输入仓库名称" />
      </el-form-item>
      <el-form-item label="所属部门" prop="department">
        <el-select v-model="createForm.department" clearable placeholder="请选择所属部门" style="width: 100%">
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
          v-model="createForm.enabled"
          inline-prompt
          active-text="启用"
          inactive-text="停用"
        />
      </el-form-item>
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="createForm.contactName" placeholder="请输入联系人" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactPhone">
        <el-input v-model="createForm.contactPhone" placeholder="请输入联系电话" />
      </el-form-item>
      <el-form-item label="区域" prop="region">
        <el-cascader
          v-model="createForm.region"
          :options="regionOptions"
          clearable
          style="width: 100%"
          placeholder="请选择区域"
        />
      </el-form-item>
      <el-form-item label="详细地址" prop="address">
        <el-input
          v-model="createForm.address"
          type="textarea"
          :rows="3"
          placeholder="请输入详细地址"
        />
      </el-form-item>
      <el-form-item label="目标毛利率" prop="targetGrossMargin">
        <el-input v-model="createForm.targetGrossMargin" placeholder="请输入目标毛利率">
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
      <el-form-item label="理想采销比" prop="idealPurchaseSaleRatio">
        <el-input v-model="createForm.idealPurchaseSaleRatio" placeholder="请输入理想采销比">
          <template #suffix>%</template>
        </el-input>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="closeCreateDialog">取消</el-button>
      <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
