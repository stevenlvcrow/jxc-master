<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection, { type ToolbarButton } from '@/components/CommonToolbarSection.vue';
import PageTabsLayout, { type PageTabItem } from '@/components/PageTabsLayout.vue';

type DishType = '堂食' | '加料' | '餐盒';
type DeleteStatus = '未删除' | '已删除';
type CategoryWarehouseRow = {
  id: number;
  dishType: DishType;
  categoryName: string;
  remark: string;
  warehouses: string[];
};
type DishWarehouseRow = {
  id: number;
  dishCode: string;
  dishName: string;
  categoryName: string;
  spec: string;
  warehouses: string[];
  dishType: DishType;
  linkedAt: string;
  deletedAt: string;
  deletedStatus: DeleteStatus;
};
type TreeNode = {
  value: string;
  label: string;
  children?: TreeNode[];
};

const tabs: PageTabItem[] = [
  { key: 'category', label: '菜品分类关联仓库' },
  { key: 'dish', label: '菜品关联仓库' },
];
const toolbarButtons: ToolbarButton[] = [
  { key: '批量关联仓库', label: '批量关联仓库', type: 'primary' },
  { key: '批量取消关联仓库', label: '批量取消关联仓库' },
];

const activeTab = ref('category');
const dishTypeOptions: DishType[] = ['堂食', '加料', '餐盒'];
const manageRangeOptions = ['全部'];
const deleteStatusOptions: DeleteStatus[] = ['未删除', '已删除'];
const categoryOptions = [
  '堂食',
  '必点菜',
  '推出新品',
  '下饭菜',
  '家常菜',
  '汤类',
  '蔬菜类',
  '酒水饮料',
  '其他',
  '美团团购套餐',
  '店内套餐',
  '美团外卖',
  '饿了么外卖',
  '京东秒送菜品',
  '加料',
  '餐盒',
];

const warehouseTree: TreeNode[] = [
  {
    value: 'all-warehouse',
    label: '全部仓库',
    children: [
      {
        value: 'north-warehouse',
        label: '北区仓',
        children: [
          { value: 'north-cold', label: '北区冷藏仓' },
          { value: 'north-dry', label: '北区干货仓' },
        ],
      },
      {
        value: 'south-warehouse',
        label: '南区仓',
        children: [
          { value: 'south-fresh', label: '南区生鲜仓' },
          { value: 'south-pack', label: '南区包材仓' },
        ],
      },
    ],
  },
];

const dishTreeData: TreeNode[] = [
  {
    value: '全部',
    label: '全部',
    children: [
      { value: '堂食', label: '堂食' },
      { value: '必点菜', label: '必点菜' },
      { value: '推出新品', label: '推出新品' },
      { value: '下饭菜', label: '下饭菜' },
      { value: '家常菜', label: '家常菜' },
      { value: '汤类', label: '汤类' },
      { value: '蔬菜类', label: '蔬菜类' },
      { value: '酒水饮料', label: '酒水饮料' },
      { value: '其他', label: '其他' },
      { value: '美团团购套餐', label: '美团团购套餐' },
      { value: '店内套餐', label: '店内套餐' },
      { value: '美团外卖', label: '美团外卖' },
      { value: '饿了么外卖', label: '饿了么外卖' },
      { value: '京东秒送菜品', label: '京东秒送菜品' },
      { value: '加料', label: '加料' },
      { value: '餐盒', label: '餐盒' },
    ],
  },
];

const categoryWarehouseRows: CategoryWarehouseRow[] = [
  { id: 1, dishType: '堂食', categoryName: '必点菜', remark: '热销菜默认关联冷藏仓', warehouses: ['北区冷藏仓'] },
  { id: 2, dishType: '堂食', categoryName: '推出新品', remark: '新品单独补货', warehouses: ['北区干货仓', '南区生鲜仓'] },
  { id: 3, dishType: '堂食', categoryName: '下饭菜', remark: '', warehouses: ['南区生鲜仓'] },
  { id: 4, dishType: '堂食', categoryName: '家常菜', remark: '', warehouses: ['北区冷藏仓', '南区生鲜仓'] },
  { id: 5, dishType: '堂食', categoryName: '汤类', remark: '', warehouses: ['南区生鲜仓'] },
  { id: 6, dishType: '加料', categoryName: '加料', remark: '统一走包材仓', warehouses: ['南区包材仓'] },
  { id: 7, dishType: '餐盒', categoryName: '餐盒', remark: '独立包材仓管理', warehouses: ['南区包材仓'] },
];

