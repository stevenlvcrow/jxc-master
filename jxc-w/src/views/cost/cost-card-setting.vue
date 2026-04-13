<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';

type YesNoAll = '全部' | '是' | '否';
type CostCardType = '全部' | '门店自建' | '集团创建' | '集团管控';
type TempDish = '是' | '否';

type QueryForm = {
  dishInfo: string;
  configuredCostCard: YesNoAll;
  costCardType: CostCardType;
  itemType: string;
  hasSubstitute: YesNoAll;
  dishDeleted: YesNoAll;
  showTemporaryDish: TempDish;
};

type DishNode = {
  id: string;
  label: string;
  children?: DishNode[];
};

type CostCardSettingRow = {
  id: number;
  dishSpuCode: string;
  dishSkuCode: string;
  dishName: string;
  spec: string;
  category: string;
  sellPrice: number;
  costPriceTax: number;
  grossProfitTax: number;
  targetGrossProfit: number;
  configuredCostCard: '是' | '否';
  costCardType: '门店自建' | '集团创建' | '集团管控';
  itemType: string;
  hasSubstitute: '是' | '否';
  dishDeleted: '是' | '否';
  isTemporaryDish: '是' | '否';
  operatedAt: string;
};

const defaultQuery: QueryForm = {
  dishInfo: '',
  configuredCostCard: '全部',
  costCardType: '全部',
  itemType: '全部',
  hasSubstitute: '全部',
  dishDeleted: '全部',
  showTemporaryDish: '否',
};

const query = reactive<QueryForm>({ ...defaultQuery });
const currentPage = ref(1);
const pageSize = ref(10);
const selectedCount = ref(0);
const selectedTreeNode = ref('all');
const tableHeight = ref(260);
const mainPanelRef = ref<HTMLElement | null>(null);
const queryRef = ref<HTMLElement | null>(null);
const toolbarRef = ref<HTMLElement | null>(null);
const paginationRef = ref<HTMLElement | null>(null);

const itemTypeOptions = ['全部', '成品', '半成品', '套餐', '饮品'];
const toolbarButtons = [
  '批量替换物品信息',
  '批量设置替代料',
  '批量设置净料率',
  '批量导入',
  '批量导出',
  '自动关联菜品和成本卡',
  '自动关联记录',
];

const dishTreeData: DishNode[] = [
  {
    id: 'all',
    label: '全部菜品',
    children: [
      {
        id: 'hot-dish',
        label: '热菜',
        children: [
          { id: 'sichuan', label: '川湘菜' },
          { id: 'stir-fry', label: '小炒' },
        ],
      },
      {
        id: 'cold-dish',
        label: '凉菜',
      },
      {
        id: 'soup',
        label: '汤羹',
      },
      {
        id: 'drink',
        label: '饮品',
      },
      {
        id: 'temp',
        label: '临时菜',
      },
    ],
  },
];

const treeCategoryMap: Record<string, string[]> = {
  all: [],
  'hot-dish': ['川湘菜', '小炒', '炖菜'],
  sichuan: ['川湘菜'],
  'stir-fry': ['小炒'],
  'cold-dish': ['凉菜', '小吃'],
  soup: ['汤羹'],
  drink: ['饮品'],
  temp: [],
};

