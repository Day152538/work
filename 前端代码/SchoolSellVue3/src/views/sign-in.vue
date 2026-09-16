<template>
    <div class="sign-in-container">
        <div class="box-card">
            <div class="sign-in-body">
                <div class="sign-in-title">
                    <img src="@/assets/background.png" style="
                                width: 30px;
                                height: 30px;
                                margin: 5px 5px -5px 0;
                            " />
                    <b style="color: #31a78e;display: inline-block; margin-bottom: 20px;font-size: 28px;">
                        新帐号注册
                    </b>
                </div>
                <el-input placeholder="请输入昵称..." maxlength="30" v-model="userInfo.nickname" class="sign-in-input"
                    clearable @blur="checkNickname">
                    <template #prepend>
                        <User style="font-size: 24px;color: black;" />
                    </template>
                </el-input>
                <el-input placeholder="请输入手机号码..." maxlength="11" v-model="userInfo.accountNumber"
                    class="sign-in-input" clearable @blur="checkPhoneNumber">
                    <template #prepend>
                        <Iphone style="font-size: 24px;" />
                    </template>
                </el-input>
                <el-input placeholder="请输入密码..." show-password maxlength="16" v-model="userInfo.userPassword"
                    class="sign-in-input" clearable @blur="checkPassword">
                    <template #prepend>
                        <Lock style="font-size: 24px;color: skyblue;" />
                    </template>
                </el-input>
                <el-input placeholder="请再次输入密码..." show-password maxlength="16" v-model="userPassword2"
                    @keyup.enter="signIn" class="sign-in-input" clearable>
                    <template #prepend>
                        <Lock style="font-size: 24px;color: red;" />
                    </template>
                </el-input>
                <div class="sign-in-input">
                    <captcha-field ref="captchaField" @enter="signIn" />
                </div>
                <div class="sign-in-submit">
                    <div class="button1" @click="signIn" :style="submitting ? 'opacity:0.6;pointer-events:none;' : ''">
                        {{ submitting ? '提交中…' : '确认' }}</div>
                    <div class="button2" @click="toLogin" style="margin-left: 20px"> 返回登录</div>
                </div>

            </div>
        </div>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - this.$api.signIn → import api from '@/api'（POST /user/sign-in）
 * - el-icon-xxx class 图标 → Element Plus 图标组件 <User/>/<Iphone/>/<Lock/>
 * - @keyup.enter.native → @keyup.enter
 * - 删除 console.log 调试语句；保留原有的手机号/密码格式校验
 */
import api from '@/api'
import CaptchaField from '@/components/CaptchaField.vue'

