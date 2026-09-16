import axios from 'axios'
import { ElMessage } from 'element-plus'

/**
 * axios 实例工厂
 *
 * 鉴权设计（对应后端 AuthInterceptor）：
 * - 用户端 token 存 localStorage['token']，调用普通业务接口
 * - 管理端 token 存 localStorage['admin_token']，角色为 admin
 *   （后端 JWT 带 role 声明，/admin/** 与 @RequireAdmin 接口要求 admin 角色）
 * - 两套 token 必须隔离：管理端 JWT 的 userId 是 sh_admin 主键，
 *   不能拿来调用户接口，反之亦然
 *
 * 原版问题（对比说明，供答辩）：
 * 1. axios 0.18（2018 年版本，有已知安全漏洞）→ 升级 axios 1.x
 * 2. 原来靠 Cookie 明文传 shUserId，任何人改 Cookie 即可冒充他人
 *    → 现在改为后端签名的 JWT，从 localStorage 读取放入 Authorization 头
 * 3. 原来响应拦截器对非 200 直接吞掉错误 → 现在 401 自动清理登录态并跳登录页
 */
function createRequest(tokenKey) {
  const service = axios.create({
    timeout: 10000,
    baseURL: import.meta.env.VITE_API_BASE
  })

  // 请求拦截：附加 JWT
  service.interceptors.request.use((config) => {
    const token = localStorage.getItem(tokenKey)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  // 响应拦截：剥离 axios 包装，直接返回后端 ResultVo { status_code, msg, data }
  service.interceptors.response.use(
    (response) => response.data,
    (error) => {
      const status = error.response?.status
      if (status === 401) {
        // 401 只在"请求携带过 token"（登录态失效）时才清态跳转；
        // 游客在公开页触发需登录接口属正常业务，不应被踢回登录页
        const hadToken = !!localStorage.getItem(tokenKey)
        localStorage.removeItem(tokenKey)
        if (hadToken && tokenKey === 'token' && !location.pathname.startsWith('/login')) {
          ElMessage.error('登录已过期，请重新登录')
          location.href = '/login'
        } else if (hadToken && tokenKey === 'admin_token' && !location.pathname.startsWith('/login-admin')) {
          // 管理端 token 失效同样要提示并回后台登录页，否则管理面板按钮会静默全部失效
          ElMessage.error('登录已过期，请重新登录')
          location.href = '/login-admin'
        }
      } else if (status === 403) {
        ElMessage.error('没有权限执行该操作')
      } else {
        ElMessage.error(error.response?.data?.msg || '网络异常，请稍后重试')
      }
      return Promise.reject(error)
    }
  )

  return service
}

/** 用户端请求实例 */
export const request = createRequest('token')
/** 管理端请求实例（带 admin 角色 JWT） */
export const adminRequest = createRequest('admin_token')
/** API 基地址（默认走 vite 代理 /api，生产改 .env.production） */
export const API_BASE = import.meta.env.VITE_API_BASE

/** 拼图片等静态资源地址（原版 12 处硬编码 localhost:9321）
 *  兼容历史数据：库里存的可能是纯文件名，也可能是旧版完整路径 "/image?imageName=xxx"，
 *  统一剥出纯文件名再拼（避免双重包装成 /api/image?imageName=%2Fimage%3F...）
 */
export const imageUrl = (name) => {
  if (!name) return ''
  const raw = String(name)
  const m = /imageName=([^&]+)/.exec(raw)
  const file = m ? m[1] : raw
  return `${API_BASE}/image?imageName=${encodeURIComponent(file)}`
}