const tableData: CostCardSettingRow[] = [
  {
    id: 1,
    dishSpuCode: 'SPU-0001',
    dishSkuCode: 'SKU-0001-01',
    dishName: '宫保鸡丁',
    spec: '标准份',
    category: '川湘菜',
    sellPrice: 38,
    costPriceTax: 16.8,
    grossProfitTax: 55.79,
    targetGrossProfit: 58,
    configuredCostCard: '是',
    costCardType: '集团管控',
    itemType: '成品',
    hasSubstitute: '是',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-08 16:12:00',
  },
  {
    id: 2,
    dishSpuCode: 'SPU-0002',
    dishSkuCode: 'SKU-0002-01',
    dishName: '鱼香肉丝',
    spec: '标准份',
    category: '川湘菜',
    sellPrice: 32,
    costPriceTax: 14.2,
    grossProfitTax: 55.63,
    targetGrossProfit: 56,
    configuredCostCard: '是',
    costCardType: '集团创建',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-08 15:36:00',
  },
  {
    id: 3,
    dishSpuCode: 'SPU-0003',
    dishSkuCode: 'SKU-0003-01',
    dishName: '红油抄手',
    spec: '大份',
    category: '小吃',
    sellPrice: 22,
    costPriceTax: 10.4,
    grossProfitTax: 52.73,
    targetGrossProfit: 55,
    configuredCostCard: '是',
    costCardType: '门店自建',
    itemType: '半成品',
    hasSubstitute: '是',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-07 20:08:00',
  },
  {
    id: 4,
    dishSpuCode: 'SPU-0004',
    dishSkuCode: 'SKU-0004-01',
    dishName: '凉拌黄瓜',
    spec: '标准份',
    category: '凉菜',
    sellPrice: 12,
    costPriceTax: 4.8,
    grossProfitTax: 60,
    targetGrossProfit: 62,
    configuredCostCard: '否',
    costCardType: '门店自建',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-07 18:30:00',
  },
  {
    id: 5,
    dishSpuCode: 'SPU-0005',
    dishSkuCode: 'SKU-0005-02',
    dishName: '番茄牛腩',
    spec: '标准份',
    category: '炖菜',
    sellPrice: 48,
    costPriceTax: 24.8,
    grossProfitTax: 48.33,
    targetGrossProfit: 52,
    configuredCostCard: '是',
    costCardType: '集团管控',
    itemType: '成品',
    hasSubstitute: '是',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-06 14:02:00',
  },
  {
    id: 6,
    dishSpuCode: 'SPU-0006',
    dishSkuCode: 'SKU-0006-01',
    dishName: '蒜香排条',
    spec: '小份',
    category: '小炒',
    sellPrice: 28,
    costPriceTax: 17.9,
    grossProfitTax: 36.07,
    targetGrossProfit: 42,
    configuredCostCard: '否',
    costCardType: '门店自建',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '是',
    operatedAt: '2026-04-04 10:34:00',
  },
  {
    id: 7,
    dishSpuCode: 'SPU-0007',
    dishSkuCode: 'SKU-0007-01',
    dishName: '香菇鸡汤',
    spec: '大碗',
    category: '汤羹',
    sellPrice: 26,
    costPriceTax: 11.4,
    grossProfitTax: 56.15,
    targetGrossProfit: 58,
    configuredCostCard: '是',
    costCardType: '集团创建',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-05 11:15:00',
  },
  {
    id: 8,
    dishSpuCode: 'SPU-0008',
    dishSkuCode: 'SKU-0008-01',
    dishName: '柠檬茶',
    spec: '500ml',
    category: '饮品',
    sellPrice: 16,
    costPriceTax: 4.6,
    grossProfitTax: 71.25,
    targetGrossProfit: 68,
    configuredCostCard: '是',
    costCardType: '集团创建',
    itemType: '饮品',
    hasSubstitute: '是',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-03 13:18:00',
  },
  {
    id: 9,
    dishSpuCode: 'SPU-0009',
    dishSkuCode: 'SKU-0009-01',
    dishName: '麻婆豆腐',
    spec: '标准份',
    category: '川湘菜',
    sellPrice: 22,
    costPriceTax: 8.7,
    grossProfitTax: 60.45,
    targetGrossProfit: 61,
    configuredCostCard: '是',
    costCardType: '集团管控',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '是',
    isTemporaryDish: '否',
    operatedAt: '2026-04-03 09:22:00',
  },
  {
    id: 10,
    dishSpuCode: 'SPU-0010',
    dishSkuCode: 'SKU-0010-01',
    dishName: '糖醋里脊',
    spec: '标准份',
    category: '小炒',
    sellPrice: 34,
    costPriceTax: 14.3,
    grossProfitTax: 57.94,
    targetGrossProfit: 58,
    configuredCostCard: '是',
    costCardType: '集团管控',
    itemType: '成品',
    hasSubstitute: '是',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-02 19:28:00',
  },
  {
    id: 11,
    dishSpuCode: 'SPU-0011',
    dishSkuCode: 'SKU-0011-01',
    dishName: '青瓜沙拉',
    spec: '标准份',
    category: '凉菜',
    sellPrice: 18,
    costPriceTax: 6.2,
    grossProfitTax: 65.56,
    targetGrossProfit: 63,
    configuredCostCard: '否',
    costCardType: '门店自建',
    itemType: '套餐',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '是',
    operatedAt: '2026-04-02 11:56:00',
  },
  {
    id: 12,
    dishSpuCode: 'SPU-0012',
    dishSkuCode: 'SKU-0012-01',
    dishName: '玉米排骨汤',
    spec: '中碗',
    category: '汤羹',
    sellPrice: 24,
    costPriceTax: 11.9,
    grossProfitTax: 50.42,
    targetGrossProfit: 53,
    configuredCostCard: '是',
    costCardType: '集团创建',
    itemType: '成品',
    hasSubstitute: '否',
    dishDeleted: '否',
    isTemporaryDish: '否',
    operatedAt: '2026-04-01 14:56:00',
  },
];

