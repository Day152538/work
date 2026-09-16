<template>
  <div id="app">
    <router-view></router-view>
    <!-- AI 悬浮助手（右下角入口 + 聊天面板） -->
    <ai-assistant></ai-assistant>
    <!-- 平台公告弹窗：登录后当天首次访问弹出一次，避免频繁打扰 -->
    <el-dialog v-model="noticeDialogVisible" title="平台公告" width="480px" :close-on-click-modal="false">
      <div class="notice-popup-body">
        <div v-for="n in noticeList" :key="n.id" class="notice-popup-item">
          {{ n.content }}
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="closeNoticeDialog">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import api from '@/api'
import AiAssistant from '@/components/AiAssistant.vue'

// 整页等比例缩放：设计稿固定 DESIGN_WIDTH 宽。
// 窗口更窄时用 CSS zoom 整页等比缩小（zoom 不改变 fixed 元素参照系，
// 页眉仍吸附顶部；transform: scale 会让 fixed 定位错乱）。
// Vue3 变更：beforeDestroy → onBeforeUnmount，生命周期钩子改名。
const DESIGN_WIDTH = 1000
const NOTICE_POPUP_KEY = 'notice_popup_date'

export default {
  name: 'App',
  components: { AiAssistant },
  data() {
    return {
      noticeDialogVisible: false,
      noticeList: []
    }
  },
  mounted() {
    this._onResize = () => this.adjustScale()
    this._onResize()
    window.addEventListener('resize', this._onResize)
    this.checkNoticePopup()
  },
  beforeUnmount() {
    window.removeEventListener('resize', this._onResize)
  },
  methods: {
    adjustScale() {
      const scale = Math.min(1, window.innerWidth / DESIGN_WIDTH)
      document.documentElement.style.zoom = scale
    },
    // 公告弹窗：已登录且当天未弹过才弹出（记录日期到 localStorage）
    checkNoticePopup() {
      if (!localStorage.getItem('token')) return
      const today = new Date().toLocaleDateString('zh-CN')
      if (localStorage.getItem(NOTICE_POPUP_KEY) === today) return
      api.getNotice().then(res => {
        if (res.status_code === 1 && Array.isArray(res.data) && res.data.length > 0) {
          this.noticeList = res.data
          this.noticeDialogVisible = true
        }
      }).catch(() => {})
    },
    closeNoticeDialog() {
      this.noticeDialogVisible = false
      localStorage.setItem(NOTICE_POPUP_KEY, new Date().toLocaleDateString('zh-CN'))
    }
  }
}
</script>

<style>
body,
#app {
  overflow-y: visible;
  background-color: #f6f6f6;
}

html {
  overflow-y: scroll;
  background-color: #f6f6f6;
}

/* Element Plus 分页器左右间距修复（沿用原版） */
.el-pagination .el-pager {
  padding: 0;
}

/* 各列表页通用的居中分页条 */
.list-paging {
  display: flex;
  justify-content: center;
  padding: 16px 0 4px;
}

/* 公告弹窗内容（el-dialog 挂载在 body，须用全局样式） */
.notice-popup-body {
  max-height: 50vh;
  overflow-y: auto;
}

.notice-popup-item {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
}
</style>
