<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { fetchCurrentUserRolesApi, logoutApi, type CurrentUserRole } from '@/api/modules/auth';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();

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

const roleList = ref<CurrentUserRole[]>([]);
const roleText = computed(() => {
  if (!roleList.value.length) {
    return '暂无角色';
  }
  return roleList.value
    .map((item) => `${item.roleName} / ${item.scopeName || '未知机构'}`)
    .join('、');
});

const loadRoles = async () => {
  try {
    roleList.value = await fetchCurrentUserRolesApi(sessionStore.currentOrgId || undefined);
  } catch {
    roleList.value = [];
  }
};

const handleSwitchOrg = () => {
  router.push('/select-org');
};

const handleAction = (action: string) => {
  ElMessage.info(`${action} 功能待接入`);
};

const handleLogout = async () => {
  try {
    await logoutApi();
  } catch {
    // Ignore logout API failure and continue local cleanup.
  } finally {
    sessionStore.logout();
    router.replace('/login');
    ElMessage.success('已退出登录');
  }
};

onMounted(() => {
  void loadRoles();
});

watch(
  () => sessionStore.currentOrgId,
  () => {
    void loadRoles();
  },
);
</script>

<template>
  <div class="profile-page">
    <section class="profile-hero">
      <div class="profile-hero__copy">
        <div class="profile-name-row">
          <h1 class="profile-title">{{ sessionStore.userName }}</h1>
          <el-tag type="success" effect="light" size="small">
            {{ sessionStore.isLoggedIn ? '已登录' : '未登录' }}
          </el-tag>
        </div>
        <p class="profile-account">账号：{{ sessionStore.loginAccount || '-' }}</p>
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
              <div class="security-card__desc">修改密码后，如当前账号已登录，将会被强制下线</div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="handleAction('修改密码')">修改密码</el-button>
        </article>

        <article class="security-card">
          <div class="security-card__main">
            <div class="security-card__icon security-card__icon--ok">✓</div>
            <div class="security-card__copy">
              <div class="security-card__title">手机号</div>
              <div class="security-card__desc">
                已绑定手机号：{{ sessionStore.loginAccount ? `${sessionStore.loginAccount.slice(0, 3)}****${sessionStore.loginAccount.slice(-4)}` : '-' }}
              </div>
              <div class="security-card__desc security-card__desc--muted">
                当前账号的手机号如不再使用，可更换为新的手机号。
              </div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="handleAction('更换手机号')">更换手机号</el-button>
        </article>

        <article class="security-card">
          <div class="security-card__main">
            <div class="security-card__icon security-card__icon--warn">账</div>
            <div class="security-card__copy">
              <div class="security-card__title">账号</div>
              <div class="security-card__desc">账号必须由 5-20 位字母数字或下划线组成</div>
              <div class="security-card__desc security-card__desc--muted">
                目前账号名为：{{ sessionStore.loginAccount || '-' }}
              </div>
            </div>
          </div>
          <el-button plain class="security-card__action" @click="handleAction('修改账号')">修改账号</el-button>
        </article>

      </div>
    </section>

    <section class="profile-notice">
      <div class="profile-notice__title">操作说明</div>
      <ul class="profile-notice__list">
        <li>从右上角菜单进入个人中心时，你会回到这里。</li>
        <li>切换机构会清空当前页签并重新加载该机构菜单。</li>
        <li>如果当前账号需要选机构，登录后会先进入机构选择页。</li>
      </ul>
    </section>
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
  background: #14b8a6;
}

.security-card__icon--warn {
  background: #f97316;
}

.security-card__icon--role {
  background: #2563eb;
}

.security-card__copy {
  min-width: 0;
}

.security-card__title {
  color: #101828;
  font-size: 20px;
  line-height: 1.2;
  font-weight: 700;
}

.security-card__desc {
  margin-top: 6px;
  color: #667085;
  font-size: 14px;
  line-height: 1.75;
}

.security-card__desc--muted {
  color: #98a2b3;
}

.security-card__action {
  flex: 0 0 auto;
  min-width: 120px;
}

.profile-notice {
  padding: 18px 20px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #e5eaf2;
}

.profile-notice__title {
  margin-bottom: 12px;
  color: #101828;
  font-size: 16px;
  font-weight: 700;
}

.profile-notice__list {
  margin: 0;
  padding-left: 18px;
  color: #475467;
  line-height: 1.9;
}

@media (max-width: 1280px) {
  .profile-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .profile-hero {
    padding: 20px;
  }

  .profile-title {
    font-size: 24px;
  }

  .profile-meta-grid {
    grid-template-columns: 1fr;
  }

  .security-card {
    flex-direction: column;
    align-items: stretch;
  }

  .security-card__action {
    width: 100%;
  }
}
</style>
