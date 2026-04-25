<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { Delete, Edit, Plus, SortDown, SortUp } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import CommonQuerySection from '@/components/CommonQuerySection.vue';
import CommonToolbarSection from '@/components/CommonToolbarSection.vue';
import {
  createMenuMaintenanceApi,
  deleteMenuMaintenanceApi,
  fetchMenuMaintenanceApi,
  sortMenuMaintenanceApi,
  updateMenuMaintenanceApi,
  type MenuAdminItem,
  type MenuMaintenancePayload,
} from '@/api/modules/system-admin';
import { syncRuntimeMenuRoutes } from '@/router';
import { useMenuStore } from '@/stores/menu';

type MenuTreeItem = MenuAdminItem & {
  children?: MenuTreeItem[];
};

const menuTypeOptions = [
  { label: '目录', value: 'DIRECTORY' },
  { label: '菜单', value: 'MENU' },
  { label: '按钮', value: 'BUTTON' },
  { label: '接口', value: 'API' },
] as const;
const statusOptions = [
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
] as const;
const toolbarButtons = [{ key: 'create', label: '新增菜单', type: 'primary' }] as const;

const loading = ref(false);
const submitting = ref(false);
const sorting = ref(false);
const visibilityChangingId = ref<number | null>(null);
const dialogVisible = ref(false);
const editingId = ref<number | null>(null);
const menus = ref<MenuAdminItem[]>([]);
const menuStore = useMenuStore();
const queryForm = reactive({
  keyword: '',
  menuType: '',
});
const form = reactive<MenuMaintenancePayload>({
  menuCode: '',
  menuName: '',
  parentId: null,
  menuType: 'MENU',
  routePath: '',
  componentKey: '',
  permissionCode: '',
  icon: '',
  sortNo: 10,
  visible: true,
  status: 'ENABLED',
});

const filteredMenus = computed(() => {
  const keyword = queryForm.keyword.trim().toLowerCase();
  const menuType = queryForm.menuType;
  if (!keyword && !menuType) {
    return menus.value;
  }
  const byId = new Map(menus.value.map((item) => [item.id, item]));
  const matchedIds = new Set<number>();
  const markWithParents = (item: MenuAdminItem) => {
    matchedIds.add(item.id);
    let parentId = item.parentId;
    while (parentId) {
      const parent = byId.get(parentId);
      if (!parent || matchedIds.has(parent.id)) {
        break;
      }
      matchedIds.add(parent.id);
      parentId = parent.parentId;
    }
  };

  menus.value.forEach((item) => {
    const matchKeyword = !keyword
      || item.menuCode.toLowerCase().includes(keyword)
      || item.menuName.toLowerCase().includes(keyword)
      || String(item.routePath ?? '').toLowerCase().includes(keyword)
      || String(item.permissionCode ?? '').toLowerCase().includes(keyword);
    const matchType = !menuType || item.menuType === menuType;
    if (matchKeyword && matchType) {
      markWithParents(item);
    }
  });
  return menus.value.filter((item) => matchedIds.has(item.id));
});

const treeData = computed<MenuTreeItem[]>(() => {
  const nodes = new Map<number, MenuTreeItem>();
  filteredMenus.value.forEach((item) => {
    nodes.set(item.id, { ...item, children: [] });
  });
  const roots: MenuTreeItem[] = [];
  nodes.forEach((node) => {
    if (node.parentId && nodes.has(node.parentId)) {
      nodes.get(node.parentId)?.children?.push(node);
      return;
    }
    roots.push(node);
  });
  const sortNodes = (items: MenuTreeItem[]) => {
    items.sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0) || a.id - b.id);
    items.forEach((item) => sortNodes(item.children ?? []));
  };
  sortNodes(roots);
  return roots;
});

const parentOptions = computed(() => menus.value
  .filter((item) => item.menuType === 'DIRECTORY' || item.menuType === 'MENU')
  .filter((item) => item.id !== editingId.value)
  .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0) || a.id - b.id));

const siblingRows = (row: MenuAdminItem) => menus.value
  .filter((item) => item.parentId === row.parentId)
  .sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0) || a.id - b.id);

