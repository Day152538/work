<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="chat-page">
                <div class="chat-panel" v-if="ready">
                    <div class="chat-head">
                        <div>
                            <b>交易沟通</b>
                            <span class="chat-sub">商品：{{ itemName }}<template v-if="orderNumber"> · 订单 #{{ orderNumber }}</template></span>
                        </div>
                        <el-button size="small" plain @click="back">返回</el-button>
                    </div>

                    <div class="chat-partner">
                        <el-avatar :size="44" shape="square" :src="partnerAvatar">
                            <el-icon><UserFilled /></el-icon>
                        </el-avatar>
                        <div class="chat-partner-info">
                            <div class="chat-partner-name">{{ partnerName }}</div>
                            <div class="chat-partner-role">{{ isOwnerMode ? '我是卖家' : '我是买家（联系卖家）' }}</div>
                        </div>
                    </div>

                    <div ref="bodyEl" class="chat-body">
                        <div v-if="!messages.length" class="chat-empty">
                            还没有消息，可以先给对方发一句话打个招呼～
                        </div>
                        <div v-for="(m, i) in messages" :key="i" class="msg" :class="isMine(m) ? 'mine' : 'other'">
                            <div class="bubble">{{ m.content }}</div>
                            <div class="time">{{ shortTime(m.createTime) }}</div>
                        </div>
                    </div>

                    <div class="chat-input">
                        <el-input v-model="draft" type="textarea" :rows="2" resize="none" maxlength="500"
                            show-word-limit placeholder="输入内容，Enter 发送（Shift+Enter 换行）"
                            @keydown.enter.exact.prevent="send"></el-input>
                        <el-button type="primary" @click="send">发送</el-button>
                    </div>
                </div>
                <div v-else class="chat-empty big">加载会话中…</div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 买卖双方私信窗口（按商品会话，下单前/后均可）
 * 支持两种入口：/chat?orderId=…（从订单进）与 /chat?itemId=…&otherUserId=…（从商品详情“联系卖家”进）
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { imageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

export default {
    name: 'chat',
    components: { AppHead, AppBody, AppFoot },
    data() {
        return {
            userStore: null,
            ready: false,
            itemId: 0,
            otherUserId: 0,
            isOwnerMode: false,
            itemName: '',
            itemImg: '',
            orderNumber: '',
            partner: null,
            messages: [],
            draft: '',
            pollTimer: null
        };
    },
    computed: {
        me() {
            return this.userStore && this.userStore.userInfo ? this.userStore.userInfo.id : 0;
        },
        // isOwnerMode：当前用户是否为该商品卖家（在 resolveFromOrder 中设置，仅用于文案）
        partnerName() {
            if (this.partner && this.partner.nickname) return this.partner.nickname;
            return this.otherUserId ? '用户 #' + this.otherUserId : '对方';
        },
        partnerAvatar() {
            const a = this.partner && this.partner.avatar;
            return a ? imageUrl(a) : '';
        }
    },
    created() {
        this.userStore = useUserStore();
        const orderId = this.$route.query.orderId;
        const itemId = this.$route.query.itemId;
        if (orderId) {
            this.resolveFromOrder(orderId);
        } else if (itemId) {
            this.itemId = Number(itemId);
            this.otherUserId = Number(this.$route.query.otherUserId || 0);
            if (!this.otherUserId) {
                this.$message.error('缺少会话对象');
                return;
            }
            this.loadItemInfo();
        } else {
            this.$message.error('缺少会话参数');
        }
    },
    mounted() {
        this.pollTimer = setInterval(() => this.loadMessages(false), 3000);
    },
    beforeUnmount() {
        clearInterval(this.pollTimer);
    },
    methods: {
        resolveFromOrder(orderId) {
            api.getOrder({ id: orderId }).then(res => {
                if (res.status_code !== 1 || !res.data) {
                    this.$message.error(res.msg || '无权查看该订单');
                    return;
                }
                const o = res.data;
                const item = o.idleItem || {};
                this.itemId = item.id || o.idleId;
                this.orderNumber = o.orderNumber || '';
                // 我是买家→对方是卖家(item.userId)；我是卖家→对方是买家(o.userId)
                const me = Number(this.userStore.userInfo.id);
                this.otherUserId = Number(me === Number(o.userId) ? item.userId : o.userId);
                this.isOwnerMode = me === Number(item.userId);
                this.itemName = item.idleName || '未知商品';
                this.loadPartner();
                this.start();
            }).catch(() => { });
        },
        loadItemInfo() {
            api.getIdleItem({ id: this.itemId }).then(res => {
                if (res.status_code === 1 && res.data) {
                    const it = res.data;
                    this.itemName = it.idleName || '';
                    let list = [];
                    try { list = JSON.parse(it.pictureList || '[]'); } catch (e) { list = []; }
                    this.itemImg = list.length ? list[0] : '';
                }
            }).catch(() => { });
            this.loadPartner();
            this.start();
        },
        loadPartner() {
            if (!this.otherUserId) return;
            api.getUserInfo({ id: this.otherUserId }).then(res => {
                if (res.status_code === 1 && res.data) {
                    this.partner = res.data;
                }
            }).catch(() => { });
        },
        start() {
            this.ready = true;
            this.loadMessages(true);
        },
        loadMessages(forceScroll) {
            if (!this.itemId || !this.otherUserId) return;
            api.chatMessages({ itemId: this.itemId, otherUserId: this.otherUserId, limit: 100 }).then(res => {
                if (res.status_code === 1) {
                    const before = this.messages.length;
                    this.messages = res.data || [];
                    if (forceScroll || this.messages.length !== before) this.scrollBottom();
                }
            }).catch(() => { });
        },
        send() {
            const text = (this.draft || '').trim();
            if (!text || !this.itemId || !this.otherUserId) return;
            this.draft = '';
            api.chatSend({ itemId: this.itemId, orderId: null, toUserId: this.otherUserId, content: text })
                .then(res => {
                    if (res.status_code === 1) {
                        this.loadMessages(true);
                    } else {
                        this.$message.error(res.msg || '发送失败');
                    }
                }).catch(() => { });
        },
        isMine(m) {
            return String(m.fromUser) === String(this.me);
        },
        shortTime(t) {
            return t ? String(t).substring(11, 16) : '';
        },
        scrollBottom() {
            this.$nextTick(() => {
                const el = this.$refs.bodyEl;
                if (el) el.scrollTop = el.scrollHeight;
            });
        },
        back() {
            if (this.$route.query.from === 'message') {
                this.$router.push({ path: '/message' });
            } else if (this.orderNumber) {
                this.$router.push({ path: '/order', query: { id: this.$route.query.orderId } });
            } else {
                this.$router.push({ path: '/index' });
            }
        }
    }
}
</script>

<style scoped>
.chat-page { min-height: 85vh; display: flex; justify-content: center; padding-top: 24px; }
.chat-panel {
    width: 760px; max-width: 94%; background: #fff;
    border: 1px solid #e6e9ef; border-radius: 12px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.08);
    display: flex; flex-direction: column; height: 74vh;
}
.chat-head { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #eef1f6; }
.chat-sub { font-size: 12px; color: #909399; margin-left: 8px; }
.chat-partner { display: flex; align-items: center; gap: 10px; padding: 10px 16px; background: #f7f9fc; border-bottom: 1px solid #eef1f6; }
.chat-partner-name { font-weight: 600; font-size: 15px; }
.chat-partner-role { font-size: 12px; color: #909399; }
.chat-body { flex: 1; overflow-y: auto; padding: 16px; background: #fbfcfe; }
.chat-empty { color: #a0a3ab; font-size: 13px; text-align: center; padding: 24px 0; }
.chat-empty.big { font-size: 16px; margin-top: 30vh; }
.msg { margin-bottom: 12px; display: flex; flex-direction: column; }
.msg.mine { align-items: flex-end; }
.msg.other { align-items: flex-start; }
.bubble { max-width: 72%; padding: 8px 12px; border-radius: 10px; font-size: 14px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
.msg.mine .bubble { background: #409eff; color: #fff; border-top-right-radius: 2px; }
.msg.other .bubble { background: #fff; color: #303133; border: 1px solid #e6e9ef; border-top-left-radius: 2px; }
.time { font-size: 11px; color: #b0b3bb; margin-top: 2px; }
.chat-input { display: flex; align-items: flex-end; gap: 8px; padding: 10px 12px; border-top: 1px solid #eef1f6; background: #fff; }
.chat-input .el-button { height: 46px; margin-left: 0; flex-shrink: 0; }
</style>
