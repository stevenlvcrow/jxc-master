/**
 * Auth Token 安全存储模块
 *
 * === 安全设计说明 ===
 *
 * [重要] 为什么不再使用 localStorage 存储 Token？
 * ────────────────────────────────────────
 * localStorage 存在 XSS 窃取风险：
 *   - 任何注入的 JS 代码均可通过 window.localStorage 直接读取 Token
 *   - 常见攻击向量：第三方脚本漏洞、DOM 注入、供应链攻击
 *   - 参考 OWASP: https://cheatsheetseries.owasp.org/cheatsheets/HTML5_Security_Cheat_Sheet.html#local-storage
 *
 * 本模块采用「内存优先 + sessionStorage 持久化备份」的双层方案：
 *
 *   Layer 1: 内存变量 (memoryStore)
 *     - Token 仅存在于 JS 闭包变量中，外部代码无法直接访问
 *     - 即使发生 XSS，攻击者也读不到内存变量（除非能执行任意代码并引用此模块）
 *     - 页面刷新后丢失 → 由 Layer 2 恢复
 *
 *   Layer 2: sessionStorage (替代 localStorage)
 *     - 仅在当前浏览器标签页有效，标签关闭即清除
 *     - 不像 localStorage 那样跨标签持久化，缩小攻击面
 *     - 用于恢复刷新前的登录状态
 *
 * === 使用方式（API完全兼容）===
 *   authStorage.getAccessToken()      -> string
 *   authStorage.getRefreshToken()     -> string
 *   authStorage.setTokens(accessToken, refreshToken?)
 *   authStorage.clearTokens()
 */

const ACCESS_TOKEN_KEY = 'jxc-access-token';
const REFRESH_TOKEN_KEY = 'jxc-refresh-token';

const isBrowser = () => typeof window !== 'undefined';

// ========== Layer 1: 内存存储（主存储）==========
let memoryAccessToken = '';
let memoryRefreshToken = '';

// ========== Layer 2: sessionStorage 持久化备份 ==========

/** 从 sessionStorage 读取 */
const readSession = (key: string): string => {
  if (!isBrowser()) {
    return '';
  }
  try {
    return sessionStorage.getItem(key) ?? '';
  } catch {
    // sessionStorage 可能因隐私模式或配额限制不可用
    return '';
  }
};

/** 写入 sessionStorage */
const writeSession = (key: string, value: string) => {
  if (!isBrowser()) {
    return;
  }
  try {
    if (!value) {
      sessionStorage.removeItem(key);
    } else {
      sessionStorage.setItem(key, value);
    }
  } catch {
    // 静默失败：内存存储仍然有效
  }
};

/**
 * 初始化时从 sessionStorage 恢复到内存
 * （确保页面刷新后仍能获取Token）
 */
if (isBrowser()) {
  memoryAccessToken = readSession(ACCESS_TOKEN_KEY);
  memoryRefreshToken = readSession(REFRESH_TOKEN_KEY);
}

export const authStorage = {
  getAccessToken: (): string => memoryAccessToken,

  getRefreshToken: (): string => memoryRefreshToken,

  setTokens: (accessToken: string, refreshToken = '') => {
    // 写入内存（主存储）
    memoryAccessToken = accessToken;
    memoryRefreshToken = refreshToken;

    // 同步到 sessionStorage（备份）
    writeSession(ACCESS_TOKEN_KEY, accessToken);
    writeSession(REFRESH_TOKEN_KEY, refreshToken);
  },

  clearTokens: () => {
    // 清除内存
    memoryAccessToken = '';
    memoryRefreshToken = '';

    // 清除备份
    writeSession(ACCESS_TOKEN_KEY, '');
    writeSession(REFRESH_TOKEN_KEY, '');
  },
};
