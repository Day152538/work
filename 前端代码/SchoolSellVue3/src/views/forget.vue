<template>
    <div class="login-container">
        <el-card class="box-card">
            <div class="login-body">
                <div class="login-title" @click="toIndex">
                    <b style="color: red;display: inline-block; margin-bottom: 20px;font-size: 28px;">
                        密码重置
                    </b>
                    <br>
                    <b>（请输入正确的手机号和邮箱）</b>
                </div>
                <el-form ref="form" :model="userForm">
                    <el-input placeholder="请输入手机号码..." maxlength="11" v-model="userForm.accountNumber"
                        class="login-input" clearable>
                        <template #prepend>
                            <Iphone style="font-size: 24px;" />
                        </template>
                    </el-input>
                    <el-input placeholder="请输入邮箱..." v-model="userForm.userEmail" class="login-input" clearable>
                        <template #prepend>
                            <User style="font-size: 24px;" />
                        </template>
                    </el-input>
                    <el-input placeholder="请输入新密码" v-model="userForm.newPassword" class="login-input" show-password
                        maxlength="16" @blur="checkPassword">
                        <template #prepend>
                            <Lock style="font-size: 24px;" />
                        </template>
                    </el-input>
                    <div class="login-submit">
                        <div class="button1" @click="login"
                            :style="submitting ? 'opacity:0.6;pointer-events:none;font-size:22px;' : 'font-size:22px;'">
                            {{ submitting ? '提交中…' : '重置密码' }}</div>
                        <div class="button2" autocomplete="off" @click="$router.push('/login')"
                            style="font-size: 22px;">
                            返回登录</div>
                    </div>
                </el-form>
            </div>
        </el-card>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - this.$api.userRet → import api from '@/api'（POST /user/reset-password，
 *   参数 {accountNumber, userEmail, newPassword}）
 * - el-icon-xxx class 图标 → Element Plus 图标组件
 * - 删除 console.log；错误提示交由 request.js 拦截器统一处理
 * - 删除按钮 div 上无效的 @blur（div 不触发 blur）
 */
import api from '@/api'

export default {
    name: "reset-password",
    data() {
        return {
            submitting: false, // 防重复提交
            userForm: {
                accountNumber: '',
                userEmail: '',
                newPassword: ''
            }
        };
    },

    methods: {
        login() {
            if (this.submitting) {
                return; // 防重复提交
            }
            // 本地即时校验（与后端规则一致，后端仍会权威校验一次）
            if (!this.userForm.accountNumber) {
                this.$message.error('请输入手机号码！');
                return;
            }
            if (!/^1[3-9]\d{9}$/.test(this.userForm.accountNumber)) {
                this.$message.error('手机号码格式不正确！');
                return;
            }
            if (!this.userForm.userEmail) {
                this.$message.error('请输入邮箱！');
                return;
            }
            if (!/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(this.userForm.userEmail)) {
                this.$message.error('邮箱格式不正确！');
                return;
            }
            if (!this.checkPassword()) {
                return;
            }
            this.submitting = true;
            api.userRet({
                accountNumber: this.userForm.accountNumber,
                userEmail: this.userForm.userEmail,
                newPassword: this.userForm.newPassword
            }).then(res => {
                if (res.status_code === 1) {
                    this.$message.success("修改成功");
                    this.$router.replace({ path: '/login' });
                } else {
                    this.$message.error(res.msg);
                }
            }).catch(() => {
                // 网络错误/401 已由 request.js 拦截器统一 ElMessage 提示
            }).finally(() => {
                this.submitting = false;
            });
        },
        toIndex() {
            this.$router.replace({ path: '/login' });
        },
        checkPassword() {
            // 与后端保持一致：6-16 位，必须同时包含字母和数字
            let passwordReg = /^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$/;
            if (!passwordReg.test(this.userForm.newPassword)) {
                this.$message.error('密码需为6-16位，且必须同时包含字母和数字！');
                return false;
            }
            return true;
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

    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    overflow-y: auto;
    height: 100%;
    background: url("../assets/background01.jpg") center top / cover no-repeat;

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
    margin-top: 20px;
    display: flex;
    justify-content: center;
}

.sign-in-container {
    padding: 0 10px;
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
    margin-top: 30px;
    margin-left: 200px;
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
  margin-left: 20px;
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
