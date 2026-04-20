<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import {
  changeCurrentUserAccountApi,
  changeCurrentUserPasswordApi,
  changeCurrentUserPhoneApi,
  fetchCurrentUserProfileApi,
  fetchCurrentUserRolesApi,
  logoutApi,
  type CurrentUserProfile,
  type CurrentUserRole,
} from '@/api/modules/auth';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();

const loadingProfile = ref(false);
const roleList = ref<CurrentUserRole[]>([]);
const profile = reactive<CurrentUserProfile>({
  userId: 0,
  userName: '',
  account: '',
  phone: '',
});

const dialogMode = ref<'password' | 'phone' | 'account' | ''>('');
const dialogVisible = computed({
  get: () => Boolean(dialogMode.value),
  set: (value: boolean) => {
    if (!value) {
      dialogMode.value = '';
    }
  },
});
const submitting = ref(false);
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const phoneForm = reactive({
  phone: '',
});
const accountForm = reactive({
  account: '',
});

const currentOrgLabel = computed(() => {
  const org = sessionStore.currentOrg;
  if (!org) {
    return sessionStore.requiresOrgSelection ? '尚未选择机构' : '平台模式';
  }
  const orgTypeLabel = org.type === 'group' ? '集团' : org.type === 'store' ? '门店' : '试店';
  return `${orgTypeLabel} / ${org.name}`;
});

const currentOrgCode = computed(() => sessionStore.currentOrg?.code ?? '-');
const currentOrgMerchant = computed(() => sessionStore.currentOrg?.merchantNo ?? '-');
const currentOrgCity = computed(() => sessionStore.currentOrg?.city ?? '-');
const maskedPhone = computed(() => {
  const phone = profile.phone?.trim();
  if (!phone || phone.length < 7) {
    return '-';
  }
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
});
const roleText = computed(() => {
  if (!roleList.value.length) {
    return '暂无角色';
  }
  return roleList.value
    .map((item) => `${item.roleName} / ${item.scopeName || '未知机构'}`)
    .join('、');
});
const dialogTitle = computed(() => {
  if (dialogMode.value === 'password') return '修改密码';
  if (dialogMode.value === 'phone') return '更换手机号';
  if (dialogMode.value === 'account') return '修改账号';
  return '';
});

const syncProfileToStore = (value: CurrentUserProfile) => {
  profile.userId = value.userId;
  profile.userName = value.userName || '';
  profile.account = value.account || '';
  profile.phone = value.phone || '';
  sessionStore.setProfile({
    userName: profile.userName,
    account: profile.account,
    phone: profile.phone,
  });
};

const loadProfile = async () => {
  if (!sessionStore.isLoggedIn) {
    return;
  }
  loadingProfile.value = true;
  try {
    const data = await fetchCurrentUserProfileApi();
    syncProfileToStore(data);
  } finally {
    loadingProfile.value = false;
  }
};

const loadRoles = async () => {
  if (!sessionStore.isLoggedIn) {
    roleList.value = [];
    return;
  }
  try {
    roleList.value = await fetchCurrentUserRolesApi(sessionStore.currentOrgId || undefined);
  } catch {
    roleList.value = [];
  }
};

const resetForms = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  phoneForm.phone = profile.phone || '';
  accountForm.account = profile.account || '';
};

const openDialog = (mode: 'password' | 'phone' | 'account') => {
  dialogMode.value = mode;
  resetForms();
};

const submitDialog = async () => {
  if (!dialogMode.value) {
    return;
  }
  submitting.value = true;
  try {
    if (dialogMode.value === 'password') {
      if (!passwordForm.oldPassword.trim() || !passwordForm.newPassword.trim() || !passwordForm.confirmPassword.trim()) {
        ElMessage.warning('请完整填写密码信息');
        return;
      }
      if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        ElMessage.warning('两次输入的新密码不一致');
        return;
      }
      await changeCurrentUserPasswordApi({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
      });
      ElMessage.success('密码修改成功，请重新登录');
      try {
        await logoutApi();
      } catch {
        // Ignore logout API failure and continue local cleanup.
      } finally {
        sessionStore.logout();
        await router.replace('/login');
      }
      return;
    }

    if (dialogMode.value === 'phone') {
      if (!/^1\d{10}$/.test(phoneForm.phone.trim())) {
        ElMessage.warning('请输入正确的11位手机号');
        return;
      }
      const data = await changeCurrentUserPhoneApi({ phone: phoneForm.phone.trim() });
      syncProfileToStore(data);
      dialogVisible.value = false;
      ElMessage.success('手机号更新成功');
      return;
    }

    if (!/^[A-Za-z][A-Za-z0-9_]{4,19}$/.test(accountForm.account.trim())) {
      ElMessage.warning('账号必须为5-20位字母数字下划线，且以字母开头');
      return;
    }
    const data = await changeCurrentUserAccountApi({ account: accountForm.account.trim() });
    syncProfileToStore(data);
    dialogVisible.value = false;
    ElMessage.success('账号更新成功');
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  void loadProfile();
  void loadRoles();
});

watch(
  () => [sessionStore.isLoggedIn, sessionStore.currentOrgId] as const,
  ([isLoggedIn]) => {
    if (!isLoggedIn) {
      roleList.value = [];
      return;
    }
    void loadProfile();
    void loadRoles();
  },
);
</script>

