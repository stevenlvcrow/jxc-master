<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';

type CostCardRow = {
  id: number;
  costCardName: string;
  costCardCode: string;
  costPriceTax: string;
  itemCostTax: string;
  otherCost: string;
  dishType: string;
  dishSpec: string;
  costCardType: string;
  linkedDishCount: number;
  operatedAt: string;
  lotteryInfo: string;
  itemInfo: string;
};

type QueryForm = {
  costCardInfo: string;
  lotteryInfo: string;
  itemInfo: string;
};

const defaultQuery: QueryForm = {
  costCardInfo: '',
  lotteryInfo: '',
  itemInfo: '',
};

const query = reactive<QueryForm>({ ...defaultQuery });
const router = useRouter();
const currentPage = ref(1);
const pageSize = ref(10);
const selectedCount = ref(0);
const tableHeight = 420;

const toolbarButtons = [
  '新增成本卡',
  '批量替换物品信息',
  '批量设置替代料',
  '批量设置净料率',
  '批量修改',
  '批量导入',
  '批量导出',
  '自动关联菜品和成本卡',
  '自动关联记录',
];

const tableData: CostCardRow[] = [
  {
    id: 1,
    costCardName: '宫保鸡丁标准卡',
    costCardCode: 'CBK-0001',
    costPriceTax: '18.60',
    itemCostTax: '16.90',
    otherCost: '1.70',
    dishType: '热菜',
    dishSpec: '标准份',
    costCardType: '标准成本卡',
    linkedDishCount: 12,
    operatedAt: '2026-04-08 16:12:00',
    lotteryInfo: 'CP-001 宫保鸡丁 GBCD',
    itemInfo: '鸡腿肉/花生米/干辣椒',
  },
  {
    id: 2,
    costCardName: '鱼香肉丝标准卡',
    costCardCode: 'CBK-0002',
    costPriceTax: '14.20',
    itemCostTax: '12.80',
    otherCost: '1.40',
    dishType: '热菜',
    dishSpec: '标准份',
    costCardType: '标准成本卡',
    linkedDishCount: 9,
    operatedAt: '2026-04-08 15:26:00',
    lotteryInfo: 'CP-002 鱼香肉丝 YSRS',
    itemInfo: '里脊肉/木耳/笋丝',
  },
  {
    id: 3,
    costCardName: '酸辣汤门店卡',
    costCardCode: 'CBK-0003',
    costPriceTax: '6.50',
    itemCostTax: '5.70',
    otherCost: '0.80',
    dishType: '汤羹',
    dishSpec: '中碗',
    costCardType: '门店成本卡',
    linkedDishCount: 6,
    operatedAt: '2026-04-07 20:08:00',
    lotteryInfo: 'CP-003 酸辣汤 SLT',
    itemInfo: '鸡蛋/豆腐/木耳',
  },
  {
    id: 4,
    costCardName: '清炒时蔬季节卡',
    costCardCode: 'CBK-0004',
    costPriceTax: '8.00',
    itemCostTax: '7.10',
    otherCost: '0.90',
    dishType: '素菜',
    dishSpec: '标准份',
    costCardType: '临时成本卡',
    linkedDishCount: 4,
    operatedAt: '2026-04-07 18:30:00',
    lotteryInfo: 'CP-004 清炒时蔬 QCSS',
    itemInfo: '青菜/蒜蓉/食用油',
  },
  {
    id: 5,
    costCardName: '番茄牛腩试制卡',
    costCardCode: 'CBK-0005',
    costPriceTax: '27.40',
    itemCostTax: '24.80',
    otherCost: '2.60',
    dishType: '炖菜',
    dishSpec: '标准份',
    costCardType: '试制成本卡',
    linkedDishCount: 3,
    operatedAt: '2026-04-06 14:02:00',
    lotteryInfo: 'CP-005 番茄牛腩 FQNN',
    itemInfo: '牛腩/番茄/洋葱',
  },
  {
    id: 6,
    costCardName: '红烧排骨标准卡',
    costCardCode: 'CBK-0006',
    costPriceTax: '23.30',
    itemCostTax: '21.20',
    otherCost: '2.10',
    dishType: '热菜',
    dishSpec: '大份',
    costCardType: '标准成本卡',
    linkedDishCount: 7,
    operatedAt: '2026-04-06 09:21:00',
    lotteryInfo: 'CP-006 红烧排骨 HSPG',
    itemInfo: '排骨/冰糖/生抽',
  },
  {
    id: 7,
    costCardName: '麻婆豆腐标准卡',
    costCardCode: 'CBK-0007',
    costPriceTax: '9.60',
    itemCostTax: '8.70',
    otherCost: '0.90',
    dishType: '热菜',
    dishSpec: '标准份',
    costCardType: '标准成本卡',
    linkedDishCount: 10,
    operatedAt: '2026-04-05 17:42:00',
    lotteryInfo: 'CP-007 麻婆豆腐 MPDF',
    itemInfo: '豆腐/肉末/郫县豆瓣',
  },
  {
    id: 8,
    costCardName: '香菇鸡汤门店卡',
    costCardCode: 'CBK-0008',
    costPriceTax: '12.80',
    itemCostTax: '11.40',
    otherCost: '1.40',
    dishType: '汤羹',
    dishSpec: '大碗',
    costCardType: '门店成本卡',
    linkedDishCount: 5,
    operatedAt: '2026-04-05 11:15:00',
    lotteryInfo: 'CP-008 香菇鸡汤 XGJT',
    itemInfo: '鸡架/香菇/姜片',
  },
  {
    id: 9,
    costCardName: '凉拌黄瓜标准卡',
    costCardCode: 'CBK-0009',
    costPriceTax: '5.20',
    itemCostTax: '4.50',
    otherCost: '0.70',
    dishType: '凉菜',
    dishSpec: '标准份',
    costCardType: '标准成本卡',
    linkedDishCount: 8,
    operatedAt: '2026-04-04 16:05:00',
    lotteryInfo: 'CP-009 凉拌黄瓜 LBHG',
    itemInfo: '黄瓜/蒜末/香醋',
  },
  {
    id: 10,
    costCardName: '蒜香排条临时卡',
    costCardCode: 'CBK-0010',
    costPriceTax: '19.70',
    itemCostTax: '17.90',
    otherCost: '1.80',
    dishType: '热菜',
    dishSpec: '小份',
    costCardType: '临时成本卡',
    linkedDishCount: 2,
    operatedAt: '2026-04-04 10:34:00',
    lotteryInfo: 'CP-010 蒜香排条 SXPT',
    itemInfo: '猪排条/蒜粉/椒盐',
  },
  {
    id: 11,
    costCardName: '糖醋里脊标准卡',
    costCardCode: 'CBK-0011',
    costPriceTax: '15.90',
    itemCostTax: '14.30',
    otherCost: '1.60',
    dishType: '热菜',
    dishSpec: '标准份',
    costCardType: '标准成本卡',
    linkedDishCount: 11,
    operatedAt: '2026-04-03 19:28:00',
    lotteryInfo: 'CP-011 糖醋里脊 TCLJ',
    itemInfo: '里脊肉/番茄酱/白醋',
  },
  {
    id: 12,
    costCardName: '玉米排骨汤标准卡',
    costCardCode: 'CBK-0012',
    costPriceTax: '13.40',
    itemCostTax: '11.90',
    otherCost: '1.50',
    dishType: '汤羹',
    dishSpec: '中碗',
    costCardType: '标准成本卡',
    linkedDishCount: 6,
    operatedAt: '2026-04-03 14:56:00',
    lotteryInfo: 'CP-012 玉米排骨汤 YMPGT',
    itemInfo: '排骨/玉米/胡萝卜',
  },
];