const dishWarehouseRows: DishWarehouseRow[] = [
  { id: 1, dishCode: 'D0001', dishName: '宫保鸡丁', categoryName: '必点菜', spec: '标准份', warehouses: ['北区冷藏仓'], dishType: '堂食', linkedAt: '2026-04-13 10:10:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 2, dishCode: 'D0002', dishName: '鱼香肉丝', categoryName: '下饭菜', spec: '标准份', warehouses: ['北区冷藏仓', '南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-12 16:20:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 3, dishCode: 'D0003', dishName: '番茄牛腩', categoryName: '家常菜', spec: '大份', warehouses: ['南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-11 09:42:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 4, dishCode: 'D0004', dishName: '老坛酸菜', categoryName: '推出新品', spec: '标准份', warehouses: ['北区干货仓'], dishType: '堂食', linkedAt: '2026-04-10 14:05:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 5, dishCode: 'D0005', dishName: '脆爽萝卜', categoryName: '加料', spec: '加量', warehouses: ['南区包材仓'], dishType: '加料', linkedAt: '2026-04-09 12:18:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 6, dishCode: 'D0006', dishName: '外卖餐盒', categoryName: '餐盒', spec: '1000ml', warehouses: ['南区包材仓'], dishType: '餐盒', linkedAt: '2026-04-08 18:30:00', deletedAt: '-', deletedStatus: '未删除' },
  { id: 7, dishCode: 'D0007', dishName: '酸梅汤', categoryName: '酒水饮料', spec: '杯', warehouses: ['北区干货仓'], dishType: '堂食', linkedAt: '2026-04-08 10:22:00', deletedAt: '2026-04-12 09:00:00', deletedStatus: '已删除' },
  { id: 8, dishCode: 'D0008', dishName: '鲜蔬拼盘', categoryName: '蔬菜类', spec: '标准份', warehouses: ['南区生鲜仓'], dishType: '堂食', linkedAt: '2026-04-07 15:40:00', deletedAt: '-', deletedStatus: '未删除' },
];

const categoryQuery = reactive({
  dishType: '' as DishType | '',
  categoryNames: [] as string[],
  warehouses: [] as string[],
});
const dishQuery = reactive({
  manageRange: '全部',
  dishInfo: '',
  warehouses: [] as string[],
  deleteStatus: '未删除' as DeleteStatus,
});

const categorySelectedIds = ref<number[]>([]);
const dishSelectedIds = ref<number[]>([]);
const categoryCurrentPage = ref(1);
const dishCurrentPage = ref(1);
const categoryPageSize = ref(10);
const dishPageSize = ref(10);
const selectedTreeNode = ref('全部');

const warehouseLabelMap = computed(() => {
  const map = new Map<string, string>();
  const walk = (nodes: TreeNode[]) => {
    nodes.forEach((node) => {
      map.set(node.value, node.label);
      if (node.children?.length) {
        walk(node.children);
      }
    });
  };
  walk(warehouseTree);
  return map;
});

const selectedWarehouseLabels = (values: string[]) => values
  .map((value) => warehouseLabelMap.value.get(value) ?? value)
  .filter((label) => label !== '全部仓库');

const categoryFilteredRows = computed(() => {
  const warehouseLabels = selectedWarehouseLabels(categoryQuery.warehouses);
  return categoryWarehouseRows.filter((row) => {
    const matchedDishType = !categoryQuery.dishType || row.dishType === categoryQuery.dishType;
    const matchedCategory = !categoryQuery.categoryNames.length || categoryQuery.categoryNames.includes(row.categoryName);
    const matchedWarehouse = !warehouseLabels.length || warehouseLabels.some((label) => row.warehouses.includes(label));
    return matchedDishType && matchedCategory && matchedWarehouse;
  });
});

const dishFilteredRows = computed(() => {
  const keyword = dishQuery.dishInfo.trim().toLowerCase();
  const warehouseLabels = selectedWarehouseLabels(dishQuery.warehouses);
  return dishWarehouseRows.filter((row) => {
    const matchedManageRange = dishQuery.manageRange === '全部';
    const matchedKeyword = !keyword
      || row.dishCode.toLowerCase().includes(keyword)
      || row.dishName.toLowerCase().includes(keyword);
    const matchedWarehouse = !warehouseLabels.length || warehouseLabels.some((label) => row.warehouses.includes(label));
    const matchedDeleteStatus = row.deletedStatus === dishQuery.deleteStatus;
    const matchedTree = selectedTreeNode.value === '全部' || row.categoryName === selectedTreeNode.value || row.dishType === selectedTreeNode.value;
    return matchedManageRange && matchedKeyword && matchedWarehouse && matchedDeleteStatus && matchedTree;
  });
});

const categoryPagedRows = computed(() => {
  const start = (categoryCurrentPage.value - 1) * categoryPageSize.value;
  return categoryFilteredRows.value.slice(start, start + categoryPageSize.value);
});

const dishPagedRows = computed(() => {
  const start = (dishCurrentPage.value - 1) * dishPageSize.value;
  return dishFilteredRows.value.slice(start, start + dishPageSize.value);
});

const handleCategorySearch = () => {
  categoryCurrentPage.value = 1;
};

const handleCategoryReset = () => {
  categoryQuery.dishType = '';
  categoryQuery.categoryNames = [];
  categoryQuery.warehouses = [];
  categoryCurrentPage.value = 1;
};

const handleDishSearch = () => {
  dishCurrentPage.value = 1;
};

const handleDishReset = () => {
  dishQuery.manageRange = '全部';
  dishQuery.dishInfo = '';
  dishQuery.warehouses = [];
  dishQuery.deleteStatus = '未删除';
  selectedTreeNode.value = '全部';
  dishCurrentPage.value = 1;
};

const handleToolbarAction = (action: string) => {
  ElMessage.info(`${action}功能待接入`);
};

const handleCategorySelectionChange = (rows: CategoryWarehouseRow[]) => {
  categorySelectedIds.value = rows.map((row) => row.id);
};

const handleDishSelectionChange = (rows: DishWarehouseRow[]) => {
  dishSelectedIds.value = rows.map((row) => row.id);
};

const handleCategoryPageChange = (page: number) => {
  categoryCurrentPage.value = page;
};

const handleCategoryPageSizeChange = (size: number) => {
  categoryPageSize.value = size;
  categoryCurrentPage.value = 1;
};

const handleDishPageChange = (page: number) => {
  dishCurrentPage.value = page;
};

const handleDishPageSizeChange = (size: number) => {
  dishPageSize.value = size;
  dishCurrentPage.value = 1;
};

const handleDishTreeSelect = (node: TreeNode) => {
  selectedTreeNode.value = node.label;
  dishCurrentPage.value = 1;
};
</script>

<template>
  <section class="dish-warehouse-page panel item-main-panel">
    <PageTabsLayout
      v-model:active-tab="activeTab"
      :tabs="tabs"
      body-class="dish-warehouse-page__body"
    >
      <template v-if="activeTab === 'category'">
        <CommonQuerySection :model="categoryQuery">
          <el-form-item label="菜品类型">
            <el-select v-model="categoryQuery.dishType" placeholder="请选择" clearable style="width: 160px">
              <el-option
                v-for="option in dishTypeOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="分类名称">
            <el-select
              v-model="categoryQuery.categoryNames"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择"
              style="width: 220px"
            >
              <el-option
                v-for="option in categoryOptions"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="仓库">
            <el-tree-select
              v-model="categoryQuery.warehouses"
              :data="warehouseTree"
              :props="{ label: 'label', value: 'value', children: 'children' }"
              multiple
              show-checkbox
              check-strictly
              default-expand-all
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择"
              style="width: 260px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleCategorySearch">查询</el-button>
            <el-button @click="handleCategoryReset">重置</el-button>
          </el-form-item>
        </CommonQuerySection>

        <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

        <el-table
          :data="categoryPagedRows"
          border
          stripe
          class="erp-table"
          :fit="false"
          :empty-text="'当前机构暂无数据'"
          @selection-change="handleCategorySelectionChange"
        >
          <el-table-column type="selection" width="44" />
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="dishType" label="菜品类型" min-width="120" />
          <el-table-column prop="categoryName" label="分类名称" min-width="180" show-overflow-tooltip />
          <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
          <el-table-column label="关联仓库" min-width="260" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.warehouses.join('，') || '-' }}
            </template>
          </el-table-column>
        </el-table>

        <div class="table-pagination">
          <div class="table-pagination-meta">已选 {{ categorySelectedIds.length }} 条</div>
          <el-pagination
            :current-page="categoryCurrentPage"
            :page-size="categoryPageSize"
            :page-sizes="[10, 20, 50]"
            :total="categoryFilteredRows.length"
            background
            small
            layout="total, sizes, prev, pager, next, jumper"
            @current-change="handleCategoryPageChange"
            @size-change="handleCategoryPageSizeChange"
          />
        </div>
      </template>

      <template v-else>
        <div class="dish-warehouse-page__content">
          <aside class="dish-warehouse-page__tree panel category-panel">
            <el-tree
              :data="dishTreeData"
              node-key="value"
              default-expand-all
              class="category-tree"
              :expand-on-click-node="false"
              @node-click="handleDishTreeSelect"
            />
          </aside>

          <div class="dish-warehouse-page__main">
            <CommonQuerySection :model="dishQuery">
              <el-form-item label="管理菜品范围">
                <el-select v-model="dishQuery.manageRange" style="width: 140px">
                  <el-option
                    v-for="option in manageRangeOptions"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="菜品信息">
                <el-input
                  v-model="dishQuery.dishInfo"
                  placeholder="请输入菜品编码或名称"
                  clearable
                  style="width: 220px"
                />
              </el-form-item>
              <el-form-item label="仓库">
                <el-tree-select
                  v-model="dishQuery.warehouses"
                  :data="warehouseTree"
                  :props="{ label: 'label', value: 'value', children: 'children' }"
                  multiple
                  show-checkbox
                  check-strictly
                  default-expand-all
                  collapse-tags
                  collapse-tags-tooltip
                  placeholder="请选择"
                  style="width: 260px"
                />
              </el-form-item>
              <el-form-item label="菜品是否删除">
                <el-select v-model="dishQuery.deleteStatus" style="width: 140px">
                  <el-option
                    v-for="option in deleteStatusOptions"
                    :key="option"
                    :label="option"
                    :value="option"
                  />
                </el-select>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleDishSearch">查询</el-button>
                <el-button @click="handleDishReset">重置</el-button>
              </el-form-item>
            </CommonQuerySection>

            <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

            <el-table
              :data="dishPagedRows"
              border
              stripe
              class="erp-table"
              :fit="false"
              :empty-text="'当前机构暂无数据'"
              @selection-change="handleDishSelectionChange"
            >
              <el-table-column type="selection" width="44" fixed="left" />
              <el-table-column type="index" label="序号" width="60" fixed="left" />
              <el-table-column prop="dishCode" label="菜品编码" min-width="120" show-overflow-tooltip />
              <el-table-column prop="dishName" label="菜品名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="categoryName" label="菜品分类" min-width="140" show-overflow-tooltip />
              <el-table-column prop="spec" label="规格" min-width="110" show-overflow-tooltip />
              <el-table-column label="关联仓库" min-width="240" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.warehouses.join('，') || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="dishType" label="菜品类型" min-width="100" />
              <el-table-column prop="linkedAt" label="最新关联时间" min-width="170" show-overflow-tooltip />
              <el-table-column prop="deletedAt" label="删除时间" min-width="170" show-overflow-tooltip />
            </el-table>

            <div class="table-pagination">
              <div class="table-pagination-meta">已选 {{ dishSelectedIds.length }} 条</div>
              <el-pagination
                :current-page="dishCurrentPage"
                :page-size="dishPageSize"
                :page-sizes="[10, 20, 50]"
                :total="dishFilteredRows.length"
                background
                small
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handleDishPageChange"
                @size-change="handleDishPageSizeChange"
              />
            </div>
          </div>
        </div>
      </template>
    </PageTabsLayout>
  </section>
</template>

<style scoped lang="scss">
.dish-warehouse-page {
  padding: 8px;
}

.dish-warehouse-page__query {
  margin-bottom: 8px;
}

.dish-warehouse-page__body {
  padding-top: 10px;
}

.dish-warehouse-page__content {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
}

.dish-warehouse-page__tree {
  min-width: 0;
}

.dish-warehouse-page__main {
  min-width: 0;
}

@media (max-width: 1200px) {
  .dish-warehouse-page__content {
    grid-template-columns: 1fr;
  }
}
</style>
