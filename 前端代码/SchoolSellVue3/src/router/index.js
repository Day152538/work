import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'

/**
 * 路由（vue-router 4）
 *
 * 变更说明（对比原版 router/index.js）：
 * 1. vue-router 3 的 `new Router()` → 4 的 `createRouter({ history: createWebHistory() })`
 * 2. 通配符 `path: '*'` 在 4 中已移除 → 改为 `/:pathMatch(.*)*`
 * 3. 原版在 main.js 里写登录态恢复逻辑（职责错位）→ 收敛到全局前置守卫
 * 4. 单次飞行去重：恢复请求进行中时，并发跳转复用同一个 Promise
 */
const routes = [
  { path: '/', redirect: '/index' },
  {
    path: '/index',
    component: () => import('@/views/index.vue'),
    meta: { title: '校园二手物品交易平台' }
  },
  {
    path: '/search',
    component: () => import('@/views/search.vue'),
    meta: { title: '闲置二手物品 | 校园二手物品交易平台' }
  },
  {
    path: '/me',
    component: () => import('@/views/me.vue'),
    meta: { title: '个人中心 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/message',
    component: () => import('@/views/message.vue'),
    meta: { title: '消息 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/message1',
    component: () => import('@/views/message1.vue'),
    meta: { title: '信息反馈 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/ai-chat',
    component: () => import('@/views/ai-chat.vue'),
    meta: { title: 'AI 助手 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/release',
    component: () => import('@/views/release.vue'),
    meta: { title: '发布二手物品 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/details',
    component: () => import('@/views/idle-details.vue'),
    meta: { title: '二手物品详情 | 校园二手物品交易平台' }
  },
  {
    path: '/order',
    component: () => import('@/views/order.vue'),
    meta: { title: '订单详情 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/chat',
    component: () => import('@/views/chat.vue'),
    meta: { title: '交易沟通 | 校园二手物品交易平台', requiresAuth: true }
  },
  {
    path: '/login',
    component: () => import('@/views/login.vue'),
    meta: { title: '登录 | 校园二手物品交易平台' }
  },
  {
    path: '/sign-in',
    component: () => import('@/views/sign-in.vue'),
    meta: { title: '注册 | 校园二手物品交易平台' }
  },
  {
    path: '/forget',
    component: () => import('@/views/forget.vue'),
    meta: { title: '找回密码 | 校园二手物品交易平台' }
  },
  {
    path: '/login-admin',
    component: () => import('@/views/login-admin.vue'),
    meta: { title: '后台登录' }
  },
  {
    path: '/platform-admin',
    component: () => import('@/views/platform-admin.vue'),
    meta: { title: '后台管理', requiresAdmin: true }
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 单次飞行：缓存进行中的“恢复登录态”请求，避免并发路由跳转重复调 /user/me
let restorePromise = null

router.beforeEach(async (to) => {
  document.title = to.meta.title || '校园二手物品交易平台'
  const store = useUserStore()

  if (to.meta.requiresAdmin) {
    // 管理端：仅校验 admin_token 存在（接口层 401/403 兜底）
    if (!localStorage.getItem('admin_token')) return '/login-admin'
    if (!store.adminInfo) store.setAdmin({ accountNumber: 'admin' })
    return true
  }

  const token = localStorage.getItem('token')

  // 公开页面同样要恢复登录态：登录后一旦刷新/直接访问首页、搜索页、详情页，
  // Pinia 内存态被清空，若不调 /user/me 恢复，顶栏的 isLogin 恒为 false，
  // 会一直显示“注册/登录”、拿不到头像/昵称。
  if (!to.meta.requiresAuth) {
    if (token && !store.isLogin) {
      const ok = await store.refreshUser(api).catch(() => false)
      // 恢复失败（token 过期/无效）时静默清掉本地 token；
      // 401 已由 request 拦截器提示“登录已过期”
      if (!ok) store.logout()
    }
    return true
  }

  // 受保护页面：内存已有登录态直接放行
  if (store.isLogin) return true
  if (!token) return '/login'

  if (!restorePromise) {
    restorePromise = store
      .refreshUser(api)
      .catch(() => false)
      .finally(() => {
        restorePromise = null
      })
  }
  const ok = await restorePromise
  if (ok) return true
  // 恢复失败：清除失效 token 并回登录页
  store.logout()
  return '/login'
})

export default router