const resetForm = () => {
  editingId.value = null;
  form.menuCode = '';
  form.menuName = '';
  form.parentId = null;
  form.menuType = 'MENU';
  form.routePath = '';
  form.componentKey = '';
  form.permissionCode = '';
  form.icon = '';
  form.sortNo = 10;
  form.visible = true;
  form.status = 'ENABLED';
};

const resetQuery = () => {
  queryForm.keyword = '';
  queryForm.menuType = '';
};

const loadMenus = async () => {
  loading.value = true;
  try {
    menus.value = await fetchMenuMaintenanceApi();
  } finally {
    loading.value = false;
  }
};

const reloadRuntimeMenus = async () => {
  await menuStore.loadMenus('platform');
  syncRuntimeMenuRoutes(menuStore.menuItems);
};

const refreshMenus = async () => {
  await Promise.all([loadMenus(), reloadRuntimeMenus()]);
};

const openCreate = (parent?: MenuAdminItem) => {
  resetForm();
  if (parent) {
    form.parentId = parent.id;
    form.sortNo = siblingRows({ ...parent, parentId: parent.id }).length * 10 + 10;
  }
  dialogVisible.value = true;
};

const openEdit = (row: MenuAdminItem) => {
  editingId.value = row.id;
  form.menuCode = row.menuCode;
  form.menuName = row.menuName;
  form.parentId = row.parentId;
  form.menuType = row.menuType;
  form.routePath = row.routePath ?? '';
  form.componentKey = row.componentKey ?? '';
  form.permissionCode = row.permissionCode ?? '';
  form.icon = row.icon ?? '';
  form.sortNo = row.sortNo ?? 10;
  form.visible = row.visible !== false;
  form.status = row.status;
  dialogVisible.value = true;
};

const handleToolbarAction = (key: string) => {
  if (key === 'create') {
    openCreate();
  }
};

const buildPayload = (): MenuMaintenancePayload => ({
  menuCode: form.menuCode.trim(),
  menuName: form.menuName.trim(),
  parentId: form.parentId,
  menuType: form.menuType,
  routePath: form.routePath?.trim() || null,
  componentKey: form.componentKey?.trim() || null,
  permissionCode: form.permissionCode?.trim() || null,
  icon: form.icon?.trim() || null,
  sortNo: Number(form.sortNo),
  visible: Boolean(form.visible),
  status: form.status,
});

const buildRowPayload = (row: MenuAdminItem, visible = row.visible !== false): MenuMaintenancePayload => ({
  menuCode: row.menuCode,
  menuName: row.menuName,
  parentId: row.parentId,
  menuType: row.menuType,
  routePath: row.routePath,
  componentKey: row.componentKey,
  permissionCode: row.permissionCode,
  icon: row.icon,
  sortNo: Number(row.sortNo ?? 0),
  visible,
  status: row.status,
});

const handleSave = async () => {
  const payload = buildPayload();
  if (!payload.menuCode) {
    ElMessage.warning('请填写菜单编码');
    return;
  }
  if (!payload.menuName) {
    ElMessage.warning('请填写菜单名称');
    return;
  }
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateMenuMaintenanceApi(editingId.value, payload);
      ElMessage.success('菜单更新成功');
    } else {
      await createMenuMaintenanceApi(payload);
      ElMessage.success('菜单创建成功');
    }
    dialogVisible.value = false;
    await refreshMenus();
  } finally {
    submitting.value = false;
  }
};

const handleToggleVisible = async (row: MenuAdminItem) => {
  const nextVisible = row.visible === false;
  const actionText = nextVisible ? '显示' : '隐藏';
  visibilityChangingId.value = row.id;
  try {
    await updateMenuMaintenanceApi(row.id, buildRowPayload(row, nextVisible));
    ElMessage.success(`菜单${actionText}成功`);
    await refreshMenus();
  } finally {
    visibilityChangingId.value = null;
  }
};

