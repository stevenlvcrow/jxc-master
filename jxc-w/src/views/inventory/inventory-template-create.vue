<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { WarningFilled } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import FixedActionBreadcrumb from '@/components/FixedActionBreadcrumb.vue';

type ItemCatalog = {
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  stockUnit: string;
};

type TemplateRow = {
  id: number;
  itemQuery: string;
  itemCode: string;
  itemName: string;
  spec: string;
  category: string;
  stockUnit: string;
  remark: string;
};

const router = useRouter();
const activeNav = ref('basic');
const basicSectionRef = ref<HTMLElement | null>(null);
const contentSectionRef = ref<HTMLElement | null>(null);

const navs = [
  { key: 'basic', label: '基础信息' },
  { key: 'content', label: '模板内容' },
];

const form = reactive({
  templateName: '',
  enabled: true,
  remark: '',
});

const stockUnitOptions = ['个', '件', '斤', '袋', '瓶', '包', '箱', '克'];

const itemCatalog: ItemCatalog[] = [
  { itemCode: 'MDWP0001', itemName: '皮皮虾', spec: '', category: '海鲜', stockUnit: '克' },
  { itemCode: 'MDWP0002', itemName: '海天蚝油', spec: '', category: '原材料', stockUnit: '瓶' },
  { itemCode: 'MDWP0003', itemName: '咖啡豆', spec: '', category: '原材料', stockUnit: '袋' },
  { itemCode: 'MDWP0004', itemName: '吸管', spec: '', category: '原材料', stockUnit: '包' },
  { itemCode: 'MDWP0005', itemName: '杯托', spec: '', category: '原材料', stockUnit: '包' },
  { itemCode: 'MDWP0006', itemName: '测试', spec: '123', category: '海鲜', stockUnit: '件' },
  { itemCode: 'MDWP0007', itemName: '物品1', spec: '', category: '原材料', stockUnit: '斤' },
  { itemCode: 'MDWP0008', itemName: '测试固定标签', spec: '标准', category: '蔬菜', stockUnit: '斤' },
  { itemCode: 'MDWP0009', itemName: '玉米', spec: '', category: '蔬菜', stockUnit: '斤' },
  { itemCode: 'MDWP0010', itemName: '鸭血', spec: '', category: '海鲜', stockUnit: '克' },
  { itemCode: 'MDWP0011', itemName: '牛肉卷', spec: '500g', category: '原材料', stockUnit: '袋' },
  { itemCode: 'MDWP0012', itemName: '小白菜', spec: '', category: '蔬菜', stockUnit: '斤' },
];

const rowSeed = ref(2);
const rows = ref<TemplateRow[]>([
  {
    id: 1,
    itemQuery: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    stockUnit: '',
    remark: '',
  },
]);

const dialogVisible = ref(false);
const dialogQuery = reactive({
  keyword: '',
  tag: '',
  addedFilter: '全部' as '全部' | '已添加' | '未添加',
});
const categoryOptions = ['海鲜', '原材料', '菜品（系统创建）', '蔬菜'];
const selectedCategories = ref<string[]>([]);
const dialogCurrentPage = ref(1);
const dialogPageSize = ref(100);
const dialogSelectedCodes = ref<string[]>([]);

const existingCodeSet = computed(() => new Set(rows.value.map((row) => row.itemCode).filter(Boolean)));

const dialogFilteredRows = computed(() => {
  const keyword = dialogQuery.keyword.trim().toLowerCase();
  return itemCatalog.filter((item) => {
    const matchKeyword = !keyword
      || item.itemCode.toLowerCase().includes(keyword)
      || item.itemName.toLowerCase().includes(keyword)
      || item.category.toLowerCase().includes(keyword);
    const matchCategory = !selectedCategories.value.length || selectedCategories.value.includes(item.category);
    const added = existingCodeSet.value.has(item.itemCode);
    const matchAdded = dialogQuery.addedFilter === '全部'
      || (dialogQuery.addedFilter === '已添加' && added)
      || (dialogQuery.addedFilter === '未添加' && !added);
    return matchKeyword && matchCategory && matchAdded;
  });
});

