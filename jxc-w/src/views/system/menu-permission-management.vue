<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  assignAdminRoleMenusApi,
  fetchAdminMenusApi,
  fetchAdminRolesApi,
  type MenuAdminItem,
  type RoleAdminItem,
} from '@/api/modules/system-admin';

type MenuTreeNode = {
  id: number;
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

const selectedRole = computed(() => roles.value.find((item) => item.id === selectedRoleId.value));

const menuTreeData = computed<MenuTreeNode[]>(() => {
  const grouped = new Map<number | null, MenuAdminItem[]>();
  menus.value.forEach((item) => {
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

  return build(null);
});

const refreshData = async () => {
  loading.value = true;
  try {
    const [roleList, menuList] = await Promise.all([fetchAdminRolesApi(), fetchAdminMenusApi()]);
    roles.value = roleList;
    menus.value = menuList;
    if (!selectedRoleId.value && roleList.length > 0) {
      selectedRoleId.value = roleList[0].id;
    }
  } finally {
    loading.value = false;
  }
};

watch(selectedRole, async (role) => {
  checkedMenuIds.value = role?.menuIds ?? [];
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
  const merged = Array.from(new Set([...checkedKeys, ...halfCheckedKeys].map((item) => Number(item))));

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
      <div class="section-head">
        <strong>菜单权限管理</strong>
      </div>

      <el-form :inline="true" class="filter-bar compact-filter-bar">
        <el-form-item label="角色">
          <el-select
            v-model="selectedRoleId"
            placeholder="请选择角色"
            style="width: 280px"
            filterable
          >
            <el-option
              v-for="item in roles"
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

      <el-alert
        v-if="selectedRole"
        :title="`当前角色：${selectedRole.roleName}（${selectedRole.roleCode}）`"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />

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

