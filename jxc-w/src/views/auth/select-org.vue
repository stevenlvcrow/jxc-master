<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchOrgTreeApi } from '@/api/modules/org';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();
const useRealOrgApi = import.meta.env.VITE_USE_REAL_ORG_API === '1';

const topTrialNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'trial'));
const groupNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'group'));
const topStoreNodes = computed(() => sessionStore.rootGroups.filter((node) => node.type === 'store'));
const activeGroups = ref<string[]>(groupNodes.value.map((item) => item.id));

const chooseOrg = (orgId: string) => {
  sessionStore.selectOrg(orgId);
  ElMessage.success('机构选择成功');
  router.replace('/dashboard');
};

const handleLogout = () => {
  sessionStore.logout();
  router.replace('/login');
};

onMounted(async () => {
  if (!useRealOrgApi) {
    return;
  }
  try {
    const tree = await fetchOrgTreeApi();
    sessionStore.setOrgTree(tree);
    activeGroups.value = tree.filter((item) => item.type === 'group').map((item) => item.id);
  } catch {
    // Global error message handled in http interceptor.
  }
});
</script>

<template>
  <div class="select-org-page">
    <section class="select-org-wrap">
      <div class="select-org-head">
        <h1 class="select-org-title">请选择机构</h1>
        <el-button text type="danger" @click="handleLogout">退出登录</el-button>
      </div>

      <el-card
        v-for="trial in topTrialNodes"
        :key="trial.id"
        class="org-card"
        shadow="never"
      >
        <div class="org-row-head">
          <div class="org-title-line">
            <el-tag type="success" effect="light">试店</el-tag>
            <strong>{{ trial.name }}</strong>
          </div>
          <el-button type="primary" size="small" @click="chooseOrg(trial.id)">选择</el-button>
        </div>
        <p class="org-meta">商户号：{{ trial.merchantNo }}　机构编码：{{ trial.code }}</p>
      </el-card>

      <el-card
        v-for="store in topStoreNodes"
        :key="store.id"
        class="org-card"
        shadow="never"
      >
        <div class="org-row-head">
          <div class="org-title-line">
            <el-tag type="warning" effect="light">门店</el-tag>
            <strong>{{ store.name }}</strong>
          </div>
          <el-button type="primary" size="small" @click="chooseOrg(store.id)">选择</el-button>
        </div>
        <p class="org-meta">商户号：{{ store.merchantNo }}　机构编码：{{ store.code }}</p>
      </el-card>

      <el-collapse v-model="activeGroups" class="org-collapse">
        <el-collapse-item
          v-for="group in groupNodes"
          :key="group.id"
          :name="group.id"
          class="org-collapse-item"
        >
          <template #title>
            <div class="org-group-title-wrap">
              <div class="org-row-head collapse-head">
                <div class="org-title-line">
                  <el-tag type="primary" effect="light">集团</el-tag>
                  <strong>{{ group.name }}</strong>
                </div>
                <el-button type="primary" size="small" @click.stop="chooseOrg(group.id)">选择</el-button>
              </div>
              <div class="org-meta">集团号：{{ group.merchantNo }}　机构编码：{{ group.code }}</div>
            </div>
          </template>

          <div class="org-child-wrap">
            <el-divider />
            <div
              v-for="child in group.children ?? []"
              :key="child.id"
              class="org-child-item"
            >
              <div class="org-child-main">
                <strong>{{ child.name }}</strong>
                <p class="org-meta">商户号：{{ child.merchantNo }}　机构编码：{{ child.code }}</p>
              </div>
              <el-button type="primary" size="small" @click="chooseOrg(child.id)">选择</el-button>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
    </section>
  </div>
</template>

<style scoped>
.select-org-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