const filteredData = computed(() => {
  const keyword = query.costCardInfo.trim().toLowerCase();
  const lotteryKeyword = query.lotteryInfo.trim().toLowerCase();
  const itemKeyword = query.itemInfo.trim().toLowerCase();

  return tableData.filter((row) => {
    const matchedCostCard = !keyword
      || row.costCardCode.toLowerCase().includes(keyword)
      || row.costCardName.toLowerCase().includes(keyword);
    const matchedLottery = !lotteryKeyword || row.lotteryInfo.toLowerCase().includes(lotteryKeyword);
    const matchedItem = !itemKeyword || row.itemInfo.toLowerCase().includes(itemKeyword);
    return matchedCostCard && matchedLottery && matchedItem;
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
  currentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  if (action === '新增成本卡') {
    router.push('/archive/2/1/create');
    return;
  }
  ElMessage.info(`已触发：${action}`);
};

const handleSelectionChange = (rows: CostCardRow[]) => {
  selectedCount.value = rows.length;
};

const handleCurrentChange = (page: number) => {
  currentPage.value = page;
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
};
</script>

<template>
  <section class="panel item-main-panel">
    <el-form :model="query" inline class="filter-bar compact-filter-bar">
      <el-form-item label="成本卡信息">
        <el-input
          v-model="query.costCardInfo"
          placeholder="成本卡名称/编码"
          clearable
          style="width: 150px"
        />
      </el-form-item>
      <el-form-item label="彩票信息">
        <el-input
          v-model="query.lotteryInfo"
          placeholder="根据编码/名称/助记码"
          clearable
          style="width: 160px"
        />
      </el-form-item>
      <el-form-item label="物品信息">
        <el-input
          v-model="query.itemInfo"
          placeholder="请输入物品信息"
          clearable
          style="width: 140px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="table-toolbar">
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
      :height="tableHeight"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" fixed="left" />
      <el-table-column type="index" label="序号" width="56" />
      <el-table-column prop="costCardCode" label="成本卡编码" width="110" show-overflow-tooltip />
      <el-table-column prop="costPriceTax" label="成本价（含税）" width="102" />
      <el-table-column prop="itemCostTax" label="物品成本（含税）" width="106" />
      <el-table-column prop="otherCost" label="其他成本" width="86" />
      <el-table-column prop="dishType" label="适用菜品类型" width="96" show-overflow-tooltip />
      <el-table-column prop="dishSpec" label="适用菜品规格" width="96" show-overflow-tooltip />
      <el-table-column prop="costCardType" label="成本卡类型" width="96" show-overflow-tooltip />
      <el-table-column prop="linkedDishCount" label="关联菜品数" width="86" />
      <el-table-column prop="operatedAt" label="操作时间" width="142" show-overflow-tooltip />
      <el-table-column label="操作" width="88" fixed="right">
        <template #default>
          <el-button text type="primary">编辑</el-button>
          <el-button text>详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-pagination">
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
</template>
