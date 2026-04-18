<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';
import {
  createGenericInventoryDocumentApi,
  fetchGenericInventoryDocumentDetailApi,
  fetchGenericInventoryDocumentPermissionApi,
  updateGenericInventoryDocumentApi,
  type GenericInventoryDocumentLinePayload,
  type GenericInventoryDocumentSavePayload,
} from '@/api/modules/inventory';
import { fetchCurrentUserRolesApi } from '@/api/modules/auth';
import { fetchStoreSalesmenApi, type SalesmanCandidateItem } from '@/api/modules/system-admin';
import { fetchStoreWarehousesApi, type WarehouseRow } from '@/api/modules/warehouse';
import { useSessionStore } from '@/stores/session';
import type { InventoryDocumentMeta } from '@/views/inventory/document-meta';
import { normalizeOrgId, parseStoreId } from '@/utils/org';

const props = defineProps<{
  meta: InventoryDocumentMeta;
}>();

type WarehouseOption = {
  id: number;
  name: string;
  label: string;
};

type SalesmanOption = {
  userId: number;
  label: string;
};

type DocumentItemRow = {
  id: number;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  unitName: string;
  availableQty: number | null;
  quantity: number | null;
  unitPrice: number | null;
  amount: number | null;
  lineReason: string;
  remark: string;
};

const router = useRouter();
const route = useRoute();
const sessionStore = useSessionStore();
const saving = ref(false);
const loading = ref(false);
const canCreate = ref(false);
const canUpdate = ref(false);
const detailStatus = ref('');
const activeNav = ref('basic');
const warehouses = ref<WarehouseOption[]>([]);
const salesmen = ref<SalesmanOption[]>([]);
const rowSeed = ref(1);
const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'items', label: '物品信息' },
];

const documentId = computed(() => {
  const raw = route.params.id;
  const value = Array.isArray(raw) ? raw[0] : raw;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
});

const isCreateMode = computed(() => route.name === props.meta.createRouteName);
const isViewMode = computed(() => route.name === props.meta.viewRouteName);
const isEditMode = computed(() => route.name === props.meta.editRouteName);
const isReadonlyMode = computed(() => {
  if (isViewMode.value || detailStatus.value === '已审核') {
    return true;
  }
  if (isCreateMode.value) {
    return !canCreate.value;
  }
  if (isEditMode.value) {
    return !canUpdate.value;
  }
  return true;
});

const form = reactive({
  documentCode: '',
  documentDate: '',
  primaryName: '',
  secondaryName: '',
  counterpartyName: '',
  counterpartyName2: '',
  reason: '',
  upstreamCode: '',
  salesmanUserId: undefined as number | undefined,
  salesmanName: '',
  remark: '',
  extraFields: {} as Record<string, string>,
});

const rows = ref<DocumentItemRow[]>([]);

const currentOrgId = computed(() => normalizeOrgId(sessionStore.currentOrgId));

const initExtraFields = () => {
  const target: Record<string, string> = {};
  props.meta.extraFields?.forEach((field) => {
    target[field.key] = '';
  });
  form.extraFields = target;
};

const createEmptyRow = (): DocumentItemRow => ({
  id: rowSeed.value++,
  itemCode: '',
  itemName: '',
  spec: '',
  category: '',
  unitName: '',
  availableQty: null,
  quantity: null,
  unitPrice: null,
  amount: null,
  lineReason: '',
  remark: '',
});

const syncRowAmount = (row: DocumentItemRow) => {
  const quantity = Number(row.quantity ?? 0);
  const unitPrice = Number(row.unitPrice ?? 0);
  row.amount = Number.isFinite(quantity * unitPrice) ? Number((quantity * unitPrice).toFixed(2)) : 0;
};

const loadWarehouses = async () => {
  const storeId = parseStoreId(currentOrgId.value);
  if (!storeId || (!props.meta.primaryField && !props.meta.secondaryField)) {
    warehouses.value = [];
    return;
  }
  try {
    const result = await fetchStoreWarehousesApi(storeId, { status: 'ENABLED' });
    warehouses.value = result.map((item: WarehouseRow) => ({
      id: item.id,
      name: item.warehouseName,
      label: `${item.warehouseName}（${item.warehouseCode}）`,
    }));
  } catch {
    warehouses.value = [];
    ElMessage.error('仓库列表加载失败');
  }
};

