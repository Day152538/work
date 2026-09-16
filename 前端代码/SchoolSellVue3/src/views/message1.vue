<template>
    <div>
      <app-head></app-head>
      <app-body>
        <div class="feedback-container">
          <div class="feedback-title">信息反馈</div>
          <hr>
          <div class="feedback-form">
            <el-form ref="feedbackForm" :model="feedbackForm">
              <el-form-item>
                <el-input type="textarea" v-model="feedbackForm.userMessage" placeholder="请输入反馈内容"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitFeedback">提交反馈</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </app-body>
      <app-foot></app-foot>
    </div>
  </template>

<script>
/*
 * 迁移要点（components/page/message1.vue → views/message1.vue）：
 * - 反馈提交走 POST /userMessage/add（api.addUserMessage），body 字段为 userMessage，
 *   userId 由后端从登录态自动填充。
 * - 管理员后台「反馈信息」读取的是同一张 sh_user_message 表（见 AdminController
 *   /admin/userMessage/list），保证“用户反馈 → 管理员可见”走同一条数据链路。
 * - $api → api；删除 console.error 调试输出
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue';
import AppFoot from '@/components/AppFoot.vue';
import api from '@/api'

export default {
    name: "feedback",
    components: {
      AppHead,
      AppBody,
      AppFoot
    },
    data() {
      return {
        feedbackForm: {
          userMessage: ''
        }
      };
    },
    methods: {
      submitFeedback() {
        // 发送反馈内容到服务器端（sh_user_message，管理员后台可统一查看）
        if (this.feedbackForm.userMessage.trim() === '') {
          this.$message.error("反馈失败,所填内容为空");
          return;
        }
        api.addUserMessage(this.feedbackForm).then(res => {
          if (res.status_code === 1) {
            this.$message.success("反馈成功，感谢您的反馈");
            this.feedbackForm.userMessage = '';
          } else {
            this.$message.error(res.msg || "反馈失败");
          }
        }).catch(() => {
          this.$message.error("反馈失败，请稍后重试");
        });
      }
    }
  }
</script>

<style scoped>
.feedback-container {
    height: 100%;
    padding: 0 20px;
  }

  .feedback-title {
    padding: 20px 0;
    width: 100%;
    height: auto;
    text-align: center;
    font-size: 25px;
    font-weight: bolder;
    color: brown;
  }

  .feedback-form {
  max-width: 600px;
  margin: 0 auto;
  text-align: center; /* 按钮水平置中 */
}
.feedback-form .el-input {
  width: 100%; /* 或者你可以设置具体的宽度，例如: 400px; */
}
</style>
