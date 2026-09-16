<template>
    <div class="login-container">
        <el-card class="box-card">
            <div class="login-body">
                <div class="login-title" @click="toIndex">
                    <img src="@/assets/background.png" style="
                                width: 40px;
                                height: 40px;
                                margin: 5px 5px -5px 0;
                            " />
                    <b class="login-brand">
                        闲品校园CampusTrade
                    </b>
                </div>
                <el-form ref="form" :model="userForm">
                    <el-input placeholder="请输入手机号码..." v-model="userForm.accountNumber" class="login-input">
                        <template #prepend>
                            <User style="font-size: 24px;" />
                        </template>
                    </el-input>
                    <el-input placeholder="请输入密码..." v-model="userForm.userPassword" class="login-input"
                        @keyup.enter="login" show-password>
                        <template #prepend>
                            <Lock style="font-size: 24px;" />
                        </template>
                    </el-input>
                    <div class="login-input">
                        <captcha-field ref="captchaField" @enter="login" />
                    </div>
                    <div class="login-submit">
                        <div class="button1" @click="login" :style="submitting ? 'opacity:0.6;pointer-events:none;' : ''">
                            {{ submitting ? '登录中…' : '登录' }}</div>
                        <div class="button2" autocomplete="off" @click="$router.push('/sign-in')"
                            style="margin-left: 20px">
                            注册</div>
                    </div>
                    <div class="other">
                        <div class="other-submit">
                            <router-link to="/login-admin" class="sign-in-text"> 管理员登录</router-link>
                        </div>
                        <div class="other-submit1">
                            <router-link to="/forget" class="sign-in-text"> 忘记密码</router-link>
                        </div>
                    </div>
                </el-form>
            </div>
        </el-card>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - this.$api.userLogin → import api from '@/api'，登录改为 POST
 * - 登录成功 res.data = { user, token }：token 存 localStorage['token']，
 *   user 写入 Pinia（useUserStore().setUser），替代原 $globalData.userInfo
 * - el-icon-xxx class 图标 → Element Plus 图标组件 <User/>/<Lock/>
 * - @keyup.enter.native → @keyup.enter；错误提示交由 request.js 拦截器统一处理
 * - 删除 console.log 调试语句
 */
import api from '@/api'
import { useUserStore } from '@/stores/user'
import CaptchaField from '@/components/CaptchaField.vue'

export default {
    name: "login",
    components: {
        CaptchaField
    },
    data() {
        return {
            submitting: false, // 防重复提交
            userForm: {
                accountNumber: '',
                userPassword: ''
            }
        };
    },

    methods: {
        refreshCaptcha() {
            this.$refs.captchaField && this.$refs.captchaField.refresh();
        },
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
            if (!this.userForm.userPassword) {
                this.$message.error('请输入密码！');
                return;
            }
            const captcha = this.$refs.captchaField;
            const captchaId = captcha && captcha.captchaId;
            const captchaCode = captcha && captcha.code.trim();
            if (!captchaId || !captchaCode) {
                this.$message.error('请输入验证码');
                this.refreshCaptcha();
                return;
            }
            this.submitting = true;
            api.userLogin({
                accountNumber: this.userForm.accountNumber,
                userPassword: this.userForm.userPassword,
                captchaId: captchaId,
                captchaCode: captchaCode
            }).then(async res => {
                if (res.status_code === 1) {
                    // 持久化登录凭证：刷新/重启浏览器后靠 token 恢复登录态
                    localStorage.setItem('token', res.data.token);
                    const store = useUserStore();
                    store.setUser(res.data.user);
                    // 登录后立刻用 /user/me 拉取一次最新个人资料（与手动刷新后一致），
                    // 避免首页顶栏头像/昵称依赖登录响应、刷新后两者不一致
                    try {
                        await store.refreshUser(api);
                    } catch (e) {
                        // 拉取失败就沿用登录响应用户信息，不影响继续登录
                    }
                    this.$router.replace({ path: '/index' });
                } else {
                    this.$message.error(res.msg);
                    // 验证码为一次性使用，失败后需换一张
                    this.refreshCaptcha();
                }
            }).catch(() => {
                // 网络错误/401 已由 request.js 响应拦截器统一 ElMessage 提示
                this.refreshCaptcha();
            }).finally(() => {
                this.submitting = false;
            });
        },
        toIndex() {
            this.$router.replace({ path: '/index' });
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
    font-weight: 700;
    font-size: 20px;
    color: #163f7d;
    cursor: pointer;
}

.login-brand {
    color: #163f7d;
    font-size: 28px;
    font-weight: 700;
    display: inline-block;
    vertical-align: middle;
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
    color: #000000;
    font-size: 16px;
    text-decoration: none;
    line-height: 28px;
    padding-left: 35px;
}

.other {
    display: flex;
    justify-content: space-between;
}

.other-submit {
    display: flex;
    justify-content: space-between;
    margin-top: 30px;
    width: 50%;
}

.other-submit1 {
    display: flex;
    justify-content: space-between;
    width: 50%;
    margin-top: 30px;

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
  background-color: #79bbff;
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
  background-color: #a0cfff; /* 颜色变亮 */
}
.box-card {
    background: url("../assets/666.png") center top / cover no-repeat;
    box-shadow: 0px 4px 18px rgba(0.5, 0.5, 0.5, 0.9); /* Adjust the values as per your preference */
}
</style>