const filteredData = computed(() => {
  const keyword = query.dishInfo.trim().toLowerCase();

  return tableData.filter((row) => {
    const matchedKeyword = !keyword
      || row.dishSpuCode.toLowerCase().includes(keyword)
      || row.dishSkuCode.toLowerCase().includes(keyword)
      || row.dishName.toLowerCase().includes(keyword);
    const matchedConfigured = query.configuredCostCard === '全部' || row.configuredCostCard === query.configuredCostCard;
    const matchedCardType = query.costCardType === '全部' || row.costCardType === query.costCardType;
    const matchedItemType = query.itemType === '全部' || row.itemType === query.itemType;
    const matchedSubstitute = query.hasSubstitute === '全部' || row.hasSubstitute === query.hasSubstitute;
    const matchedDeleted = query.dishDeleted === '全部' || row.dishDeleted === query.dishDeleted;
    const matchedTempDish = row.isTemporaryDish === query.showTemporaryDish;

    const matchedTree = (() => {
      if (selectedTreeNode.value === 'all') {
        return true;
      }
      if (selectedTreeNode.value === 'temp') {
        return row.isTemporaryDish === '是';
      }
      const categories = treeCategoryMap[selectedTreeNode.value] ?? [];
      return categories.includes(row.category);
    })();

    return matchedKeyword
      && matchedConfigured
      && matchedCardType
      && matchedItemType
      && matchedSubstitute
      && matchedDeleted
      && matchedTempDish
      && matchedTree;
  });
});

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredData.value.slice(start, start + pageSize.value);
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  Object.assign(query, defaultQuery);
  selectedTreeNode.value = 'all';
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`已触发：${action}`);
};

const handleSelectionChange = (rows: CostCardSettingRow[]) => {
  selectedCount.value = rows.length;
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};

const handleTreeNodeClick = (data: DishNode) => {
  selectedTreeNode.value = data.id;
  currentPage.value = 1;
};

const formatPercent = (value: number) => `${value.toFixed(2)}%`;
const formatPrice = (value: number) => value.toFixed(2);

const updateTableHeight = () => {
  const mainPanel = mainPanelRef.value;
  if (!mainPanel) {
    return;
  }

  const panelTop = mainPanel.getBoundingClientRect().top;
  const viewportBottomGap = 24;
  const availableHeight = window.innerHeight - panelTop - viewportBottomGap;
  const queryHeight = queryRef.value?.offsetHeight ?? 0;
  const toolbarHeight = toolbarRef.value?.offsetHeight ?? 0;
  const paginationHeight = paginationRef.value?.offsetHeight ?? 0;
  const panelInnerPadding = 16;
  const sectionGaps = 12;
  const nextHeight = availableHeight - queryHeight - toolbarHeight - paginationHeight - panelInnerPadding - sectionGaps;

  tableHeight.value = Math.max(120, Math.floor(nextHeight));
};

onMounted(() => {
  nextTick(() => {
    updateTableHeight();
    window.addEventListener('resize', updateTableHeight);
  });
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateTableHeight);
});
</script>

