import { defineStore } from 'pinia'

/**
 * 用户状态（Pinia，替代原版挂在 Vue.prototype 上的 $globalData/$sta 全局变量）
 *
 * 原版问题：
 * - $globalData.userInfo.nickname 是非响应式普通对象，跨组件同步靠手动赋值
 * - isLogin/adminName 散落在 $sta 里，无持久化约定
 * Pinia 的 state 天然响应式，组件可直接绑定；computed 提供 isVip 派生。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: {},
    adminInfo: null
  }),

  getters: {
    isLogin: (state) => !!localStorage.getItem('token') && !!state.userInfo.nickname,
    /**
     * VIP 判断：后端 vip_expire_time > 当前时间即为会员
     * （原版用 userStatus===3 判断，与封禁状态字段耦合，且可被前端伪造）
     */
    isVip: (state) => {
      const t = state.userInfo.vipExpireTime
      return !!t && new Date(t).getTime() > Date.now()
    },
    isAdminLogin: (state) => !!localStorage.getItem('admin_token') && !!state.adminInfo
  },

  actions: {
    setUser(user) {
      this.userInfo = user || {}
    },
    setAdmin(admin) {
      this.adminInfo = admin
    },
    async refreshUser(api) {
      const res = await api.userMe()
      if (res && res.status_code === 1 && res.data) {
        if (res.data.signInTime) {
          res.data.signInTime = String(res.data.signInTime).substring(0, 10)
        }
        this.setUser(res.data)
        return true
      }
      return false
    },
    logout() {
      localStorage.removeItem('token')
      this.userInfo = {}
    },
    adminLogout() {
      localStorage.removeItem('admin_token')
      this.adminInfo = null
    }
  }
})