const loadSalesmen = async () => {
  if (!currentOrgId.value) {
    salesmen.value = [];
    return;
  }
  try {
    const [salesmanResult] = await Promise.all([
      fetchStoreSalesmenApi(currentOrgId.value),
      fetchCurrentUserRolesApi(currentOrgId.value),
    ]);
    salesmen.value = salesmanResult.map((item: SalesmanCandidateItem) => ({
      userId: item.userId,
      label: `${item.realName}${item.phone ? `（${item.phone}）` : ''}`,
    }));
  } catch {
    salesmen.value = [];
    ElMessage.error('业务员列表加载失败');
  }
};

const loadPermission = async () => {
  if (!currentOrgId.value) {
    canCreate.value = false;
    canUpdate.value = false;
    return;
  }
  try {
    const result = await fetchGenericInventoryDocumentPermissionApi(props.meta.type, currentOrgId.value || undefined);
    canCreate.value = Boolean(result.canCreate);
    canUpdate.value = Boolean(result.canUpdate);
  } catch {
    canCreate.value = false;
    canUpdate.value = false;
    ElMessage.error('权限信息加载失败');
  }
};

const fillDetail = async () => {
  if (!documentId.value) {
    detailStatus.value = '';
    form.documentCode = '';
    form.documentDate = '';
    form.primaryName = '';
    form.secondaryName = '';
    form.counterpartyName = '';
    form.counterpartyName2 = '';
    form.reason = '';
    form.upstreamCode = '';
    form.salesmanUserId = undefined;
    form.salesmanName = '';
    form.remark = '';
    initExtraFields();
    rows.value = [createEmptyRow()];
    return;
  }
  loading.value = true;
  try {
    const detail = await fetchGenericInventoryDocumentDetailApi(props.meta.type, documentId.value, currentOrgId.value || undefined);
    detailStatus.value = detail.status;
    form.documentCode = detail.documentCode;
    form.documentDate = detail.documentDate;
    form.primaryName = detail.primaryName;
    form.secondaryName = detail.secondaryName;
    form.counterpartyName = detail.counterpartyName;
    form.counterpartyName2 = detail.counterpartyName2;
    form.reason = detail.reason;
    form.upstreamCode = detail.upstreamCode;
    form.salesmanUserId = detail.salesmanUserId ?? undefined;
    form.salesmanName = detail.salesmanName;
    form.remark = detail.remark;
    initExtraFields();
    Object.entries(detail.extraFields ?? {}).forEach(([key, value]) => {
      form.extraFields[key] = value;
    });
    rows.value = detail.items.map((item) => ({
      id: rowSeed.value++,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: item.spec,
      category: item.category,
      unitName: item.unitName,
      availableQty: item.availableQty,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      amount: item.amount,
      lineReason: item.lineReason,
      remark: item.remark,
    }));
    if (!rows.value.length) {
      rows.value = [createEmptyRow()];
    }
  } catch {
    detailStatus.value = '';
    initExtraFields();
    rows.value = [createEmptyRow()];
    ElMessage.error(`${props.meta.title}详情加载失败`);
  } finally {
    loading.value = false;
  }
};

const validateRows = () => {
  const validRows = rows.value.filter((item) => item.itemCode.trim() || item.itemName.trim());
  if (!validRows.length) {
    ElMessage.warning('请至少录入一条物品明细');
    return null;
  }
  const invalid = validRows.some((item) => !item.itemCode.trim() || !item.itemName.trim() || !(Number(item.quantity) > 0));
  if (invalid) {
    ElMessage.warning('请完善物品编码、名称和数量');
    return null;
  }
  return validRows;
};

