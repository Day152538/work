import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
// Element Plus 消息/弹窗等命令式服务（$message/$confirm 等不随 app.use 自动挂载，
// 必须显式引入后挂到全局属性，否则组件里 this.$message 会是 undefined）
import { ElMessage, ElMessageBox, ElNotification, ElLoading } from 'element-plus'

import App from './App.vue'
import router from './router'
import api from './api'
import AdminSearchBar from './components/AdminSearchBar.vue'

/**
 * 入口（对比原版 main.js 的变更）：
 * 1. Vue2 `new Vue()` → Vue3 `createApp()`；Element UI → Element Plus
 * 2. 删除 babel-polyfill（现代浏览器不需要）、jquery（无 DOM 操作刚需）
 * 3. api 不再挂 Vue.prototype（隐式全局），组件内显式 `import api from '@/api'`
 * 4. 登录态恢复逻辑移入 router.beforeEach（见 router/index.js）
 * 5. Pinia 替代 Vuex / $globalData / $sta 三套并存的状态方案
 * 6. （2026-09 AI 功能）补挂 $message/$confirm/$alert/$prompt/$notify/$loading：
 *    迁移 Vue2 时遗漏了 Element Plus 命令式服务的全局注册，导致 release.vue 等页面
 *    调用 this.$message 时静默失败（迁移前功能正常是巧合，触发了才暴露）
 */
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全量注册 Element Plus 图标（原版无图标体系）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 管理端各列表页共用统一的搜索栏组件
app.component('AdminSearchBar', AdminSearchBar)

// 命令式服务挂载到全局属性，兼容 Vue2 时代的 this.$message / this.$confirm 调用风格
app.config.globalProperties.$api = api
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$msgbox = ElMessageBox
app.config.globalProperties.$alert = ElMessageBox.alert
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.config.globalProperties.$prompt = ElMessageBox.prompt
app.config.globalProperties.$notify = ElNotification
app.config.globalProperties.$loading = ElLoading.service

app.mount('#app')
