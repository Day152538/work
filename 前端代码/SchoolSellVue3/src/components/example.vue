<template>
  <div class="main-border">
    <!-- 统一搜索栏 -->
    <admin-search-bar v-model="searchText" placeholder="输入反馈用户昵称 / 账号搜索" @search="search" />
    <el-table :data="userData1" stripe style="width: 100%; color: #5a5c61;">
      <el-table-column prop="nickname" label="用户昵称" show-overflow-tooltip min-width="150" width="250">
      </el-table-column>
      <el-table-column prop="accountNumber" label="用户账号" show-overflow-tooltip min-width="150" width="250">
      </el-table-column>
      <el-table-column label="操作" style="text-align: center;">
        <template #default="scope">
          <div style="display: flex; align-items: center; justify-content: center;">
            <div class="button3" @click="del(scope.row)">
              删除
            </div>
            <div class="button1" @click="showDetails(scope.row)">
              查看详情
            </div>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <!-- 弹窗组件 -->
    <el-dialog v-model="dialogVisible" title="消息详情" width="50%" :style="{ 'max-height': '80vh' }">
      <pre class="pre-wrapper">{{ adminMessage }}</pre>
    </el-dialog>
  </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - this.$api → import api from '@/api'；接口名映射（按新 API 层）：
 *   反馈列表：前端“信息反馈”写入 sh_user_message（/userMessage/add），
 *   管理端这里调 api.getUserMessagesAll()（GET /admin/userMessage/list，adminRequest，
 *   联查昵称/账号，一次返回），删除走 api.adminDeleteUserMessage(id)
 *   （原读 /receive/all（sh_admin_message）与用户提交不是同一张表，导致“反馈了看不到”）
 * - 删除“回复用户消息”功能：原实现调 addUserMessage（/userMessage/add），
 *   新后端该接口 userId 强制取自用户 JWT，管理端无用户 token，调用必然 401
 *   并把管理员踢回 /login；新后端未提供管理员回复接口，故整块移除
 * - 修复原版索引错位：原来表格行取 userData1（用户信息）索引去查 userData（留言），
 *   搜索过滤后两者长度不一致会删错/看错；现合并为单行数据 {receiveId, adminMessage, nickname, ...}
 * - :visible.sync → v-model、slot-scope → #default；删除 console.log
 */
import api from '@/api'

export default {
  name: "example",
  data() {
    return {
      userData1: [], // 合并后的展示行：留言 + 对应用户信息
      _rows: [], // 全量展示行（表格展示的是它过滤后的结果，避免二次搜索叠加过滤）
      receiveList: [], // /receive/all 返回的原始留言
      dialogVisible: false, // 控制弹窗显示
      adminMessage: '', // 保存消息内容
      searchText: '' // 保存用户输入的搜索关键词
    };
  },
  created() {
    this.getAllAdminMessage();
  },
  methods: {
    getAllAdminMessage() {
      // 读取所有用户的反馈（后端已联查昵称/账号，见 /admin/userMessage/list）
      api.getUserMessagesAll().then(res => {
        if (res.status_code === 1) {
          this._rows = (res.data || []).map(row => ({
            id: row.id,
            adminMessage: row.adminMessage,
            nickname: row.nickname || '未知用户',
            accountNumber: row.accountNumber || ''
          }));
          this.search();
        } else {
          this.$message.error(res.msg);
          this.$router.push({ path: '/login-admin' });
        }
      }).catch(() => {
        this.$message.error('反馈信息加载失败');
      });
    },
    del(row) {
      // 管理员删除反馈：DELETE /admin/userMessage/delete/{id}（adminRequest）
      api.adminDeleteUserMessage(row.id).then(res => {
        if (res.status_code == 1) {
          this.$message.success("删除成功");
          this.getAllAdminMessage();
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
      });
    },
    showDetails(row) {
      // 获取消息内容并显示弹窗
      this.adminMessage = row.adminMessage;
      this.dialogVisible = true;
    },
    search() {
      // 从全量原始行过滤（而非在已过滤结果上继续过滤）
      const kw = this.searchText.trim().toLowerCase();
      this.userData1 = this._rows.filter(row => {
        if (!kw) return true;
        const nickname = String(row.nickname || '').toLowerCase();
        const account = String(row.accountNumber || '').toLowerCase();
        const content = String(row.adminMessage || '').toLowerCase();
        return nickname.includes(kw) || account.includes(kw) || content.includes(kw);
      });
    }
  }
};
</script>
<style scoped>
.pre-wrapper {
  overflow-wrap: break-word; /* 自动换行 */
  word-wrap: break-word; /* 兼容性设置，用于较老的浏览器 */
  white-space: pre-wrap; /* 允许保留换行符，并允许自动换行 */
}
.button1,
.button2,
.button3 {
  min-width: 74px;
  height: 32px;
  padding: 0 12px;
  font-size: 14px;
  color: white;
  text-align: center;
  border-radius: 6px;
  box-sizing: border-box;
  display: flex;
  justify-content: center;
  align-items: center;
  white-space: nowrap;
  margin: 0 4px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.button1 {
  background-color: #409EFF;
}

.button2 {
  background-color: #67c23a;
}

.button3 {
  background-color: #f56c6c;
}

.button1:hover,
.button2:hover,
.button3:hover {
  opacity: 0.85;
}

.dialog-footer {
    display: flex;
    justify-content: space-between; /* 水平间距平均分布 */
    margin-top: 20px; /* 为了和上面的表单区域有一定的间距 */
}
.main-border {
    background-color: #FFF;
    padding: 10px 30px;
    box-shadow: 0 1px 15px -6px rgba(0, 0, 0, .5);
    border-radius: 5px;
}
.search-bar {
    width: 700px;
    height: 50px;
}
</style>