const dialogPagedRows = computed(() => {
  const start = (dialogCurrentPage.value - 1) * dialogPageSize.value;
  return dialogFilteredRows.value.slice(start, start + dialogPageSize.value);
});

const selectedDialogItems = computed(() => itemCatalog.filter((item) => dialogSelectedCodes.value.includes(item.itemCode)));

const handleDialogSelectionChange = (list: ItemCatalog[]) => {
  dialogSelectedCodes.value = list.map((item) => item.itemCode);
};

const querySearchItem = (queryString: string, cb: (items: Array<{ value: string; raw: ItemCatalog }>) => void) => {
  const keyword = queryString.trim().toLowerCase();
  const result = itemCatalog
    .filter((item) => !keyword || item.itemCode.toLowerCase().includes(keyword) || item.itemName.toLowerCase().includes(keyword))
    .map((item) => ({
      value: `${item.itemCode} / ${item.itemName}`,
      raw: item,
    }));
  cb(result);
};

const applyItemToRow = (row: TemplateRow, matched?: ItemCatalog) => {
  if (!matched) {
    row.itemCode = '';
    row.itemName = '';
    row.spec = '';
    row.category = '';
    if (!row.stockUnit) {
      row.stockUnit = '';
    }
    return;
  }
  row.itemCode = matched.itemCode;
  row.itemName = matched.itemName;
  row.spec = matched.spec;
  row.category = matched.category;
  if (!row.stockUnit) {
    row.stockUnit = matched.stockUnit;
  }
};

const handleSelectItem = (row: TemplateRow, selected: { value: string; raw: ItemCatalog }) => {
  row.itemQuery = selected.value;
  applyItemToRow(row, selected.raw);
};

const handleBlurItemQuery = (row: TemplateRow) => {
  const keyword = row.itemQuery.trim().toLowerCase();
  if (!keyword) {
    applyItemToRow(row, undefined);
    return;
  }
  const matched = itemCatalog.find((item) => item.itemCode.toLowerCase() === keyword || item.itemName.toLowerCase() === keyword);
  applyItemToRow(row, matched);
};

const addRow = (index?: number) => {
  const target = typeof index === 'number' ? index + 1 : rows.value.length;
  rows.value.splice(target, 0, {
    id: rowSeed.value,
    itemQuery: '',
    itemCode: '',
    itemName: '',
    spec: '',
    category: '',
    stockUnit: '',
    remark: '',
  });
  rowSeed.value += 1;
};

const removeRow = (index: number) => {
  if (rows.value.length <= 1) {
    ElMessage.warning('至少保留一条物品');
    return;
  }
  rows.value.splice(index, 1);
};

const openDialog = () => {
  dialogSelectedCodes.value = [];
  dialogVisible.value = true;
};

const clearDialogSelection = () => {
  dialogSelectedCodes.value = [];
};

const confirmDialog = () => {
  if (!dialogSelectedCodes.value.length) {
    ElMessage.warning('请至少选择一个物品');
    return;
  }
  const appendRows = itemCatalog
    .filter((item) => dialogSelectedCodes.value.includes(item.itemCode))
    .filter((item) => !existingCodeSet.value.has(item.itemCode))
    .map((item) => ({
      id: rowSeed.value++,
      itemQuery: `${item.itemCode} / ${item.itemName}`,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: item.spec,
      category: item.category,
      stockUnit: item.stockUnit,
      remark: '',
    }));
  if (!appendRows.length) {
    ElMessage.info('所选物品已在模板中');
    dialogVisible.value = false;
    return;
  }
  rows.value.push(...appendRows);
  ElMessage.success(`已添加 ${appendRows.length} 条物品`);
  dialogVisible.value = false;
};

const handleImport = () => {
  const imported = itemCatalog
    .slice(0, 3)
    .filter((item) => !existingCodeSet.value.has(item.itemCode))
    .map((item) => ({
      id: rowSeed.value++,
      itemQuery: `${item.itemCode} / ${item.itemName}`,
      itemCode: item.itemCode,
      itemName: item.itemName,
      spec: item.spec,
      category: item.category,
      stockUnit: item.stockUnit,
      remark: '',
    }));
  rows.value.push(...imported);
  ElMessage.success(`导入完成，新增 ${imported.length} 条`);
};

