<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { loginApi } from '@/api/modules/auth';
import { useSessionStore } from '@/stores/session';

const router = useRouter();
const sessionStore = useSessionStore();

const account = ref('13800000000');
const password = ref('123654');
const useRealAuthApi = import.meta.env.VITE_USE_REAL_AUTH_API === '1';
const PROFILE_HOME_PATH = '/profile';

const isMockPlatformAccount = (value: string) => {
  const normalized = value.trim().toLowerCase();
  return normalized === 'admin' || normalized === '13800000000';
};

const submitLogin = async () => {
  if (!account.value.trim() || !password.value.trim()) {
    ElMessage.warning('请输入账号和密码');
    return;
  }

  if (!useRealAuthApi) {
    const loginAccount = account.value.trim();
    sessionStore.login('李智杰', loginAccount, {
      platformAdminMode: isMockPlatformAccount(loginAccount),
    });
    router.replace(sessionStore.requiresOrgSelection ? '/select-org' : PROFILE_HOME_PATH);
    return;
  }

  try {
    const result = await loginApi({
      account: account.value.trim(),
      password: password.value,
    });
    sessionStore.setAuth({
      accessToken: result.accessToken,
      refreshToken: result.refreshToken,
    });
    const loginAccount = account.value.trim();
    sessionStore.login(result.userName || loginAccount, loginAccount, {
      platformAdminMode: Boolean(result.platformAdmin),
    });
    router.replace(sessionStore.requiresOrgSelection ? '/select-org' : PROFILE_HOME_PATH);
  } catch {
    // Global error message handled in http interceptor.
  }
};
</script>

<template>
  <div class="login-page">
    <section class="login-panel">
      <div class="login-title-wrap">
        <h1 class="login-title">账号登录</h1>
        <p class="login-subtitle">欢迎登录进销存管理平台</p>
      </div>

      <div class="login-form">
        <label class="field-row">
          <span class="field-label">账号</span>
          <input v-model="account" class="field-input" type="text" />
        </label>
        <label class="field-row">
          <span class="field-label">密码</span>
          <input v-model="password" class="field-input" type="password" />
        </label>
      </div>

      <button class="submit-button" @click="submitLogin">登录</button>
    </section>
  </div>
</template>