<template>
  <div class="item-management-layout">
    <section class="panel category-panel">
      <div class="section-head">
        <strong>菜品树</strong>
      </div>
      <el-tree
        :data="dishTreeData"
        node-key="id"
        default-expand-all
        class="category-tree"
        @node-click="handleTreeNodeClick"
      />
    </section>

    <section ref="mainPanelRef" class="panel item-main-panel">
      <el-form ref="queryRef" :model="query" inline class="filter-bar compact-filter-bar">
        <el-form-item label="菜品信息">
          <el-input
            v-model="query.dishInfo"
            placeholder="编码/名称"
            clearable
            style="width: 150px"
          />
        </el-form-item>

        <el-form-item label="配置成本卡">
          <el-select v-model="query.configuredCostCard" style="width: 96px">
            <el-option label="全部" value="全部" />
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>

        <el-form-item label="成本卡类型">
          <el-select v-model="query.costCardType" style="width: 108px">
            <el-option label="全部" value="全部" />
            <el-option label="门店自建" value="门店自建" />
            <el-option label="集团创建" value="集团创建" />
            <el-option label="集团管控" value="集团管控" />
          </el-select>
        </el-form-item>

        <el-form-item label="物品类型">
          <el-select v-model="query.itemType" style="width: 98px">
            <el-option
              v-for="option in itemTypeOptions"
              :key="option"
              :label="option"
              :value="option"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="设置替代料">
          <el-select v-model="query.hasSubstitute" style="width: 96px">
            <el-option label="全部" value="全部" />
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>

        <el-form-item label="菜品是否删除">
          <el-select v-model="query.dishDeleted" style="width: 96px">
            <el-option label="全部" value="全部" />
            <el-option label="是" value="是" />
            <el-option label="否" value="否" />
          </el-select>
        </el-form-item>

        <el-form-item label="是否展示临时菜">
          <el-radio-group v-model="query.showTemporaryDish">
            <el-radio value="是">是</el-radio>
            <el-radio value="否">否</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div ref="toolbarRef" class="table-toolbar">
        <el-button
          v-for="(button, index) in toolbarButtons"
          :key="button"
          :type="index === 0 ? 'primary' : 'default'"
          @click="handleToolbarAction(button)"
        >
          {{ button }}
        </el-button>
      </div>

      <el-table
        :data="pagedData"
        :fit="false"
        border
        stripe
        scrollbar-always-on
        class="erp-table"
        :max-height="tableHeight"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="44" fixed="left" />
        <el-table-column type="index" label="序号" width="56" fixed="left" />
        <el-table-column prop="dishSpuCode" label="菜品SPU编码" width="120" show-overflow-tooltip />
        <el-table-column prop="dishSkuCode" label="菜品SKU编码" width="120" show-overflow-tooltip />
        <el-table-column prop="dishName" label="菜品名称" width="120" show-overflow-tooltip />
        <el-table-column prop="spec" label="规格" width="92" show-overflow-tooltip />
        <el-table-column prop="category" label="菜品分类" width="96" show-overflow-tooltip />
        <el-table-column label="售卖价" width="86" align="right">
          <template #default="{ row }">
            {{ formatPrice(row.sellPrice) }}
          </template>
        </el-table-column>
        <el-table-column label="成本价（含税）" width="96" align="right">
          <template #default="{ row }">
            {{ formatPrice(row.costPriceTax) }}
          </template>
        </el-table-column>
        <el-table-column label="成本卡毛利率（含税）" width="130" align="right">
          <template #default="{ row }">
            {{ formatPercent(row.grossProfitTax) }}
          </template>
        </el-table-column>
        <el-table-column label="目标毛利率" width="92" align="right">
          <template #default="{ row }">
            {{ formatPercent(row.targetGrossProfit) }}
          </template>
        </el-table-column>
        <el-table-column prop="configuredCostCard" label="配置成本卡" width="88" />
        <el-table-column prop="dishDeleted" label="菜品是否删除" width="96" />
        <el-table-column prop="operatedAt" label="操作时间" width="142" show-overflow-tooltip />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default>
            <el-button text type="primary">配置</el-button>
            <el-button text>详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div ref="paginationRef" class="table-pagination">
        <div class="table-pagination-meta">已选 {{ selectedCount }} 条</div>
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :total="filteredData.length"
          background
          small
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>
  </div>
</template>


