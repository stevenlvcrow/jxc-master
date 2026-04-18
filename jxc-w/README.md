# JXC Admin Starter

一个适合管理平台的 Vue 3 脚手架，内置以下基础能力：

- Vue 3 + Vite + TypeScript
- Vue Router 路由管理
- Pinia 状态管理
- Element Plus 作为后台 UI 组件库
- Axios 请求封装
- 管理后台布局与示例页面

## API 对接模板（已内置）

当前项目已按“可扩展”的方式拆分接口层：

```text
src/api/
  auth-storage.ts      # token 持久化
  errors.ts            # 统一 ApiError
  http-client.ts       # axios 实例 + 鉴权 + 401 刷新 + 错误归一
  refresh-token.ts     # 刷新 token（单飞）
  modules/
    auth.ts            # 登录示例
    item.ts            # 物品列表、分类、标签与统计类型接口
    org.ts             # 机构树示例
  types.ts             # 通用响应/分页类型
  index.ts             # 统一导出
```

### 响应约定

默认按以下格式自动解包（`code === 0` 或 `200` 判定成功）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {}
}
```

如果你的后端返回结构不同，只需要调整 `src/api/http-client.ts` 的 `handleResponse`。

### 认证与刷新

- 请求自动注入 `Authorization: Bearer <accessToken>`
- `401` 自动触发 `/auth/refresh`
- 刷新成功后自动重放原请求
- 刷新失败会清空 token

## 启动

```bash
npm.cmd install
npm.cmd run dev
```

## 环境变量

开发环境默认对接真实后端接口，需确保后端服务已启动：

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_USE_REAL_AUTH_API=1
VITE_USE_REAL_ORG_API=1
```

## 新增一个业务接口的标准步骤

1. 在 `src/api/modules/*.ts` 新增函数
2. 用 `apiClient.get/post/put/delete` 发请求，并补全返回类型
3. 在页面或 store 调用接口
4. 业务错误无需重复 toast，拦截器会统一处理

示例：

```ts
// src/api/modules/item.ts
import { apiClient } from '@/api/http-client';

export type Item = { id: string; name: string };

export const fetchItemsApi = () => apiClient.get<Item[]>('/items');
```

## 建议下一步

1. 对接 OpenAPI 文档并自动生成 `types + modules`
2. 引入 `@tanstack/vue-query` 统一缓存、重试和失效刷新
3. 增加业务级错误码映射（例如 10001 显示特定文案）