const handleDelete = async (row: MenuAdminItem) => {
  try {
    await ElMessageBox.confirm(`确定删除菜单“${row.menuName}”吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    });
    await deleteMenuMaintenanceApi(row.id);
    ElMessage.success('菜单删除成功');
    await refreshMenus();
  } catch {
    // 取消删除或删除失败时由全局错误处理器提示。
  }
};

const moveSibling = async (row: MenuAdminItem, direction: -1 | 1) => {
  const siblings = siblingRows(row);
  const index = siblings.findIndex((item) => item.id === row.id);
  const targetIndex = index + direction;
  if (index < 0 || targetIndex < 0 || targetIndex >= siblings.length) {
    return;
  }
  const next = [...siblings];
  const current = next[index];
  next[index] = next[targetIndex];
  next[targetIndex] = current;
  sorting.value = true;
  try {
    await sortMenuMaintenanceApi(next.map((item, itemIndex) => ({
      id: item.id,
      parentId: item.parentId,
      sortNo: (itemIndex + 1) * 10,
    })));
    ElMessage.success('排序已保存');
    await refreshMenus();
  } finally {
    sorting.value = false;
  }
};

onMounted(() => {
  refreshMenus();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel">
      <CommonQuerySection :model="queryForm">
        <el-form-item label="关键字">
          <el-input
            v-model="queryForm.keyword"
            placeholder="菜单编码/名称/路径/权限"
            clearable
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="queryForm.menuType" placeholder="请选择" clearable style="width: 150px">
            <el-option
              v-for="item in menuTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </CommonQuerySection>

      <CommonToolbarSection :buttons="toolbarButtons" @action="handleToolbarAction" />

      <el-table
        :data="treeData"
        row-key="id"
        border
        stripe
        default-expand-all
        class="erp-table menu-maintenance-table"
        v-loading="loading || sorting"
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column prop="menuCode" label="菜单编码" min-width="210" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.menuType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="routePath" label="路由地址" min-width="210" show-overflow-tooltip />
        <el-table-column prop="componentKey" label="组件键" min-width="240" show-overflow-tooltip />
        <el-table-column prop="permissionCode" label="权限标识" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sortNo" label="排序" width="90" align="right" />
        <el-table-column label="可见" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visible === false ? 'info' : 'success'">
              {{ row.visible === false ? '否' : '是' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button :icon="Plus" type="primary" link @click="openCreate(row)">新增下级</el-button>
            <el-button :icon="Edit" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              type="primary"
              link
              :loading="visibilityChangingId === row.id"
              @click="handleToggleVisible(row)"
            >
              {{ row.visible === false ? '显示' : '隐藏' }}
            </el-button>
            <el-button :icon="SortUp" link @click="moveSibling(row, -1)">上移</el-button>
            <el-button :icon="SortDown" link @click="moveSibling(row, 1)">下移</el-button>
            <el-button :icon="Delete" type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑菜单' : '新增菜单'"
      width="720px"
      class="standard-form-dialog"
      @closed="resetForm"
    >
      <el-form label-width="100px" class="standard-dialog-form menu-form">
        <el-form-item label="菜单编码" required>
          <el-input v-model="form.menuCode" placeholder="请输入菜单编码" />
        </el-form-item>
        <el-form-item label="菜单名称" required>
          <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
        </el-form-item>
        <el-form-item label="父级菜单">
          <el-select v-model="form.parentId" placeholder="顶级菜单" clearable filterable style="width: 100%">
            <el-option
              v-for="item in parentOptions"
              :key="item.id"
              :label="`${item.menuName}（${item.menuCode}）`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜单类型" required>
          <el-select v-model="form.menuType" style="width: 100%">
            <el-option
              v-for="item in menuTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="路由地址">
          <el-input v-model="form.routePath" placeholder="/system/example" />
        </el-form-item>
        <el-form-item label="组件键">
          <el-input v-model="form.componentKey" placeholder="system.example.index" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permissionCode" placeholder="system:example:view" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="setting" />
        </el-form-item>
        <el-form-item label="排序号" required>
          <el-input-number v-model="form.sortNo" :min="0" :max="999999" :step="10" style="width: 100%" />
        </el-form-item>
        <el-form-item label="是否可见">
          <el-switch v-model="form.visible" active-text="可见" inactive-text="隐藏" />
        </el-form-item>
        <el-form-item label="状态" required>
          <el-select v-model="form.status" style="width: 100%">
            <el-option
              v-for="item in statusOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.item-main-panel {
  overflow: visible;
}

.menu-maintenance-table :deep(.cell) {
  line-height: 1.4;
}

.menu-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  column-gap: 16px;
}

.menu-form :deep(.el-form-item:nth-child(5)),
.menu-form :deep(.el-form-item:nth-child(6)),
.menu-form :deep(.el-form-item:nth-child(7)) {
  grid-column: span 2;
}
</style>
