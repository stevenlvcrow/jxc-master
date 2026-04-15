<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  assignAdminRoleMenusApi,
  fetchAdminMenusApi,
  fetchAdminRolesApi,
  type MenuAdminItem,
  type RoleAdminItem,
} from '@/api/modules/system-admin';

type MenuTreeNode = {
  id: number | string;
  label: string;
  menuCode: string;
  menuType: string;
  children?: MenuTreeNode[];
};

const loading = ref(false);
const saving = ref(false);
const roles = ref<RoleAdminItem[]>([]);
const menus = ref<MenuAdminItem[]>([]);
const selectedRoleId = ref<number>();
const checkedMenuIds = ref<number[]>([]);
const menuTreeRef = ref<any>();
const route = useRoute();

const isGroupMenuPermissionPage = computed(() => route.path.startsWith('/group/'));
const filteredRoles = computed(() => {
  if (!isGroupMenuPermissionPage.value) {
    return roles.value;
  }
  return roles.value.filter((item) => {
    if (item.roleCode === 'GROUP_ADMIN') {
      return false;
    }
    return item.roleType === 'GROUP' || item.roleType === 'STORE';
  });
});

const selectedRole = computed(() => filteredRoles.value.find((item) => item.id === selectedRoleId.value));
const visibleMenus = computed(() => {
  const roleType = selectedRole.value?.roleType;
  if (isGroupMenuPermissionPage.value) {
    if (roleType === 'STORE') {
      return menus.value.filter((item) => item.menuCode.startsWith('STORE_BIZ_'));
    }
    if (roleType === 'GROUP') {
      return menus.value.filter((item) => item.menuCode.startsWith('GROUP_'));
    }
    return [];
  }
  if (roleType === 'STORE') {
    return menus.value.filter((item) => item.menuCode.startsWith('STORE_BIZ_'));
  }
  if (roleType === 'GROUP') {
    return menus.value.filter((item) => !item.menuCode.startsWith('STORE_BIZ_'));
  }
  return menus.value;
});

const menuTreeData = computed<MenuTreeNode[]>(() => {
  const grouped = new Map<number | null, MenuAdminItem[]>();
  visibleMenus.value.forEach((item) => {
    const key = item.parentId ?? null;
    const list = grouped.get(key) ?? [];
    list.push(item);
    grouped.set(key, list);
  });

  const build = (parentId: number | null): MenuTreeNode[] => {
    const children = grouped.get(parentId) ?? [];
    children.sort((a, b) => (a.sortNo ?? 0) - (b.sortNo ?? 0));
    return children.map((item) => ({
      id: item.id,
      label: item.menuName,
      menuCode: item.menuCode,
      menuType: item.menuType,
      children: build(item.id),
    }));
  };

  return [{
    id: -1,
    label: '全部菜单',
    menuCode: 'ALL_MENUS',
    menuType: 'ROOT',
    children: build(null),
  }];
});

const refreshData = async () => {
  loading.value = true;
  try {
    const [roleList, menuList] = await Promise.all([fetchAdminRolesApi(), fetchAdminMenusApi()]);
    roles.value = roleList;
    menus.value = menuList;
    if (!selectedRoleId.value && filteredRoles.value.length > 0) {
      selectedRoleId.value = filteredRoles.value[0].id;
    }
  } finally {
    loading.value = false;
  }
};

watch(filteredRoles, (nextRoles) => {
  if (!nextRoles.length) {
    selectedRoleId.value = undefined;
    return;
  }
  const exists = nextRoles.some((item) => item.id === selectedRoleId.value);
  if (!exists) {
    selectedRoleId.value = nextRoles[0].id;
  }
}, { immediate: true });

watch(selectedRole, async (role) => {
  const visibleMenuIdSet = new Set(visibleMenus.value.map((item) => item.id));
  checkedMenuIds.value = (role?.menuIds ?? []).filter((id) => visibleMenuIdSet.has(id));
  await nextTick();
  menuTreeRef.value?.setCheckedKeys(checkedMenuIds.value);
}, { immediate: true });

const handleSave = async () => {
  if (!selectedRoleId.value) {
    ElMessage.warning('请选择角色');
    return;
  }

  const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) ?? [];
  const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() ?? [];
  const visibleMenuIdSet = new Set(visibleMenus.value.map((item) => item.id));
  const merged = Array.from(new Set(
    [...checkedKeys, ...halfCheckedKeys]
      .map((item) => Number(item))
      .filter((id) => Number.isFinite(id) && visibleMenuIdSet.has(id)),
  ));

  saving.value = true;
  try {
    await assignAdminRoleMenusApi(selectedRoleId.value, merged);
    ElMessage.success('菜单权限保存成功');
    await refreshData();
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  refreshData();
});
</script>

<template>
  <div class="page-grid single">
    <section class="panel item-main-panel" v-loading="loading">
      <div class="menu-permission-toolbar">
        <el-form :inline="true" class="filter-bar compact-filter-bar">
          <el-form-item label="角色">
            <el-select
              v-model="selectedRoleId"
              placeholder="请选择角色"
              style="width: 280px"
              filterable
            >
              <el-option
                v-for="item in filteredRoles"
                :key="item.id"
                :label="`${item.roleName}（${item.roleCode}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSave">保存权限</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-tree
        ref="menuTreeRef"
        node-key="id"
        show-checkbox
        default-expand-all
        :data="menuTreeData"
        :props="{ label: 'label', children: 'children' }"
      >
        <template #default="{ data }">
          <el-space>
            <span>{{ data.label }}</span>
            <el-tag size="small" type="info">{{ data.menuType }}</el-tag>
            <el-tag size="small">{{ data.menuCode }}</el-tag>
          </el-space>
        </template>
      </el-tree>
    </section>
  </div>
</template>

<style scoped>
.item-main-panel {
  overflow: visible;
}

.menu-permission-toolbar {
  position: sticky;
  top: 8px;
  z-index: 20;
  margin-bottom: 12px;
  padding: 8px 0;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}
</style>