const buildPayload = (): GenericInventoryDocumentSavePayload | null => {
  if (!form.documentDate) {
    ElMessage.warning(`请填写${props.meta.dateLabel}`);
    return null;
  }
  const validRows = validateRows();
  if (!validRows) {
    return null;
  }
  const payload: GenericInventoryDocumentSavePayload = {
    documentDate: form.documentDate,
    primaryName: form.primaryName || undefined,
    secondaryName: form.secondaryName || undefined,
    counterpartyName: form.counterpartyName || undefined,
    counterpartyName2: form.counterpartyName2 || undefined,
    reason: form.reason || undefined,
    upstreamCode: form.upstreamCode || undefined,
    salesmanUserId: form.salesmanUserId,
    salesmanName: form.salesmanName || undefined,
    remark: form.remark || undefined,
    extraFields: form.extraFields,
    items: validRows.map((item): GenericInventoryDocumentLinePayload => ({
      itemCode: item.itemCode.trim(),
      itemName: item.itemName.trim(),
      spec: item.spec || undefined,
      category: item.category || undefined,
      unitName: item.unitName || undefined,
      availableQty: item.availableQty,
      quantity: item.quantity,
      unitPrice: item.unitPrice,
      amount: item.amount,
      lineReason: item.lineReason || undefined,
      remark: item.remark || undefined,
    })),
  };
  return payload;
};

const handleSubmit = async () => {
  if (isReadonlyMode.value) {
    return;
  }
  const payload = buildPayload();
  if (!payload) {
    return;
  }
  saving.value = true;
  try {
    if (documentId.value && isEditMode.value) {
      await updateGenericInventoryDocumentApi(props.meta.type, documentId.value, payload, currentOrgId.value || undefined);
      ElMessage.success('保存成功');
      router.push({ name: props.meta.listRouteName });
      return;
    }
    await createGenericInventoryDocumentApi(props.meta.type, payload, currentOrgId.value || undefined);
    ElMessage.success('保存成功');
    router.push({ name: props.meta.listRouteName });
  } catch {
    ElMessage.error('保存失败');
  } finally {
    saving.value = false;
  }
};

const handleCancel = () => {
  router.push({ name: props.meta.listRouteName });
};

const handleSaveDraft = () => {
  void handleSubmit();
};

const updateSalesmanName = (userId?: number) => {
  const target = salesmen.value.find((item) => item.userId === userId);
  form.salesmanName = target?.label.split('（')[0] ?? '';
};

const reloadPageContext = async () => {
  initExtraFields();
  rows.value = [createEmptyRow()];
  await Promise.all([loadPermission(), loadWarehouses(), loadSalesmen()]);
  await fillDetail();
};

watch(
  () => [sessionStore.currentOrgId, route.name, route.params.id],
  () => {
    void reloadPageContext();
  },
);

onMounted(async () => {
  await reloadPageContext();
});
</script>