const scrollToSection = (key: string) => {
  activeNav.value = key;
  const target = key === 'basic' ? basicSectionRef.value : contentSectionRef.value;
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' });
};

const handleBack = () => {
  router.push('/inventory/5/1');
};

const handleSaveDraft = () => {
  ElMessage.success('库存模板草稿已保存');
};

const handleSave = () => {
  if (!form.templateName.trim()) {
    ElMessage.warning('请输入模板名称');
    return;
  }
  if (!rows.value.some((row) => row.itemCode)) {
    ElMessage.warning('请添加至少一个物品');
    return;
  }
  ElMessage.success('库存模板保存成功');
  router.push('/inventory/5/1');
};
</script>

<template>
  <div class="item-create-page">
    <FixedActionBreadcrumb
      :navs="navs"
      :active-key="activeNav"
      @back="handleBack"
      @save-draft="handleSaveDraft"
      @save="handleSave"
      @navigate="scrollToSection"
    />

    <section class="panel form-panel">
      <div class="inventory-template-tip">
        <el-icon><WarningFilled /></el-icon>
        <span>库存模板用于填写采购入库单、盘点单时快速引用，提高录入单据速度</span>
      </div>

      <div ref="basicSectionRef" class="form-section-block">
        <h3 class="form-section-title">基础信息</h3>
        <el-form label-width="92px" class="item-create-form inventory-template-create-form">
          <div class="item-form-grid inventory-template-basic-grid">
            <el-form-item label="模板名称" required>
              <el-input v-model="form.templateName" placeholder="请输入模板名称" maxlength="30" />
            </el-form-item>
            <el-form-item label="模板状态">
              <el-switch v-model="form.enabled" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" placeholder="在此填写备注信息..." maxlength="100" />
            </el-form-item>
          </div>
        </el-form>
      </div>

      <div ref="contentSectionRef" class="form-section-block">
        <h3 class="form-section-title">模板内容</h3>
        <div class="table-toolbar">
          <el-button @click="openDialog">添加物品</el-button>
          <el-button @click="handleImport">导入物品</el-button>
        </div>

        <el-table :data="rows" border stripe class="erp-table inventory-template-edit-table" :fit="false">
          <el-table-column type="index" label="序号" width="56" fixed="left" />
          <el-table-column label="操作" width="86" fixed="left">
            <template #default="{ $index }">
              <el-button text type="primary" @click="addRow($index)">+</el-button>
              <el-button text @click="removeRow($index)">-</el-button>
            </template>
          </el-table-column>
          <el-table-column label="物品编码" min-width="220">
            <template #default="{ row }">
              <el-autocomplete
                v-model="row.itemQuery"
                :fetch-suggestions="querySearchItem"
                placeholder="根据编码名称检索"
                clearable
                @select="handleSelectItem(row, $event)"
                @blur="handleBlurItemQuery(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="物品名称" min-width="140">
            <template #default="{ row }">{{ row.itemName || '-' }}</template>
          </el-table-column>
          <el-table-column label="规格型号" min-width="110">
            <template #default="{ row }">{{ row.spec || '-' }}</template>
          </el-table-column>
          <el-table-column label="物品类别" min-width="110">
            <template #default="{ row }">{{ row.category || '-' }}</template>
          </el-table-column>
          <el-table-column label="库存单位" min-width="120">
            <template #default="{ row }">
              <el-select v-model="row.stockUnit" placeholder="请选择单位">
                <el-option v-for="option in stockUnitOptions" :key="option" :label="option" :value="option" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="180">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="最多30个字" maxlength="30" />
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      title="选择物品"
      width="1240px"
      class="standard-form-dialog inventory-template-item-dialog"
      destroy-on-close
    >
      <div class="inventory-dialog-toolbar">
        <el-form inline class="compact-filter-bar" :model="dialogQuery">
          <el-form-item label="物品">
            <el-input v-model="dialogQuery.keyword" placeholder="物品名称/编码/助记码/..." style="width: 220px" />
          </el-form-item>
          <el-form-item label="物品标签">
            <el-select v-model="dialogQuery.tag" placeholder="请选择" clearable style="width: 200px">
              <el-option label="海鲜" value="海鲜" />
              <el-option label="原材料" value="原材料" />
              <el-option label="蔬菜" value="蔬菜" />
            </el-select>
          </el-form-item>
          <el-form-item label="是否已添加">
            <el-radio-group v-model="dialogQuery.addedFilter">
              <el-radio label="全部">全部</el-radio>
              <el-radio label="已添加">已添加</el-radio>
              <el-radio label="未添加">未添加</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item>
            <el-button type="primary">查询</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div class="inventory-dialog-content">
        <aside class="inventory-dialog-left">
          <el-checkbox-group v-model="selectedCategories">
            <div v-for="item in categoryOptions" :key="item" class="inventory-category-item">
              <el-checkbox :label="item">{{ item }}</el-checkbox>
            </div>
          </el-checkbox-group>
        </aside>

        <section class="inventory-dialog-main">
          <el-table
            :data="dialogPagedRows"
            border
            stripe
            height="430"
            class="erp-table"
            @selection-change="handleDialogSelectionChange"
          >
            <el-table-column type="selection" width="46" fixed="left" />
            <el-table-column type="index" label="序号" width="56" fixed="left" />
            <el-table-column prop="itemCode" label="物品编码" min-width="130" />
            <el-table-column prop="itemName" label="物品名称" min-width="130" />
            <el-table-column prop="spec" label="规格型号" min-width="120" />
            <el-table-column prop="category" label="物品类别" min-width="120" />
            <el-table-column prop="stockUnit" label="库存单位" min-width="100" />
          </el-table>
          <div class="table-pagination">
            <div class="table-pagination-meta">共{{ dialogFilteredRows.length }}条信息</div>
            <el-pagination
              :current-page="dialogCurrentPage"
              :page-size="dialogPageSize"
              :page-sizes="[20, 50, 100]"
              :total="dialogFilteredRows.length"
              background
              small
              layout="prev, pager, next, sizes"
              @current-change="(p:number) => { dialogCurrentPage = p; }"
              @size-change="(s:number) => { dialogPageSize = s; dialogCurrentPage = 1; }"
            />
          </div>
        </section>
      </div>

      <div class="inventory-dialog-selected">
        <div class="inventory-selected-head">
          <span>已选择（{{ selectedDialogItems.length }}）：</span>
          <el-button text type="danger" @click="clearDialogSelection">清空</el-button>
        </div>
        <div class="inventory-selected-tags">
          <el-tag v-for="item in selectedDialogItems" :key="item.itemCode" size="small">
            {{ item.itemCode }} {{ item.itemName }}
          </el-tag>
        </div>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDialog">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.inventory-template-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding: 6px 10px;
  border: 1px solid #f6d365;
  border-radius: 4px;
  background: #fffbe8;
  color: #7c5914;
  font-size: 11px;
}

