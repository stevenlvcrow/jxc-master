<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { Plus, RefreshRight, Search } from '@element-plus/icons-vue';
import { useRouter } from 'vue-router';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import { useSessionStore } from '@/stores/session';
import { useDictionaryOptions } from '@/composables/useDictionaryOptions';

type InventoryTemplateRow = {
  id: number;
  templateCode: string;
  templateName: string;
  itemCount: number;
  orgName: string;
  status: string;
  createdAt: string;
  updatedAt: string;
};

const router = useRouter();
const sessionStore = useSessionStore();
const { optionsOf } = useDictionaryOptions(['item.status']);
const statusOptions = optionsOf('item.status');
const rows = ref<InventoryTemplateRow[]>([]);
const currentPage = ref(1);
const pageSize = ref(10);

const orgOptions = computed(() => sessionStore.flatOrgs.map((org) => ({
  value: org.name,
  label: org.name,
})));

const query = reactive({
  keyword: '',
  orgName: '',
  status: '',
  itemKeyword: '',
});

const handleSearch = () => {
  currentPage.value = 1;
};

const handleReset = () => {
  query.keyword = '';
  query.orgName = '';
  query.status = '';
  query.itemKeyword = '';
  currentPage.value = 1;
};

const handleAdd = () => {
  router.push('/inventory/inventory-templates/create');
};
</script>

<template>
  <section class="panel item-main-panel">
    <CommonQuerySection :model="query">
      <el-form-item label="模板编号/名称">
        <el-input v-model="query.keyword" placeholder="请输入模板编号/名称" clearable style="width: 200px" />
      </el-form-item>
      <el-form-item label="创建机构">
        <el-select v-model="query.orgName" clearable filterable style="width: 180px">
          <el-option v-for="option in orgOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable style="width: 120px">
          <el-option v-for="item in statusOptions" :key="item.itemCode" :label="item.itemLabel" :value="item.itemCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="物品">
        <el-input v-model="query.itemKeyword" placeholder="请输入物品编码/名称" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </CommonQuerySection>

    <div class="table-toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增
      </el-button>
    </div>

    <el-table
      :data="rows"
      border
      stripe
      class="erp-table"
      :fit="false"
      :height="360"
      empty-text="暂无库存模板数据"
    >
      <el-table-column type="index" label="序号" width="56" fixed="left" />
      <el-table-column prop="templateCode" label="模板编号" min-width="140" show-overflow-tooltip />
      <el-table-column prop="templateName" label="模板名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="物品（项）" min-width="100" show-overflow-tooltip />
      <el-table-column prop="orgName" label="创建机构" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" min-width="90" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" show-overflow-tooltip />
      <el-table-column prop="updatedAt" label="最后修改时间" min-width="170" show-overflow-tooltip />
    </el-table>

    <div class="table-pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="rows.length"
        background
        small
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="(page: number) => { currentPage = page; }"
        @size-change="(size: number) => { pageSize = size; currentPage = 1; }"
      />
    </div>
  </section>
</template>
