<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="ai-chat-page">
                <div class="ai-chat-panel">
                    <div class="ai-chat-head">
                        <div class="ai-chat-head-title">
                            <b>🤖 AI 助手</b>
                            <span>可查询平台在售商品、你的发布、分类与真实成交行情（刷新不丢记忆，联网需手动开）</span>
                        </div>
                        <el-button size="small" plain @click="newChat">＋ 新对话</el-button>
                    </div>
                    <div ref="bodyEl" class="ai-chat-body">
                        <div v-if="!messages.length" class="ai-chat-empty">
                            <p>你可以这样问我：</p>
                            <p>· 帮我查一下现在在卖的手机</p>
                            <p>· 书桌最近平台成交价大概多少？</p>
                            <p>· 我发布过哪些商品？</p>
                        </div>
                        <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
                            <div class="bubble">{{ m.content }}</div>
                        </div>
                        <div v-if="loading" class="msg assistant">
                            <div class="bubble">思考中…</div>
                        </div>
                    </div>
                    <div class="ai-chat-tools">
                        <el-button :type="webSearchOn ? 'success' : 'default'" plain size="small"
                            @click="webSearchOn = !webSearchOn">
                            🌐 联网搜索{{ webSearchOn ? '：已开（下一条生效）' : '' }}
                        </el-button>
                        <span class="ai-chat-tools-tip">{{ webSearchOn
                            ? '已开启：你接下来的提问会联网查最新信息（稍慢、含搜索费用）'
                            : '默认关闭：仅用平台数据与模型知识回答' }}</span>
                    </div>
                    <div class="ai-chat-input">
                        <el-input v-model="draft" type="textarea" :rows="2" resize="none"
                            placeholder="输入你的问题，回车发送（Shift+Enter 换行）"
                            @keydown.enter.exact.prevent="send"></el-input>
                        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
                    </div>
                </div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * AI 对话助手页（2026-09 新增）
 * - 无状态多轮：前端保留最近 8 轮随请求带回（后端不存记忆，多实例/重启不丢上下文）
 * - 消息 body 固定 { message, history:[{role,content}] } → { reply }
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'

export default {
    name: "ai-chat",
    components: { AppHead, AppBody, AppFoot },
    data() {
        return {
            draft: '',
            loading: false,
            messages: [],
            // 联网搜索按钮：默认关，点击开启后仅对“接下来这轮提问”生效
            webSearchOn: false,
            // M0：会话 id 持久化在 localStorage；刷新/换设备后凭它从后端取回历史
            conversationId: localStorage.getItem('ai_conv') || ''
        };
    },
    mounted() {
        if (this.conversationId) {
            this.loadHistory();
        }
    },
    methods: {
        send() {
            const text = (this.draft || '').trim();
            if (!text || this.loading) return;
            this.messages.push({ role: 'user', content: text });
            this.draft = '';
            this.scrollBottom();
            this.loading = true;
            api.aiChat({
                message: text,
                conversationId: this.conversationId || undefined,
                webSearch: this.webSearchOn
            }).then((res) => {
                if (res.status_code === 1 && res.data) {
                    // M0：回传的会话 id 存本地，供刷新/换设备恢复
                    if (res.data.conversationId) {
                        this.conversationId = res.data.conversationId;
                        localStorage.setItem('ai_conv', this.conversationId);
                    }
                    const reply = (res.data.reply || '').trim();
                    this.messages.push({ role: 'assistant', content: reply || '（助手没有返回内容，请重试）' });
                } else {
                    this.$message.error(res.msg || 'AI 回复失败，请稍后重试');
                }
            })
                .catch(() => {
                    // HTTP/网络/401 已由 request.js 统一提示
                })
                .finally(() => {
                    this.loading = false;
                    this.scrollBottom();
                });
        },
        loadHistory() {
            if (!this.conversationId) return;
            api.aiChatHistory(this.conversationId).then((res) => {
                if (res.status_code === 1 && Array.isArray(res.data)) {
                    this.messages = res.data.map((m) => ({ role: m.role, content: m.content }));
                    this.scrollBottom();
                }
            }).catch(() => { /* 已由 request.js 统一提示 */ });
        },
        newChat() {
            this.messages = [];
            this.conversationId = '';
            localStorage.removeItem('ai_conv');
            this.scrollBottom();
        },
        scrollBottom() {
            this.$nextTick(() => {
                const el = this.$refs.bodyEl;
                if (el) el.scrollTop = el.scrollHeight;
            });
        }
    }
}
</script>

<style scoped>
.ai-chat-page {
    min-height: 85vh;
    display: flex;
    justify-content: center;
    padding-top: 24px;
}

.ai-chat-panel {
    width: 860px;
    max-width: 94%;
    background: #fff;
    border: 1px solid #e6e9ef;
    border-radius: 12px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
    display: flex;
    flex-direction: column;
    height: 72vh;
}

.ai-chat-head {
    padding: 12px 16px;
    border-bottom: 1px solid #eef1f6;
    font-size: 16px;
    color: #303133;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.ai-chat-head span {
    margin-left: 8px;
    font-size: 12px;
    color: #909399;
    display: block;
}

.ai-chat-body {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
}

.ai-chat-empty {
    color: #a0a3ab;
    font-size: 13px;
    line-height: 1.9;
    text-align: center;
    margin-top: 30px;
}

.msg {
    margin-bottom: 14px;
    display: flex;
}

.msg.user {
    justify-content: flex-end;
}

.msg.assistant {
    justify-content: flex-start;
}

.bubble {
    max-width: 78%;
    padding: 9px 12px;
    border-radius: 10px;
    font-size: 14px;
    line-height: 1.65;
    white-space: pre-wrap;
    word-break: break-word;
}

.msg.user .bubble {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 2px;
}

.msg.assistant .bubble {
    background: #f2f6fc;
    color: #303133;
    border-top-left-radius: 2px;
}

.ai-chat-input {
    display: flex;
    align-items: flex-end;
    gap: 8px;
    padding: 10px 12px;
    border-top: 1px solid #eef1f6;
}

.ai-chat-input .el-button {
    flex-shrink: 0;
    height: 44px;
    margin-left: 0;
}

.ai-chat-tools {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px 0;
}

.ai-chat-tools-tip {
    font-size: 12px;
    color: #909399;
}
</style>