<template>
  <div class="profile-page" v-loading="loadingProfile">
    <section class="profile-hero">
      <div class="profile-hero__copy">
        <div class="profile-name-row">
          <h1 class="profile-title">{{ profile.userName || sessionStore.userName }}</h1>
          <el-tag type="success" effect="light" size="small">
            {{ sessionStore.isLoggedIn ? '已登录' : '未登录' }}
          </el-tag>
        </div>
        <p class="profile-account">账号：{{ profile.account || sessionStore.loginAccount || '-' }}</p>
        <p class="profile-role-line">所属角色：{{ roleText }}</p>
      </div>
    </section>

    <section class="profile-meta-grid">
      <article class="profile-meta-card">
        <div class="profile-meta-label">当前机构</div>
        <div class="profile-meta-value">{{ currentOrgLabel }}</div>
        <div class="profile-meta-sub">机构编码：{{ currentOrgCode }}</div>
      </article>

      <article class="profile-meta-card">
        <div class="profile-meta-label">机构信息</div>
        <div class="profile-meta-value">{{ currentOrgMerchant }}</div>
        <div class="profile-meta-sub">城市：{{ currentOrgCity }}</div>
      </article>

      <article class="profile-meta-card">
        <div class="profile-meta-label">登录模式</div>
        <div class="profile-meta-value">
          {{ sessionStore.platformAdminMode ? '平台管理员' : '集团 / 门店账号' }}
        </div>
        <div class="profile-meta-sub">
          {{ sessionStore.requiresOrgSelection ? '需要先选择机构' : '无需机构切换' }}
        </div>
      </article>
    </section>

    <section class="section-block">
      <div class="section-title">
        <span class="section-title__bar" />
        <span>账户与安全</span>
      </div>

      <div class="security-list">
        <article class="security-card">
          <div class="security-card__main">
            <div class="security-card__icon security-card__icon--ok">✓</div>
            <div class="security-card__copy">
              <div class="security-card__title">登录密码</div>
              <div class="security-card__desc">修改密码后，当前登录会话会立即失效</div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="openDialog('password')">修改密码</el-button>
        </article>

        <article class="security-card">
          <div class="security-card__main">
            <div class="security-card__icon security-card__icon--ok">✓</div>
            <div class="security-card__copy">
              <div class="security-card__title">手机号</div>
              <div class="security-card__desc">已绑定手机号：{{ maskedPhone }}</div>
              <div class="security-card__desc security-card__desc--muted">
                当前账号的手机号如不再使用，可更换为新的手机号。
              </div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="openDialog('phone')">更换手机号</el-button>
        </article>

        <article class="security-card">
          <div class="security-card__main">
            <div class="security-card__icon security-card__icon--warn">账</div>
            <div class="security-card__copy">
              <div class="security-card__title">账号</div>
              <div class="security-card__desc security-card__desc--muted">
                当前账号：{{ profile.account || sessionStore.loginAccount || '-' }}
              </div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="openDialog('account')">修改账号</el-button>
        </article>
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" destroy-on-close>
      <div v-if="dialogMode === 'password'" class="dialog-form">
        <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
      </div>
      <div v-else-if="dialogMode === 'phone'" class="dialog-form">
        <el-input v-model="phoneForm.phone" maxlength="11" placeholder="请输入新的11位手机号" />
      </div>
      <div v-else-if="dialogMode === 'account'" class="dialog-form">
        <el-input v-model="accountForm.account" maxlength="20" placeholder="请输入新的账号编码" />
        <div class="dialog-tip">账号需为5-20位字母数字下划线，且以字母开头。</div>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDialog">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile-hero {
  padding: 28px 30px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfbfd 100%);
  border: 1px solid #eef1f6;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.05);
}

.profile-hero__copy {
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.profile-title {
  margin: 0;
  color: #101828;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 700;
}

.profile-account {
  margin: 12px 0 0;
  color: #667085;
  font-size: 15px;
}

.profile-role-line {
  margin: 8px 0 0;
  color: #344054;
  font-size: 14px;
  line-height: 1.8;
}

.profile-meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.profile-meta-card {
  padding: 18px 20px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e8edf4;
  box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
}

.profile-meta-label {
  color: #667085;
  font-size: 13px;
}

.profile-meta-value {
  margin-top: 10px;
  color: #101828;
  font-size: 18px;
  font-weight: 700;
  word-break: break-word;
}

.profile-meta-sub {
  margin-top: 8px;
  color: #667085;
  font-size: 13px;
}

.section-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #101828;
  font-size: 17px;
  font-weight: 700;
}

.section-title__bar {
  width: 6px;
  height: 22px;
  border-radius: 999px;
  background: #f59e0b;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.security-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px 22px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.security-card__main {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  min-width: 0;
}

.security-card__icon {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.security-card__icon--ok {
  background: #16a34a;
}

.security-card__icon--warn {
  background: #f59e0b;
}

.security-card__copy {
  min-width: 0;
}

.security-card__title {
  color: #101828;
  font-size: 16px;
  font-weight: 700;
}

.security-card__desc {
  margin-top: 6px;
  color: #344054;
  font-size: 14px;
  line-height: 1.7;
}

.security-card__desc--muted {
  color: #667085;
}

.security-card__action {
  flex: 0 0 auto;
}

.dialog-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dialog-tip {
  color: #667085;
  font-size: 12px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .profile-meta-grid {
    grid-template-columns: 1fr;
  }

  .security-card {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
