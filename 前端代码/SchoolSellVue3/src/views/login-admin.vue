<template>
    <div class="login-container">
        <el-card class="">
            <div class="login-body">
                <div class="login-title">
                    <b style="color: black;display: inline-block; margin-bottom: 20px;font-size: 28px;">
                        管理员登录
                    </b>
                </div>
                <el-form ref="form" :model="userForm">
                    <el-input placeholder="请输入管理员账号" v-model="userForm.accountNumber" class="login-input">
                        <template #prepend>
                            <User style="font-size: 24px;color: red;" />
                        </template>
                    </el-input>
                    <el-input placeholder="请输入管理员密码" v-model="userForm.adminPassword" class="login-input"
                        @keyup.enter="login" show-password>
                        <template #prepend>
                            <Lock style="font-size: 24px;color: red;" />
                        </template>
                    </el-input>
                    <div class="login-submit" style="margin-top: 20px">
                        <div class="button1" @click="login"
                            :style="submitting ? 'opacity:0.6;pointer-events:none;' : ''">
                            {{ submitting ? '登录中…' : '登录' }}</div>
                        <div class="button2" autocomplete="off" @click="$router.push('/login')"
                            style="margin-left: 20px">
                            返回</div>
                    </div>
                </el-form>
            </div>
        </el-card>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - this.$api.adminLogin → import api from '@/api'（POST，参数 {accountNumber, adminPassword}）
 * - 登录成功 res.data = { admin, token }：token 存 localStorage['admin_token']，
 *   admin 写入 Pinia（useUserStore().setAdmin），替代原 $sta.isLogin/$sta.adminName
 * - iconfont 图标 class（新项目未引入 iconfont 字体）→ Element Plus 图标组件 <User/>/<Lock/>
 * - @keyup.enter.native → @keyup.enter；删除 console.log
 */
import api from '@/api'
import { useUserStore } from '@/stores/user'

export default {
    name: "login-admin",
    data() {
        return {
            submitting: false, // 防重复提交
            userForm: {
                accountNumber: '',
                adminPassword: ''
            }
        };
    },
    methods: {
        login() {
            if (this.submitting) {
                return; // 防重复提交
            }
            if (!this.userForm.accountNumber) {
                this.$message.error('请输入管理员账号！');
                return;
            }
            if (!this.userForm.adminPassword) {
                this.$message.error('请输入管理员密码！');
                return;
            }
            this.submitting = true;
            api.adminLogin({
                accountNumber: this.userForm.accountNumber,
                adminPassword: this.userForm.adminPassword
            }).then(res => {
                if (res.status_code === 1) {
                    // 管理端凭证与用户端 token 隔离，存 admin_token
                    localStorage.setItem('admin_token', res.data.token);
                    useUserStore().setAdmin(res.data.admin);
                    this.$router.replace({ path: '/platform-admin' });
                } else {
                    this.$message.error(res.msg || '登录失败，账号或密码错误！');
                }
            }).catch(() => {
                // 网络错误已由 adminRequest 拦截器统一 ElMessage 提示
            }).finally(() => {
                this.submitting = false;
            });
        }
    }
}
</script>

<style scoped>
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    width: 100%;
    background-color: #f1f1f1;

    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    overflow-y: auto;
    height: 100%;
    background: url("../assets/background.jpeg") center top / cover no-repeat;
}

.login-body {
    padding: 30px;
    width: 300px;
    height: 100%;
}

.login-title {
    padding-bottom: 30px;
    text-align: center;
    font-weight: 600;
    font-size: 20px;
    color: #409EFF;
    cursor: pointer;
}

.login-input {
    margin-bottom: 20px;
}

.login-submit {
    display: flex;
    justify-content: center;
}

.sign-in-text {
    color: #409EFF;
    font-size: 16px;
    text-decoration: none;
    line-height: 28px;
}

.other-submit {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
}
.button1 {
  width: 120px;
  height: 50px;
  font-size: 20px;
  color: white;
  text-align: center;
  background-color: #409EFF;
  border-radius: 10px;
  /* 使用Flexbox布局 */
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */

  /* 设置鼠标悬浮样式 */
  cursor: pointer;
  transition: background-color 0.3s ease; /* 添加过渡效果 */

}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
  background-color: #66b1ff; /* 颜色变亮 */
}
.button2 {
  width: 120px;
  height: 50px;
  font-size: 20px;
  color: white;
  text-align: center;
  background-color: #bcd462;
  border-radius: 10px;
  /* 使用Flexbox布局 */
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */

  /* 设置鼠标悬浮样式 */
  cursor: pointer;
  transition: background-color 0.3s ease; /* 添加过渡效果 */
}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button2:hover {
  background-color: #d2e493; /* 颜色变亮 */
}

</style>
