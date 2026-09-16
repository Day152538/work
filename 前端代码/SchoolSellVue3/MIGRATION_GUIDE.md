# Vue2 → Vue3 迁移规范（内部工作文档）

本文件供组件迁移时统一遵守。新项目路径：`F:\BaiduNetdiskDownload\idleschool\前端代码\SchoolSellVue3`
旧项目参照：`F:\BaiduNetdiskDownload\idleschool\前端代码\SchoolSellVue\src`

## 路径映射
- `components/page/*.vue` → `src/views/*.vue`
- `components/common/*.vue` → `src/components/*.vue`

## 必须遵守的迁移规则

### 1. API 调用
- 删除 `this.$api.xxx`，改为文件顶部 `import api from '@/api'`，调用 `api.xxx(...)`
- 响应结构：`{ status_code: 1|0, msg, data }`，判断 `res.status_code === 1`
- 接口签名以 `src/api/index.js` 为准（**先读它**），与旧版差异：
  - `userLogin(data)` POST，返回 `res.data = { user, token }`；登录成功后 `localStorage.setItem('token', res.data.token)`
  - `adminLogin(data)` POST，参数 `{accountNumber, adminPassword}`，返回 `res.data = { admin, token }`；存 `localStorage.setItem('admin_token', res.data.token)`
  - `updatePassword({oldPassword, newPassword})` POST
  - `userRet({accountNumber, userEmail, newPassword})` POST（找回密码）
  - 管理端删除用户 `adminDeleteUser(id)`、重置密码 `adminResetPassword(params)`；商品/订单管理接口全部 POST
  - 新增 `subscribeVip()`：开通会员，返回最新 user；**删除一切前端直接改 userStatus=3 的假支付逻辑**

### 2. 状态管理（Pinia）
- 删除 `this.$globalData.userInfo` → `import { useUserStore } from '@/stores/user'`
- Options API 中用法：在 `created()`（或其他任意生命周期）里 `this.userStore = useUserStore()`，模板里 `userStore.userInfo.nickname`
- 判断 VIP：`userStore.isVip`（基于 vipExpireTime），**不要用 userStatus===3**
- 判断登录：`userStore.isLogin`

### 3. 事件总线
- `import bus from '@/utils/bus'`（mitt）
- `bus.$emit(e, d)` → `bus.emit(e, d)`；`bus.$on` → `bus.on`；`bus.$off` → `bus.off`

### 4. 图片/静态地址
- `http://localhost:9321/image?imageName=xxx` 或拼接 baseApi 的图片 → `imageUrl('xxx')`（`import { imageUrl } from '@/utils/request'`）
- `require('@/assets/xxx.png')` → `import 变量名 from '@/assets/xxx.png'` 顶部静态引入（Vite 不支持 require）
- `/avatar.jpg` 等公共目录引用不变

### 5. Element UI → Element Plus
- `el-dialog :visible.sync="x"` → `v-model="x"`（`:visible.sync` 全部替换）
- `<template slot="xxx">` / `slot-scope="scope"` → `<template #xxx>` / `#default="scope"`
- `el-icon-xxx` class 图标 → 删除或换 Element Plus 图标组件（`<Search/>` 等已全局注册）
- `el-select` 的 `value-key`/事件基本不变；`this.$message`/`this.$confirm` 可继续用
- `el-date-picker` 的 `picker-options` → `:disabled-date` 回调（若有）
- `el-input` 上 `.native` 修饰符删除（`@keyup.enter.native` → `@keyup.enter`）

### 6. Vue3 语法差异
- `beforeDestroy` → `beforeUnmount`；`destroyed` → `unmounted`
- `this.$set(obj, k, v)` → `obj[k] = v`；`this.$delete` → `delete obj[k]`
- 过滤器 `{{ x | fmt }}` / `filters:{}` 已移除 → 改为 method 调用 `{{ fmt(x) }}`
- `v-model` 自定义组件默认 prop 由 `value/input` 改为 `modelValue/update:modelValue`（本项目主要是 element 组件，无需处理）
- 其他 Options API（data/computed/methods/watch/mounted）保持不变即可，Vue3 兼容
- 移除所有 jquery 引用（如个别地方用了 `$(...)`，改写为原生 DOM 或删除）

### 7. 代码卫生（原分析报告要求）
- 删除 `console.log` 调试语句
- `setInterval` 必须在 `beforeUnmount` 清除（`clearInterval`）
- 移除死代码、未使用的 import

### 8. 功能语义调整（后端已改）
- 退出登录：调 `api.logout()`（可忽略结果）+ `localStorage.removeItem('token')` + `userStore.logout()` + 跳转 `/login`
- VIP 展示一律看 `vipExpireTime`/`userStore.isVip`
- 订单超时时间统一 30 分钟（Constants.ORDER_TIMEOUT_MINUTES），倒计时逻辑若硬编码请改 30
- 管理端（platform-admin）不要保留"批量取消订单调 /user 接口"之类越权逻辑，全部走 admin API

### 9. 交付要求
- 保留原有页面功能与视觉（本任务是框架迁移，不是重设计）
- 每个迁移文件在 `<script>` 顶部加简短注释块说明本文件的迁移要点（3-5 行）
- 文件必须语法完整（template/script/style 齐全），style 里的 scoped 保持原样
