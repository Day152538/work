<template>
    <div class="captcha-field">
        <el-input v-model="code" placeholder="验证码（数字+字母，不区分大小写）" maxlength="4"
            class="captcha-input" clearable @keyup.enter="$emit('enter')"></el-input>
        <img v-if="imgSrc" :src="imgSrc" class="captcha-img" alt="验证码"
            title="看不清？点击刷新" @click="refresh" />
        <div v-else class="captcha-img captcha-loading" @click="refresh">加载中…</div>
    </div>
</template>

<script>
/**
 * 登录 / 注册共用的图形验证码组件：
 * - 挂载时自动向后端请求一张验证码（GET /captcha/image，返回 base64 图片）
 * - 点击图片可刷新；通过 $refs 读取 code / captchaId 用于提交，调用 refresh() 重新换一张
 */
import { request } from '@/utils/request'

export default {
    name: 'CaptchaField',
    emits: ['enter'],
    data() {
        return {
            code: '',
            captchaId: '',
            imgSrc: ''
        }
    },
    mounted() {
        this.refresh()
    },
    methods: {
        // 获取 / 刷新验证码
        refresh() {
            request.get('/captcha/image').then(res => {
                if (res.status_code === 1 && res.data) {
                    this.captchaId = res.data.captchaId;
                    this.imgSrc = 'data:image/png;base64,' + res.data.imgBase64;
                    this.code = '';
                } else {
                    this.imgSrc = '';
                }
            }).catch(() => {
                this.imgSrc = '';
            });
        }
    }
}
</script>

<style scoped>
.captcha-field {
    display: flex;
    align-items: center;
    gap: 10px;
}

.captcha-input {
    flex: 1 1 auto;
    min-width: 0;
}

.captcha-img {
    width: 100px;
    height: 40px;
    border-radius: 4px;
    border: 1px solid #dcdfe6;
    cursor: pointer;
    flex-shrink: 0;
}

.captcha-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    color: #909399;
    background: #f5f7fa;
}
</style>
