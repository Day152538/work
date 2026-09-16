<template>
  <div class="main-border">
    <!-- 公告表格 -->
    <el-table :data="announcementList" style="width: 100%">
      <el-table-column prop="content" label="公告内容" show-overflow-tooltip></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button type="primary" @click="editAnnouncement(scope.row)">编辑公告</el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 修改公告表单 -->
    <el-form ref="form" :model="form" label-width="80px" v-if="isEditing">
      <el-form-item label="公告内容">
        <el-input type="textarea" v-model="form.content" :rows="6"></el-input>
      </el-form-item>
      <el-form-item>
        <div class="dialog-footer">
          <div class="button1" @click="updateAnnouncement" style="margin-top: 15px;">确认修改</div>
          <div class="button2" @click="cancelEdit" style="margin-top: 15px;">取消</div>
        </div>
      </el-form-item>
    </el-form>

    <!-- 添加展示公告内容的部分 -->
    <div v-if="!isEditing && selectedAnnouncement">
      <h3>公告内容</h3>
      <p v-html="selectedAnnouncement.content"></p>
    </div>
  </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 读取公告 $api.getAllNotices() → api.getNotice()（GET /notices，公开）
 * 2. 修改公告后端已改契约：PUT /notices/{id}，@RequireAdmin 鉴权。
 *    api/index.js 未收录该接口，故直接用 adminRequest（携带 admin_token）
 *    按 NoticeController 契约调用；原版 updateNotice 无任何鉴权，任何游客可改全站公告
 * 3. 删除未被引用的 deleteAnnouncement 死方法；slot-scope → #default
 */
import api from '@/api'
import { adminRequest } from '@/utils/request'

export default {
  name: "announcementList",
  data() {
    return {
      announcementList: [], // 存储公告列表
      form: {
        id: '',
        content: '' // 公告的内容
      },
      isEditing: false, // 是否正在编辑
      selectedAnnouncement: null // 存储选中的公告内容
    };
  },
  created() {
    this.listAnnouncements();
  },
  methods: {
    listAnnouncements() {
      api.getNotice().then(res => {
        if (res.status_code == 1) {
          this.announcementList = res.data; // 更新公告列表
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
        this.$message.error('公告加载失败');
      });
    },
    editAnnouncement(announcement) {
      this.form.id = announcement.id;
      this.form.content = announcement.content;
      this.selectedAnnouncement = announcement; // 选中当前公告
      this.isEditing = true;
    },
    updateAnnouncement() {
      if (!this.form.content.trim()) {
        this.$message.error('公告内容不能为空');
        return;
      }
      adminRequest.put(`/notices/${this.form.id}`, { content: this.form.content }).then(res => {
        if (res.status_code == 1) {
          this.$message.success('公告修改成功');
          this.cancelEdit(); // 取消编辑状态
          this.listAnnouncements(); // 更新公告列表
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
        this.$message.error('公告修改失败');
      });
    },
    cancelEdit() {
      this.isEditing = false;
      this.form.id = '';
      this.form.content = '';
      this.selectedAnnouncement = null; // 取消选中的公告
    }
  }
};
</script>

<style scoped>
.main-border {
  background-color: #FFF;
  padding: 10px 30px;
  box-shadow: 0 1px 15px -6px rgba(0, 0, 0, .5);
  border-radius: 5px;
}

.button1,
.button2 {
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

.button1:hover,
.button2:hover {
  opacity: 0.85;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}
</style>