.inventory-template-basic-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.inventory-template-edit-table :deep(.el-input__wrapper),
.inventory-template-edit-table :deep(.el-select__wrapper) {
  min-height: 22px;
}

.inventory-dialog-toolbar {
  margin-bottom: 8px;
}

.inventory-dialog-content {
  display: grid;
  grid-template-columns: 210px minmax(0, 1fr);
  border: 1px solid #d8deea;
}

.inventory-dialog-left {
  padding: 8px;
  border-right: 1px solid #d8deea;
  background: #f8fafc;
}

.inventory-category-item {
  min-height: 32px;
  display: flex;
  align-items: center;
}

.inventory-dialog-main {
  min-width: 0;
}

.inventory-dialog-selected {
  margin-top: 8px;
  border: 1px solid #d8deea;
  border-radius: 4px;
  padding: 8px;
}

.inventory-selected-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.inventory-selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-height: 24px;
}

@media (max-width: 1200px) {
  .inventory-template-basic-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .inventory-dialog-content {
    grid-template-columns: 1fr;
  }

  .inventory-dialog-left {
    border-right: 0;
    border-bottom: 1px solid #d8deea;
  }
}

@media (max-width: 960px) {
  .inventory-template-basic-grid {
    grid-template-columns: 1fr;
  }
}
</style>