export default {
    name: "sign-in",
    components: {
        CaptchaField
    },
    data() {
        return {
            submitting: false, // 防重复提交
            userPassword2: '',
            userInfo: {
                accountNumber: '',
                userPassword: '',
                nickname: ''
            }
        };
    },
    methods: {
        // 跳转到登录页面
        toLogin() {
            this.$router.replace({ path: '/login' });
        },
        checkNickname() {
            const name = (this.userInfo.nickname || '').trim();
            if (!name) {
                this.$message.error('昵称不能为空！');
                return false;
            }
            if (name.length > 30) {
                this.$message.error('昵称不能超过30个字符！');
                return false;
            }
            return true;
        },
        checkPhoneNumber() {
            // 与后端保持一致：1 开头，第二位 3-9，共 11 位
            let phoneReg = /^1[3-9]\d{9}$/;
            if (!phoneReg.test(this.userInfo.accountNumber)) {
                this.$message.error('手机号码格式不正确！');
                return false;
            }
            else {
                return true;
            }
        },
        checkPassword() {
            // 与后端保持一致：6-16 位，必须同时包含字母和数字
            let passwordReg = /^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$/;
            if (!passwordReg.test(this.userInfo.userPassword)) {
                this.$message.error('密码需为6-16位，且必须同时包含字母和数字！');
                return false;
            }
            else {
                return true
            }
        },

        // 刷新验证码（失败后需更换，验证码为一次性）
        refreshCaptcha() {
            this.$refs.captchaField && this.$refs.captchaField.refresh();
        },
        // 用户注册
        signIn() {
            if (this.submitting) {
                return; // 防重复提交
            }
            // 检查账号、密码和昵称是否都已填写（昵称去首尾空格判断）
            const nickname = (this.userInfo.nickname || '').trim();
            if (!this.userInfo.accountNumber || !this.userInfo.userPassword || !nickname) {
                this.$message.error('注册信息未填写完整！');
                return;
            }
            // 如果两次输入的密码不相同
            if (this.userInfo.userPassword !== this.userPassword2) {
                this.$message.error('两次输入的密码不相同！');
                return;
            }
            // 执行昵称、手机号码和密码格式验证（前端即时提示，后端同样权威校验）
            if (!this.checkNickname() || !this.checkPhoneNumber() || !this.checkPassword()) {
                return;
            }
            // 校验验证码
            const captcha = this.$refs.captchaField;
            const captchaId = captcha && captcha.captchaId;
            const captchaCode = captcha && captcha.code.trim();
            if (!captchaId || !captchaCode) {
                this.$message.error('请输入验证码');
                this.refreshCaptcha();
                return;
            }
            // 调用 API 进行用户注册（账号/密码/昵称 + 验证码，后端一并校验）
            this.submitting = true;
            api.signIn({
                accountNumber: this.userInfo.accountNumber,
                userPassword: this.userInfo.userPassword,
                nickname: nickname,
                captchaId: captchaId,
                captchaCode: captchaCode
            }).then(res => {
                if (res.status_code === 1) {
                    this.$message({
                        message: '注册成功！',
                        type: 'success'
                    });
                    this.$router.replace({ path: '/login' });
                } else {
                    this.$message.error(res.msg || '注册失败，用户已存在！');
                    this.refreshCaptcha();
                }
            }).catch(() => {
                // 网络错误已由 request.js 拦截器统一提示
                this.$message.error('注册失败，网络异常！');
                this.refreshCaptcha();
            }).finally(() => {
                this.submitting = false;
            });
        }
    }
}
</script>

<style scoped>
.sign-in-container {
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
    background: url("../assets/background03.jpg") center top / cover no-repeat;
}

.sign-in-body {
    position: relative;
    padding: 30px;
    width: 300px;
    height: 100%;

    border-radius: 10px;
    overflow: hidden;
    /* 确保背景不会溢出容器 */
    box-shadow: 0 10px 20px rgba(0.15, 0.15, 0.15, 0.15);
    /* 添加阴影效果 */
}


.sign-in-title {
    padding-bottom: 30px;
    text-align: center;
    font-weight: 600;
    font-size: 20px;
    color: #409EFF;
}

.sign-in-input {
    margin-bottom: 20px;
}

.sign-in-submit {
    margin-top: 20px;
    display: flex;
    justify-content: center;
}

.login-container {
    padding: 0 10px;
}

.login-text {
    color: #409EFF;
    font-size: 16px;
    cursor: pointer;
}

.button2 {
    width: 120px;
    height: 50px;
    font-size: 20px;
    color: white;
    text-align: center;
    background-color: #409EFF;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease;
    /* 添加过渡效果 */

}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button2:hover {
    background-color: #66b1ff;
    /* 颜色变亮 */
}

.button1 {
    width: 120px;
    height: 50px;
    font-size: 20px;
    color: white;
    text-align: center;
    background-color: #cd6b2f;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease;
    /* 添加过渡效果 */
}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
    background-color: #dc8445;
    /* 颜色变亮 */
}

.box-card {
    background: url("../assets/biejing.png") center top / cover no-repeat;
    box-shadow: 0px 4px 18px rgba(0.5, 0.5, 0.5, 0.9);
    /* Adjust the values as per your preference */
}
</style>
