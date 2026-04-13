<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import {
  ArrowDown,
  Back,
  Check,
  CloseBold,
  Delete,
  Plus,
  RefreshLeft,
  RefreshRight,
  Search,
  Upload,
} from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';

type PricingEnabledStatus = '启用' | '停用';
type PricingRuleStatus = '草稿' | '已提交' | '已审核';
type PricingWriteResult = '成功' | '失败' | '处理中';
type PricingType = '供应商定价' | '仓库定价' | '活动定价';
type PricingEffectiveStatus = '未生效' | '生效中' | '已失效';
type CreateMode = '手工创建' | '批量导入' | '规则生成';
type PricingRow = {
  id: number;
  pricingCode: string;
  pricingName: string;
  pricingType: PricingType;
  supplier: string;
  effectiveDate: string;
  expireDate: string;
  enabledStatus: PricingEnabledStatus;
  warehouse: string;
  orderStatus: PricingRuleStatus;
  writeResult: PricingWriteResult;
  updatedBy: string;
  updatedAt: string;
};
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const router = useRouter();
const tabs: PageTabItem[] = [
  { key: 'item', label: '物品维度' },
  { key: 'rule', label: '规则维度' },
];

const activeTab = ref('item');
const expandFilters = ref(false);
const selectedIds = ref<number[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const pricingTypeOptions: PricingType[] = ['供应商定价', '仓库定价', '活动定价'];
const enabledStatusOptions: PricingEnabledStatus[] = ['启用', '停用'];
const effectiveStatusOptions: PricingEffectiveStatus[] = ['未生效', '生效中', '已失效'];
const orderStatusOptions: PricingRuleStatus[] = ['草稿', '已提交', '已审核'];
const writeResultOptions: PricingWriteResult[] = ['成功', '失败', '处理中'];
const createModeOptions: CreateMode[] = ['手工创建', '批量导入', '规则生成'];
const warehouseOptions = ['中央成品仓', '北区原料仓', '南区包材仓'];
const itemOptions = ['鸡胸肉', '牛腩', '包装盒', '酸梅汤'];

const supplierTree: TreeNode[] = [
  {
    value: 'supplier-group-a',
    label: '华东供应商组',
    children: [
      { value: 'supplier-1', label: '鲜达食品' },
      { value: 'supplier-2', label: '优选农场' },
    ],
  },
  {
    value: 'supplier-group-b',
    label: '直营供应商组',
    children: [
      { value: 'supplier-3', label: '沪上冷链' },
      { value: 'supplier-4', label: '盒马包材' },
    ],
  },
];

const creatorTree: TreeNode[] = [
  {
    value: 'dept-pricing',
    label: '定价组',
    children: [
      { value: 'creator-1', label: '张敏' },
      { value: 'creator-2', label: '李娜' },
    ],
  },
  {
    value: 'dept-procurement',
    label: '采购组',
    children: [
      { value: 'creator-3', label: '王磊' },
      { value: 'creator-4', label: '赵晨' },
    ],
  },
];

const query = reactive({
  pricingCode: '',
  pricingName: '',
  pricingType: '',
  supplier: '',
  warehouse: '',
  enabledStatus: '启用',
  effectiveStatus: '',
  orderStatus: '',
  writeResult: '',
  itemName: '',
  creator: '',
  createMode: '',
});

const tableData: PricingRow[] = [
  {
    id: 1,
    pricingCode: 'PJ-202604-001',
    pricingName: '鲜达食品四月统配定价',
    pricingType: '供应商定价',
    supplier: '鲜达食品',
    effectiveDate: '2026-04-01',
    expireDate: '2026-04-30',
    enabledStatus: '启用',
    warehouse: '中央成品仓',
    orderStatus: '已审核',
    writeResult: '成功',
    updatedBy: '张敏',
    updatedAt: '2026-04-13 10:22:00',
  },
  {
    id: 2,
    pricingCode: 'PJ-202604-002',
    pricingName: '优选农场蔬菜定价',
    pricingType: '供应商定价',
    supplier: '优选农场',
    effectiveDate: '2026-04-08',
    expireDate: '2026-05-08',
    enabledStatus: '启用',
    warehouse: '北区原料仓',
    orderStatus: '已提交',
    writeResult: '处理中',
    updatedBy: '李娜',
    updatedAt: '2026-04-12 16:48:00',
  },
  {
    id: 3,
    pricingCode: 'PJ-202604-003',
    pricingName: '盒马包材季度定价',
    pricingType: '活动定价',
    supplier: '盒马包材',
    effectiveDate: '2026-04-10',
    expireDate: '2026-06-30',
    enabledStatus: '停用',
    warehouse: '南区包材仓',
    orderStatus: '草稿',
    writeResult: '失败',
    updatedBy: '王磊',
    updatedAt: '2026-04-11 09:30:00',
  },
];

const treeLabelMap = (nodes: TreeNode[]) => {
  const map = new Map<string, string>();
  const walk = (currentNodes: TreeNode[]) => {
    currentNodes.forEach((node) => {
      map.set(node.value, node.label);
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(nodes);
  return map;
};

const supplierLabelMap = treeLabelMap(supplierTree);
const creatorLabelMap = treeLabelMap(creatorTree);

const filteredRows = computed(() => {
  const pricingCodeKeyword = query.pricingCode.trim().toLowerCase();
  const pricingNameKeyword = query.pricingName.trim().toLowerCase();
  const supplierLabel = query.supplier ? supplierLabelMap.get(query.supplier) ?? query.supplier : '';

  return tableData.filter((row) => {
    const matchedCode = !pricingCodeKeyword || row.pricingCode.toLowerCase().includes(pricingCodeKeyword);
    const matchedName = !pricingNameKeyword || row.pricingName.toLowerCase().includes(pricingNameKeyword);
    const matchedType = !query.pricingType || row.pricingType === query.pricingType;
    const matchedSupplier = !supplierLabel || row.supplier === supplierLabel;
    const matchedWarehouse = !query.warehouse || row.warehouse === query.warehouse;
    const matchedEnabledStatus = !query.enabledStatus || row.enabledStatus === query.enabledStatus;
    const matchedOrderStatus = !query.orderStatus || row.orderStatus === query.orderStatus;
    const matchedWriteResult = !query.writeResult || row.writeResult === query.writeResult;
    const matchedItem = !query.itemName || row.pricingName.includes(query.itemName);
    const matchedCreator = !query.creator || row.updatedBy === (creatorLabelMap.get(query.creator) ?? query.creator);
    const matchedEffectiveStatus = !query.effectiveStatus || query.effectiveStatus === '生效中';
    const matchedCreateMode = !query.createMode || query.createMode === '手工创建';
    return matchedCode
      && matchedName
      && matchedType
      && matchedSupplier
      && matchedWarehouse
      && matchedEnabledStatus
      && matchedOrderStatus
      && matchedWriteResult
      && matchedItem
      && matchedCreator
      && matchedEffectiveStatus
      && matchedCreateMode;
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
  query.pricingCode = '';
  query.pricingName = '';
  query.pricingType = '';
  query.supplier = '';
  query.warehouse = '';
  query.enabledStatus = '启用';
  query.effectiveStatus = '';
  query.orderStatus = '';
  query.writeResult = '';
  query.itemName = '';
  query.creator = '';
  query.createMode = '';
  currentPage.value = 1;
  expandFilters.value = false;
};

const handleToolbarAction = (action: string) => {
  if (action === '新增') {
    router.push('/purchase/1/1/create');
    return;
  }
  ElMessage.info(`${action}功能待接入`);
};

const handleSelectionChange = (rows: PricingRow[]) => {
  selectedIds.value = rows.map((row) => row.id);
};

const handleView = (row: PricingRow) => {
  ElMessage.info(`查看：${row.pricingName}`);
};

const handleEdit = (row: PricingRow) => {
  ElMessage.info(`编辑：${row.pricingName}`);
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
    <PageTabsLayout v-model:active-tab="activeTab" :tabs="tabs">
      <template v-if="activeTab === 'item'">
        <el-alert
          title="请至【采购定价明细】中查看相关信息"
          type="info"
          :closable="false"
          show-icon
        />
      </template>

      <template v-else>
        <CommonQuerySection :model="query">
          <el-form-item label="定价单编号">
            <el-input
              v-model="query.pricingCode"
              placeholder="请输入定价单编号"
              clearable
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item label="定价单名称">
            <el-input
              v-model="query.pricingName"
              placeholder="请输入定价单名称"
              clearable
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item label="定价单类型">
            <el-select
              v-model="query.pricingType"
              clearable
              placeholder="请选择定价单类型"
              style="width: 160px"
            >
              <el-option
                v-for="option in pricingTypeOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="供应商">
            <el-tree-select
              v-model="query.supplier"
              :data="supplierTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              check-strictly
              default-expand-all
              clearable
              placeholder="请选择供应商"
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item label="仓库">
            <el-select
              v-model="query.warehouse"
              clearable
              placeholder="请选择仓库"
              style="width: 150px"
            >
              <el-option
                v-for="option in warehouseOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="定价启用状态">
            <el-select
              v-model="query.enabledStatus"
              clearable
              placeholder="请选择定价启用状态"
              style="width: 150px"
            >
              <el-option
                v-for="option in enabledStatusOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>

          <template v-if="expandFilters">
            <el-form-item label="定价生效状态">
              <el-tree-select
                v-model="query.effectiveStatus"
                :data="effectiveStatusOptions.map((item) => ({ value: item, label: item }))"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                clearable
                check-strictly
                placeholder="请选择定价生效状态"
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="定价单状态">
              <el-select
                v-model="query.orderStatus"
                clearable
                placeholder="请选择定价单状态"
                style="width: 150px"
              >
                <el-option
                  v-for="option in orderStatusOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="定价写入结果">
              <el-select
                v-model="query.writeResult"
                clearable
                placeholder="请选择定价写入结果"
                style="width: 150px"
              >
                <el-option
                  v-for="option in writeResultOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="物品">
              <el-select
                v-model="query.itemName"
                clearable
                placeholder="请选择物品"
                style="width: 150px"
              >
                <el-option
                  v-for="option in itemOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="创建人">
              <el-tree-select
                v-model="query.creator"
                :data="creatorTree"
                :props="{ label: 'label', value: 'value', children: 'children' }"
                clearable
                check-strictly
                default-expand-all
                placeholder="请选择创建人"
                style="width: 150px"
              />
            </el-form-item>
            <el-form-item label="创建方式">
              <el-select
                v-model="query.createMode"
                clearable
                placeholder="请选择创建方式"
                style="width: 150px"
              >
                <el-option
                  v-for="option in createModeOptions"
                  :key="option"
                  :label="option"
                  :value="option"
                />
              </el-select>
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
            <el-button @click="expandFilters = !expandFilters">
              <el-icon><ArrowDown /></el-icon>
              {{ expandFilters ? '收起筛选' : '展开筛选' }}
            </el-button>
          </el-form-item>
        </CommonQuerySection>

        <div class="table-toolbar">
          <el-button type="primary" @click="handleToolbarAction('新增')">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button @click="handleToolbarAction('批量导入')">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
          <el-button @click="handleToolbarAction('批量提交')">
            <el-icon><Check /></el-icon>
            批量提交
          </el-button>
          <el-button @click="handleToolbarAction('批量删除')">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
          <el-button @click="handleToolbarAction('批量撤回')">
            <el-icon><Back /></el-icon>
            批量撤回
          </el-button>
          <el-button @click="handleToolbarAction('批量启用')">
            <el-icon><Check /></el-icon>
            批量启用
          </el-button>
          <el-button @click="handleToolbarAction('批量停用')">
            <el-icon><CloseBold /></el-icon>
            批量停用
          </el-button>
          <el-button @click="handleToolbarAction('批量反审核')">
            <el-icon><RefreshLeft /></el-icon>
            批量反审核
          </el-button>
        </div>

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
          <el-table-column prop="pricingCode" label="定价单编号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="pricingName" label="定价单名称" min-width="200" show-overflow-tooltip />
          <el-table-column prop="pricingType" label="定价单类型" min-width="120" show-overflow-tooltip />
          <el-table-column prop="supplier" label="供应商" min-width="140" show-overflow-tooltip />
          <el-table-column prop="effectiveDate" label="价格生效日期" min-width="120" show-overflow-tooltip />
          <el-table-column prop="expireDate" label="价格失效日期" min-width="120" show-overflow-tooltip />
          <el-table-column prop="enabledStatus" label="定价启用状态" min-width="120" show-overflow-tooltip />
          <el-table-column prop="warehouse" label="适用仓库" min-width="120" show-overflow-tooltip />
          <el-table-column prop="orderStatus" label="定价单状态" min-width="110" show-overflow-tooltip />
          <el-table-column prop="writeResult" label="定价写入结果" min-width="120" show-overflow-tooltip />
          <el-table-column prop="updatedBy" label="最后修改人" min-width="100" show-overflow-tooltip />
          <el-table-column prop="updatedAt" label="最后修改时间" min-width="170" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button text type="primary" @click="handleView(row)">查看</el-button>
              <el-button text @click="handleEdit(row)">编辑</el-button>
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
      </template>
    </PageTabsLayout>
  </section>
</template>