<template>
  <section v-loading="loading" class="inventory-document-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeNav"
      :show-actions="!isReadonlyMode"
      @back="handleCancel"
      @save-draft="handleSaveDraft"
      @save="handleSubmit"
      @navigate="(key) => { activeNav = key; }"
    />

    <el-card shadow="never" class="form-card">
      <template #header>
        <span>{{ props.meta.title }}</span>
      </template>
      <el-form label-width="110px">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="单据编号">
              <el-input :model-value="form.documentCode || '保存后生成'" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item :label="props.meta.dateLabel">
              <el-date-picker
                v-model="form.documentDate"
                type="date"
                value-format="YYYY-MM-DD"
                :disabled="isReadonlyMode"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="业务员">
              <el-select v-model="form.salesmanUserId" :disabled="isReadonlyMode" clearable filterable style="width: 100%" @change="updateSalesmanName">
                <el-option v-for="item in salesmen" :key="item.userId" :label="item.label" :value="item.userId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="props.meta.primaryField" :span="8">
            <el-form-item :label="props.meta.primaryField.label">
              <el-select
                v-if="props.meta.primaryField.kind === 'warehouse'"
                v-model="form.primaryName"
                :disabled="isReadonlyMode"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option v-for="item in warehouses" :key="item.id" :label="item.label" :value="item.name" />
              </el-select>
              <el-select
                v-else-if="props.meta.primaryField.kind === 'select'"
                v-model="form.primaryName"
                :disabled="isReadonlyMode"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option v-for="item in props.meta.primaryField.options ?? []" :key="item" :label="item" :value="item" />
              </el-select>
              <el-input v-else v-model="form.primaryName" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col v-if="props.meta.secondaryField" :span="8">
            <el-form-item :label="props.meta.secondaryField.label">
              <el-select
                v-if="props.meta.secondaryField.kind === 'warehouse'"
                v-model="form.secondaryName"
                :disabled="isReadonlyMode"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option v-for="item in warehouses" :key="item.id" :label="item.label" :value="item.name" />
              </el-select>
              <el-input v-else v-model="form.secondaryName" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col v-if="props.meta.counterpartyField" :span="8">
            <el-form-item :label="props.meta.counterpartyField.label">
              <el-select
                v-if="props.meta.counterpartyField.kind === 'select'"
                v-model="form.counterpartyName"
                :disabled="isReadonlyMode"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option v-for="item in props.meta.counterpartyField.options ?? []" :key="item" :label="item" :value="item" />
              </el-select>
              <el-input v-else v-model="form.counterpartyName" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col v-if="props.meta.counterpartyField2" :span="8">
            <el-form-item :label="props.meta.counterpartyField2.label">
              <el-input v-model="form.counterpartyName2" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col v-if="props.meta.reasonField" :span="8">
            <el-form-item :label="props.meta.reasonField.label">
              <el-select
                v-if="props.meta.reasonField.kind === 'select'"
                v-model="form.reason"
                :disabled="isReadonlyMode"
                clearable
                filterable
                style="width: 100%"
              >
                <el-option v-for="item in props.meta.reasonField.options ?? []" :key="item" :label="item" :value="item" />
              </el-select>
              <el-input v-else v-model="form.reason" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col v-for="field in props.meta.extraFields ?? []" :key="field.key" :span="8">
            <el-form-item :label="field.label">
              <el-input v-model="form.extraFields[field.key]" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上游单号">
              <el-input v-model="form.upstreamCode" :disabled="isReadonlyMode" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :disabled="isReadonlyMode" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <el-card shadow="never" class="form-card">
      <template #header>
        <div class="card-header">
          <span>物品信息</span>
          <el-button v-if="!isReadonlyMode" type="primary" link @click="rows.push(createEmptyRow())">新增明细</el-button>
        </div>
      </template>
      <el-table :data="rows" border stripe class="erp-table" :fit="false">
        <el-table-column label="物品编码" min-width="130">
          <template #default="{ row }">
            <el-input v-model="row.itemCode" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column label="物品名称" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.itemName" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column label="规格" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.spec" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="120">
          <template #default="{ row }">
            <el-input v-model="row.category" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column label="单位" min-width="100">
          <template #default="{ row }">
            <el-input v-model="row.unitName" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column v-if="props.meta.showAvailableQty" label="可用数量" min-width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.availableQty" :disabled="isReadonlyMode" :precision="4" :min="0" controls-position="right" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="数量" min-width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.quantity" :disabled="isReadonlyMode" :precision="4" :min="0" controls-position="right" style="width: 100%" @change="syncRowAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="单价" min-width="100">
          <template #default="{ row }">
            <el-input-number v-model="row.unitPrice" :disabled="isReadonlyMode" :precision="4" :min="0" controls-position="right" style="width: 100%" @change="syncRowAmount(row)" />
          </template>
        </el-table-column>
        <el-table-column label="金额" min-width="110">
          <template #default="{ row }">
            <span>{{ Number(row.amount ?? 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原因" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.lineReason" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.remark" :disabled="isReadonlyMode" />
          </template>
        </el-table-column>
        <el-table-column v-if="!isReadonlyMode" label="操作" width="100" fixed="right">
          <template #default="{ $index }">
            <el-button type="danger" link @click="rows.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.inventory-document-create-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
