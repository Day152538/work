<template>
    <div class="ai-assistant">
        <!-- 右下角悬浮入口（管理后台/管理员登录页不显示） -->
        <div v-if="!visible && !hiddenByRoute" class="ai-fab" @click="openPanel">
            <span class="ai-fab-ico">🤖</span>
            <span class="ai-fab-text">AI 助手</span>
        </div>

        <!-- 聊天面板 -->
        <div v-if="visible" class="ai-panel">
            <div class="ai-head">
                <b class="ai-head-title">{{ view === 'release' ? '📦 发布商品' : '🤖 AI 助手' }}</b>
                <div class="ai-head-actions">
                    <el-button v-if="view === 'release'" size="small" text @click="view = 'chat'">← 返回聊天</el-button>
                    <el-button v-if="view === 'chat'" size="small" text @click="newChat">＋ 新对话</el-button>
                    <el-button size="small" text @click="closePanel">✕</el-button>
                </div>
            </div>

            <div ref="bodyEl" class="ai-body">
                <!-- 未登录提示 -->
                <div v-if="!loggedIn" class="ai-login-tip">
                    <p>登录后可以使用 AI 助手：查商品、看行情、管自己的商品、代理发布等。</p>
                    <el-button type="primary" size="small" @click="goLogin">去登录</el-button>
                </div>

                <!-- 发布视图 -->
                <div v-else-if="view === 'release'" class="ai-release">
                    <div class="ai-release-row">
                        <div class="ai-release-label">商品图片（最多 3 张）</div>
                        <div class="ai-release-pics">
                            <div v-for="(p, i) in releasePics" :key="i" class="ai-pic-item">
                                <img :src="imageUrl(p)" alt="" @error="onImgError" />
                                <span class="ai-pic-del" @click="releasePics.splice(i, 1)">✕</span>
                            </div>
                            <div v-if="uploadingCount > 0" class="ai-pic-add ai-pic-uploading">⏳ 上传中…{{ uploadingCount > 1 ? '（剩余 ' + uploadingCount + ' 张）' : '' }}</div>
                            <label v-if="releasePics.length < 3 && uploadingCount === 0" class="ai-pic-add">
                                <input type="file" accept="image/*" multiple style="display:none" @change="onPickPics" />
                                ＋
                            </label>
                        </div>
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">物品名称</div>
                        <el-input v-model="releaseForm.idleName" placeholder="例如：九成新 iphone13 128G" maxlength="60" />
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">商品类别</div>
                        <el-select v-model="releaseForm.idleLabel" placeholder="请选择类别" style="width:100%">
                            <el-option v-for="t in typeOptions" :key="t.id" :label="t.name" :value="t.id" />
                        </el-select>
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">AI 智能预填（可选）</div>
                        <div class="ai-release-ai">
                            <el-button type="primary" plain size="small" :loading="aiLoading" @click="aiPrefillClick">
                                ✨ 根据图片/名称生成描述和参考价
                            </el-button>
                            <el-button size="small" :loading="complianceLoading" @click="aiComplianceClick">🛡 合规自检</el-button>
                        </div>
                        <div v-if="aiDraft.name || aiDraft.details || aiDraft.suggestPrice > 0" class="ai-draft-box">
                            <div class="ai-draft-price">
                                建议价 <b>￥{{ aiDraft.suggestPrice }}</b>
                                <span v-if="aiDraft.priceReason" class="ai-draft-reason">（{{ aiDraft.priceReason }}）</span>
                            </div>
                            <div v-if="aiDraft.name" class="ai-draft-line"><b>推荐名称：</b>{{ aiDraft.name }}</div>
                            <div v-if="aiDraft.details" class="ai-draft-line"><b>推荐描述：</b>{{ aiDraft.details }}</div>
                            <el-button size="small" type="success" plain @click="applyAiDraft">一键应用到下方表单</el-button>
                        </div>
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">价格（元）</div>
                        <el-input-number v-model="releaseForm.idlePrice" :min="0.01" :precision="2" :step="10" style="width:100%" />
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">库存（件）</div>
                        <el-input-number v-model="releaseForm.stock" :min="1" :max="999" style="width:100%" />
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">所在地区</div>
                        <el-input v-model="releaseForm.idlePlace" placeholder="例如：山东工商学院" maxlength="30" />
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">标签（可选，逗号分隔）</div>
                        <el-input v-model="releaseForm.idleTags" placeholder="例如：九成新, 可小刀, 教材" maxlength="100" />
                    </div>
                    <div class="ai-release-row">
                        <div class="ai-release-label">商品描述</div>
                        <el-input v-model="releaseForm.idleDetails" type="textarea" :rows="3" resize="none" maxlength="500" placeholder="成色、入手渠道、转让原因…" />
                    </div>
                    <div v-if="complianceResult.riskLevel" class="ai-compliance-box" :class="'lv-' + complianceResult.riskLevel">
                        <div>{{ complianceTitle }}</div>
                        <div v-if="complianceResult.reasons.length" class="ai-compliance-reasons">
                            <div v-for="(r, i) in complianceResult.reasons" :key="i">· {{ r }}</div>
                        </div>
                    </div>
                    <div class="ai-release-submit">
                        <el-button type="primary" :loading="submitting" style="width:100%" @click="submitRelease">确认发布</el-button>
                    </div>
                </div>

                <!-- 聊天视图 -->
                <div v-else class="ai-chat">
                    <div v-if="!messages.length" class="ai-empty">
                        <p class="ai-empty-tip">我是闲品校园CampusTrade的 AI 助手，可以帮你：</p>
                        <div class="ai-chips">
                            <div v-for="c in chips" :key="c.key" class="ai-chip" @click="onChip(c)">
                                {{ c.label }}
                            </div>
                        </div>
                        <p class="ai-empty-example">也可以直接问我，例如：</p>
                        <p class="ai-empty-example">· 现在有哪些手机在卖？预算 1000 以内</p>
                        <p class="ai-empty-example">· 书桌最近成交价大概多少？</p>
                        <p class="ai-empty-example">· 我发布过哪些商品？我的收藏有哪些？</p>
                    </div>
                    <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role">
                        <div v-if="m.role === 'user'" class="bubble bubble-user">{{ m.content }}</div>
                        <div v-else-if="m.role === 'assistant'" class="bubble bubble-ai">
                            <template v-if="m.streaming && !m.content">正在思考…</template>
                            <template v-else>
                                <template v-for="(seg, si) in parseReply(m.content)" :key="si">
                                    <span v-if="seg.type === 'text'">{{ seg.text }}</span>
                                    <span v-else class="ai-inline-link" @click="goDetails(seg.id)">【{{ seg.text }}】</span>
                                </template>
                            </template>
                            <span v-if="m.streaming && m.content" class="ai-cursor">▍</span>
                        </div>
                        <div v-else-if="m.role === 'card'" class="ai-card">
                            <div v-if="m.cardType === 'goods'" class="ai-card-title">🛍 我的商品（点击按钮操作）</div>
                            <div v-if="m.cardType === 'notice'" class="ai-card-title">📢 平台公告</div>
                            <div v-if="m.cardType === 'stats'" class="ai-card-title">📊 我的统计</div>
                            <div v-if="m.cardType === 'orders'" class="ai-card-title">🧾 我的订单</div>
                            <div v-if="m.cardType === 'favs'" class="ai-card-title">⭐ 我的收藏</div>

                            <!-- 商品管理卡 -->
                            <div v-if="m.cardType === 'goods'" class="ai-goods-list">
                                <div v-for="g in m.data" :key="g.id" class="ai-goods-item">
                                    <img class="ai-goods-img" :src="g.imgUrl" alt="" />
                                    <div class="ai-goods-info">
                                        <div class="ai-goods-name">{{ g.idleName }}</div>
                                        <div class="ai-goods-meta">
                                            <span class="ai-goods-price">￥{{ g.idlePrice }}</span>
                                            <el-tag size="small" :type="statusTagType(g.idleStatus)">{{ statusText(g.idleStatus) }}</el-tag>
                                        </div>
                                        <div class="ai-goods-ops">
                                            <el-button v-if="g.idleStatus === 1" size="small" plain type="warning" @click="changeStatus(g, 2)">下架</el-button>
                                            <el-button v-if="g.idleStatus === 2" size="small" plain type="success" @click="changeStatus(g, 1)">上架</el-button>
                                            <el-button size="small" plain type="primary" @click="goDetails(g.id)">详情/编辑</el-button>
                                            <el-button v-if="g.idleStatus === 2 || g.idleStatus === 4" size="small" plain type="danger" @click="removeGood(g)">删除</el-button>
                                        </div>
                                    </div>
                                </div>
                                <div v-if="!m.data.length" class="ai-card-empty">还没有发布任何商品，点上方「📦 发布商品」试试</div>
                            </div>

                            <!-- 通知卡 -->
                            <div v-else-if="m.cardType === 'notice'" class="ai-notice-list">
                                <div v-for="n in m.data" :key="n.id" class="ai-notice-item">{{ n.content }}</div>
                                <div v-if="!m.data.length" class="ai-card-empty">平台当前暂无公告</div>
                            </div>

                            <!-- 统计卡 -->
                            <div v-else-if="m.cardType === 'stats'" class="ai-stats">
                                <div class="ai-stats-row">
                                    <span>在售 <b>{{ m.data.onSale }}</b></span>
                                    <span>待审核 <b>{{ m.data.pending }}</b></span>
                                    <span>违规 <b>{{ m.data.violated }}</b></span>
                                    <span>已下架 <b>{{ m.data.off }}</b></span>
                                </div>
                                <div class="ai-stats-sold">已支付卖出 <b>{{ m.data.soldCount }}</b> 单，累计成交 <b>￥{{ m.data.soldAmount }}</b></div>
                            </div>

                            <!-- 订单卡 -->
                            <div v-else-if="m.cardType === 'orders'" class="ai-order-list">
                                <div v-for="o in m.data" :key="o.key" class="ai-order-item">
                                    <span class="ai-order-name">{{ o.idleName }}</span>
                                    <span class="ai-order-price">￥{{ o.orderPrice }}</span>
                                    <el-tag size="small" :type="o.statusType">{{ o.statusText }}</el-tag>
                                </div>
                                <div v-if="!m.data.length" class="ai-card-empty">暂无订单记录</div>
                            </div>

                            <!-- 收藏卡 -->
                            <div v-else-if="m.cardType === 'favs'" class="ai-fav-list">
                                <div v-for="f in m.data" :key="f.idleId" class="ai-fav-item" @click="goDetails(f.idleId)">
                                    <span class="ai-fav-name">{{ f.idleName }}</span>
                                    <span class="ai-goods-price">￥{{ f.idlePrice }}</span>
                                </div>
                                <div v-if="!m.data.length" class="ai-card-empty">你还没有收藏任何商品</div>
                            </div>
                        </div>
                    </div>
                    <div v-if="loading && !streamingActive" class="msg assistant">
                        <div class="bubble bubble-ai">思考中…</div>
                    </div>
                </div>
            </div>

            <!-- 底部输入区 -->
            <div v-if="loggedIn" class="ai-foot">
                <template v-if="view === 'chat'">
                    <div class="ai-web-row">
                        <span :class="['ai-web-toggle', { on: webSearchOn }]" @click="webSearchOn = !webSearchOn">
                            🌐 联网搜索{{ webSearchOn ? '：已开' : '' }}
                        </span>
                    </div>
                    <div class="ai-input-row">
                        <label class="ai-upload-btn" title="上传图片">
                            <input type="file" accept="image/*" style="display:none" @change="onChatPickPic" />
                            🖼
                        </label>
                        <el-input v-model="draft" placeholder="输入问题，回车发送" @keydown.enter.exact.prevent="send" />
                        <el-button type="primary" :loading="loading" @click="send">发送</el-button>
                    </div>
                </template>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * AI 悬浮助手（2026-09 新增）
 * - 右下角悬浮入口 + 聊天面板；对话逻辑与 /ai-chat 页一致（会话持久化 + 可开联网）
 * - 平台服务（仅登录用户）：
 *   ① 代理发布：传图/名称 → AI 预填草稿 → 合规自检 → 表单确认后调 /idle/add
 *   ② 我的商品：列表 + 上架/下架/删除（确认制，调现有接口）+ 跳详情编辑
 *   ③ 平台通知 / 我的统计 / 我的订单 / 我的收藏：卡片式查询
 * - 安全：所有写操作（发布/上下架/删除）均需用户点击确认；AI 只做查询与引导，不直接改数据
 */
import api from '@/api'
import { imageUrl } from '@/utils/request'
import { compressImageFile } from '@/utils/image-compress'

// 兼容旧数据：后端上传返回 "/image?imageName=xxx"，剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

const ORDER_STATUS = ['待付款', '待发货', '待收货', '已完成', '已取消']
const MAX_PICS = 3

export default {
    name: 'AiAssistant',
    data() {
        return {
            imageUrl,               // 模板中拼图片地址（须挂到实例上才能用）
            visible: false,
            hiddenByRoute: false,
            view: 'chat',           // chat | release
            loggedIn: false,
            // 对话
            draft: '',
            loading: false,
            webSearchOn: false,
            conversationId: localStorage.getItem('ai_conv') || '',
            messages: [],
            // 分类
            typeOptions: [],
            // 发布表单
            releaseForm: {
                idleName: '',
                idleLabel: '',
                idlePrice: 0,
                stock: 1,
                idlePlace: '',
                idleTags: '',
                idleDetails: ''
            },
            releasePics: [],
            uploadingCount: 0,
            aiLoading: false,
            aiDraft: { name: '', details: '', labelId: null, suggestPrice: 0, priceReason: '' },
            complianceLoading: false,
            complianceResult: { riskLevel: '', reasons: [] },
            submitting: false,
            chips: [
                { label: '📦 发布商品', key: 'release' },
                { label: '🛍 我的商品', key: 'goods' },
                { label: '📢 平台通知', key: 'notice' },
                { label: '📊 我的统计', key: 'stats' },
                { label: '🧾 我的订单', key: 'orders' },
                { label: '⭐ 我的收藏', key: 'favs' }
            ]
        };
    },
    computed: {
        // 当前是否有流式回复气泡正在接收增量（有则不显示独立的"思考中…"气泡）
        streamingActive() {
            return this.messages.some((m) => m.streaming);
        },
        complianceTitle() {
            const lv = this.complianceResult.riskLevel;
            if (lv === 'HIGH') return '⚠ 高风险：建议修改后再发布';
            if (lv === 'MEDIUM') return '⚡ 中风险：请重点核对后再发布';
            if (lv === 'LOW') return '✅ 低风险：内容基本合规';
            return '';
        }
    },
    mounted() {
        // 页头“AI 助手”入口通过自定义事件打开悬浮面板（AppHeader.toAiChat 已改为派发该事件）
        window.addEventListener('open-ai-assistant', this.openPanel);
        this.loggedIn = !!localStorage.getItem('token');
        if (this.loggedIn) {
            this.listTypes();
        }
        this.applyRouteRule(this.$route);
    },
    beforeUnmount() {
        window.removeEventListener('open-ai-assistant', this.openPanel);
    },
    watch: {
        '$route'(to) {
            this.applyRouteRule(to);
        }
    },
    methods: {
        // 管理后台/管理员登录页不使用用户端 AI 助手：隐藏悬浮入口
        applyRouteRule(route) {
            this.hiddenByRoute = !!(route && (route.path.startsWith('/platform-admin') || route.path.startsWith('/login-admin')));
            if (this.hiddenByRoute) {
                this.visible = false;
            }
        },
        // ==================== 面板开关 ====================
        openPanel() {
            this.loggedIn = !!localStorage.getItem('token');
            if (this.loggedIn) {
                this.listTypes();
                if (!this.messages.length && this.conversationId) {
                    this.loadHistory();
                }
            }
            this.visible = true;
        },
        closePanel() {
            this.visible = false;
            this.view = 'chat';
        },
        goLogin() {
            this.$router.push('/login');
        },
        goDetails(id) {
            this.$router.push({ path: '/details', query: { id } });
            this.closePanel();
        },

        // ==================== 对话 ====================
        // 流式对话（SSE）：逐块 append 到当前气泡（打字机效果），结束后整体解析商品链接
        send() {
            const text = (this.draft || '').trim();
            if (!text || this.loading) return;
            this.messages.push({ role: 'user', content: text });
            this.draft = '';
            this.scrollBottom();
            this.loading = true;
            // 先插入一个空的 assistant 气泡接收增量
            this.messages.push({ role: 'assistant', content: '', streaming: true });
            api.aiChatStream({
                message: text,
                conversationId: this.conversationId || undefined,
                webSearch: this.webSearchOn
            }, {
                onDelta: (delta) => {
                    const last = this.messages[this.messages.length - 1];
                    if (last && last.role === 'assistant' && last.streaming) {
                        last.content += delta;
                        this.scrollBottom();
                    }
                },
                onDone: (conversationId) => {
                    if (conversationId) {
                        this.conversationId = conversationId;
                        localStorage.setItem('ai_conv', this.conversationId);
                    }
                    const last = this.messages[this.messages.length - 1];
                    if (last && last.role === 'assistant' && last.streaming) {
                        last.streaming = false;
                        if (!last.content.trim()) {
                            last.content = '（助手没有返回内容，请重试）';
                        }
                    }
                },
                onError: (msg) => {
                    const last = this.messages[this.messages.length - 1];
                    if (last && last.role === 'assistant' && last.streaming) {
                        if (!last.content) {
                            // 一个字都没收到：直接移除空气泡
                            this.messages.pop();
                        } else {
                            // 已收到部分内容：保留并结束流式态
                            last.streaming = false;
                        }
                    }
                    this.$message.error(msg || 'AI 回复失败，请稍后重试');
                }
            }).catch((err) => {
                // HTTP 层错误（401 等）已由 api 层提示；这里兜底
                if (err && err.message && err.message.indexOf('登录已过期') === -1) {
                    this.$message.error(err.message || 'AI 回复失败，请稍后重试');
                }
            }).finally(() => {
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
            }).catch(() => {});
        },
        newChat() {
            this.messages = [];
            this.conversationId = '';
            localStorage.removeItem('ai_conv');
        },
        // 解析 AI 回复中的商品标注（【名称 id=3】/（名称 id=3）/【id=3】等变体）→ 可点击片段
        // 只用 v-for 片段渲染，不用 v-html，防 XSS
        parseReply(text) {
            const re = /(【[^】]*?id\s*[=:：]\s*\d+】|（[^）]*?id\s*[=:：]\s*\d+）|\([^)]*?id\s*[=:：]\s*\d+\))/g;
            const parts = [];
            let last = 0, m;
            while ((m = re.exec(text))) {
                if (m.index > last) parts.push({ type: 'text', text: text.slice(last, m.index) });
                const block = m[1];
                const idMatch = /id\s*[=:：]\s*(\d+)/i.exec(block);
                const id = idMatch ? Number(idMatch[1]) : 0;
                const name = block.replace(/id\s*[=:：]\s*\d+/i, '').replace(/[【】（）()]/g, '').trim();
                parts.push({ type: 'link', text: name || ('商品#' + id), id });
                last = m.index + m[0].length;
            }
            if (last < text.length) parts.push({ type: 'text', text: text.slice(last) });
            return parts.length ? parts : [{ type: 'text', text }];
        },
        scrollBottom() {
            this.$nextTick(() => {
                const el = this.$refs.bodyEl;
                if (el) el.scrollTop = el.scrollHeight;
            });
        },

        // ==================== 快捷功能 ====================
        onChip(c) {
            if (!this.loggedIn) {
                this.$message.warning('请先登录');
                return;
            }
            if (c.key === 'release') {
                this.view = 'release';
                return;
            }
            this.messages.push({ role: 'user', content: c.label });
            this.pushLoadingCard(c.key);
            this.fetchCard(c.key);
        },
        pushLoadingCard(key) {
            const titleMap = {
                goods: '🛍 我的商品（点击按钮操作）',
                notice: '📢 平台公告',
                stats: '📊 我的统计',
                orders: '🧾 我的订单',
                favs: '⭐ 我的收藏'
            };
            this.messages.push({ role: 'card', cardType: key, title: titleMap[key], data: [], loading: true });
            this.scrollBottom();
        },
        updateCard(key, data) {
            const idx = [...this.messages].reverse().findIndex((m) => m.role === 'card' && m.cardType === key);
            if (idx === -1) return;
            const realIdx = this.messages.length - 1 - idx;
            this.messages[realIdx].data = data || [];
            this.messages[realIdx].loading = false;
            this.scrollBottom();
        },
        fetchCard(key) {
            const done = (data) => this.updateCard(key, data);
            if (key === 'goods') {
                api.getAllIdleItem().then((res) => {
                    if (res.status_code === 1) {
                        done((res.data || []).map((g) => ({
                            id: g.id,
                            idleName: g.idleName,
                            idlePrice: g.idlePrice,
                            idleStatus: g.idleStatus,
                            imgUrl: this.firstPic(g.pictureList)
                        })));
                    } else {
                        done([]);
                        this.$message.error(res.msg || '获取商品失败');
                    }
                }).catch(() => {});
            } else if (key === 'notice') {
                api.getNotice().then((res) => {
                    if (res.status_code === 1) done(res.data || []);
                    else done([]);
                }).catch(() => {});
            } else if (key === 'stats') {
                Promise.all([api.getAllIdleItem(), api.getMySoldIdle()])
                    .then(([g, o]) => {
                        const list = (g.status_code === 1 && g.data) ? g.data : [];
                        let onSale = 0, pending = 0, violated = 0, off = 0;
                        list.forEach((it) => {
                            const s = it.idleStatus;
                            if (s === 1) onSale++;
                            else if (s === 2) off++;
                            else if (s === 3) pending++;
                            else if (s === 4) violated++;
                        });
                        const sold = (o.status_code === 1 && Array.isArray(o.data)) ? o.data : [];
                        let soldCount = 0, soldAmount = 0;
                        sold.forEach((it) => {
                            const st = it.orderStatus;
                            if (st >= 1 && st <= 3) {
                                soldCount++;
                                soldAmount += Number(it.orderPrice) || 0;
                            }
                        });
                        done({ onSale, pending, violated, off, soldCount, soldAmount: soldAmount.toFixed(2) });
                    }).catch(() => {});
            } else if (key === 'orders') {
                Promise.all([api.getMyOrder(), api.getMySoldIdle()])
                    .then(([b, s]) => {
                        const rows = [];
                        const push = (list, isSold) => {
                            if (!list || !Array.isArray(list)) return;
                            list.forEach((it) => {
                                const st = it.orderStatus;
                                rows.push({
                                    key: (isSold ? 's' : 'b') + it.id,
                                    idleName: it.idleItem && it.idleItem.idleName ? it.idleItem.idleName : '（订单#' + it.id + '）',
                                    orderPrice: it.orderPrice,
                                    statusText: ORDER_STATUS[st] || '未知',
                                    statusType: st === 0 ? 'warning' : (st === 4 ? 'info' : 'success')
                                });
                            });
                        };
                        push(b.data, false);
                        push(s.data, true);
                        done(rows);
                    }).catch(() => {});
            } else if (key === 'favs') {
                api.getMyFavorite().then((res) => {
                    if (res.status_code === 1) {
                        done((res.data || []).map((f) => {
                            const item = f.idleItem || {};
                            return { idleId: f.idleId, idleName: item.idleName || '商品', idlePrice: item.idlePrice };
                        }));
                    } else {
                        done([]);
                    }
                }).catch(() => {});
            }
        },

        // ==================== 商品管理 ====================
        firstPic(pictureList) {
            try {
                const arr = JSON.parse(pictureList || '[]');
                return arr.length ? imageUrl(arr[0]) : '';
            } catch (e) {
                return '';
            }
        },
        statusText(s) {
            return s === 1 ? '在售' : s === 2 ? '已下架' : s === 3 ? '待审核' : s === 4 ? '违规' : '未知';
        },
        statusTagType(s) {
            return s === 1 ? 'success' : s === 3 ? 'warning' : s === 4 ? 'danger' : 'info';
        },
        changeStatus(g, toStatus) {
            const action = toStatus === 1 ? '上架' : '下架';
            this.$confirm(`确定要${action}「${g.idleName}」吗？`, '操作确认', {
                confirmButtonText: action,
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                api.updateIdleItem({ id: g.id, idleStatus: toStatus }).then((res) => {
                    if (res.status_code === 1) {
                        this.$message.success(`${action}成功`);
                        this.refreshGoodsCard(g.id);
                    } else {
                        this.$message.error(res.msg || `${action}失败`);
                    }
                }).catch(() => {});
            }).catch(() => {});
        },
        removeGood(g) {
            this.$confirm(`删除后不可恢复，确定删除「${g.idleName}」吗？`, '删除确认', {
                confirmButtonText: '删除',
                cancelButtonText: '取消',
                type: 'error'
            }).then(() => {
                api.deleteGood(g.id).then((res) => {
                    if (res.status_code === 1) {
                        this.$message.success('已删除');
                        this.refreshGoodsCard(g.id);
                    } else {
                        this.$message.error(res.msg || '删除失败');
                    }
                }).catch(() => {});
            }).catch(() => {});
        },
        refreshGoodsCard(id) {
            const idx = [...this.messages].reverse().findIndex((m) => m.role === 'card' && m.cardType === 'goods');
            if (idx === -1) return;
            api.getAllIdleItem().then((res) => {
                if (res.status_code === 1) {
                    const realIdx = this.messages.length - 1 - idx;
                    this.messages[realIdx].data = (res.data || []).map((g) => ({
                        id: g.id,
                        idleName: g.idleName,
                        idlePrice: g.idlePrice,
                        idleStatus: g.idleStatus,
                        imgUrl: this.firstPic(g.pictureList)
                    }));
                }
            }).catch(() => {});
        },

        // ==================== 发布流程 ====================
        listTypes() {
            if (this.typeOptions.length) return;
            api.listType({ begin: 0, size: 999 }).then((res) => {
                if (res.status_code === 1) {
                    this.typeOptions = (res.data || []).map((t) => ({ id: t.id, name: t.name }));
                }
            }).catch(() => {});
        },
        onPickPics(e) {
            const files = Array.from(e.target.files || []);
            e.target.value = '';
            if (!files.length) return;
            const rest = MAX_PICS - this.releasePics.length;
            if (files.length > rest) {
                this.$message.warning(`最多上传 ${MAX_PICS} 张图片`);
            }
            // 串行队列上传：逐张压缩→上传→加入列表，多选不丢图
            const todo = files.slice(0, rest);
            const uploadNext = () => {
                const f = todo.shift();
                if (!f) return;
                this.uploadOne(f).finally(uploadNext);
            };
            uploadNext();
        },
        uploadOne(file) {
            this.uploadingCount++;
            // 压缩 + 上传（axios 自身有 10s 超时，但 DNS/连接挂起阶段不一定被覆盖）
            const work = (async () => {
                const compressed = await compressImageFile(file);
                const res = await api.uploadFile(compressed);
                if (res.status_code === 1) {
                    this.releasePics.push(toImageName(res.data));
                } else {
                    this.$message.error(res.msg || '图片上传失败');
                }
            })();
            // 强制超时兜底：20s 内无论卡在哪一步都结束并提示，避免"一直显示上传中"
            const timeout = new Promise((resolve) => {
                setTimeout(() => resolve('timeout'), 20000);
            });
            return Promise.race([work.then(() => 'ok'), timeout])
                .then((r) => {
                    if (r === 'timeout') {
                        this.$message.error('上传超时，请确认后端服务已启动、图片未过大');
                    }
                })
                .catch(() => {
                    this.$message.error('图片上传失败，请重试');
                })
                .finally(() => {
                    this.uploadingCount--;
                });
        },
        onImgError(ev) {
            // 图片加载失败：显示占位，避免空白
            ev.target.style.background = '#eef1f6';
            ev.target.style.padding = '0';
            ev.target.alt = '加载失败';
        },
        confirmSendImages() {
            if (!this.releasePics.length) return Promise.resolve(true);
            return this.$confirm(
                '图片将被发送至第三方 AI 模型（云服务）用于识别/审查。若图中含人脸、证件、他人手机号等隐私信息，请勿发送。是否继续？',
                '隐私提示', { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
            ).then(() => true).catch(() => false);
        },
        async aiPrefillClick() {
            const title = (this.releaseForm.idleName || '').trim();
            const hasImg = this.releasePics.length > 0;
            if (!title && !hasImg) {
                this.$message.warning('请先填写物品名称或上传至少一张照片，AI 才能生成草稿');
                return;
            }
            if (hasImg && !(await this.confirmSendImages())) return;
            this.aiLoading = true;
            api.aiPrefill({
                title,
                labelId: this.releaseForm.idleLabel || null,
                imageNames: this.releasePics.slice(0, MAX_PICS)
            }).then((res) => {
                if (res.status_code === 1 && res.data) {
                    const d = res.data;
                    this.aiDraft = {
                        name: d.name || '',
                        details: d.details || '',
                        labelId: (d.labelId != null && d.labelId !== '') ? Number(d.labelId) : null,
                        suggestPrice: parseFloat(d.suggestPrice) > 0 ? parseFloat(d.suggestPrice) : 0,
                        priceReason: d.priceReason || ''
                    };
                    this.$message.success('AI 草稿已生成，可一键应用或手动修改');
                } else {
                    this.$message.error(res.msg || 'AI 生成失败，可手动填写后发布');
                }
            }).catch(() => {}).finally(() => { this.aiLoading = false; });
        },
        applyAiDraft() {
            const d = this.aiDraft;
            if (d.name) this.releaseForm.idleName = d.name;
            if (d.labelId != null && d.labelId !== '') this.releaseForm.idleLabel = d.labelId;
            if (d.suggestPrice > 0) this.releaseForm.idlePrice = d.suggestPrice;
            if (d.details) this.releaseForm.idleDetails = d.details;
            this.$message.success('已应用到表单，请核对后发布');
        },
        async aiComplianceClick() {
            const title = (this.releaseForm.idleName || '').trim();
            const details = (this.releaseForm.idleDetails || '').trim();
            if (!title && !details && !this.releasePics.length) {
                this.$message.warning('请先填写标题/描述或上传照片，再进行合规自检');
                return;
            }
            if (this.releasePics.length && !(await this.confirmSendImages())) return;
            this.complianceLoading = true;
            api.aiCompliance({
                title,
                details,
                imageNames: this.releasePics.slice(0, MAX_PICS)
            }).then((res) => {
                if (res.status_code === 1 && res.data) {
                    this.complianceResult = {
                        riskLevel: (res.data.riskLevel || 'LOW').toUpperCase(),
                        reasons: Array.isArray(res.data.reasons) ? res.data.reasons : []
                    };
                } else {
                    this.$message.error(res.msg || '合规自检暂不可用，可继续手动发布');
                }
            }).catch(() => {}).finally(() => { this.complianceLoading = false; });
        },
        submitRelease() {
            const f = this.releaseForm;
            if (!f.idleName || !f.idleName.trim()) { this.$message.error('请输入物品名称'); return; }
            if (!f.idleLabel) { this.$message.error('请选择商品类别'); return; }
            if (!f.idlePrice || f.idlePrice <= 0) { this.$message.error('请输入正确的价格'); return; }
            if (!f.stock || f.stock < 1) { this.$message.error('库存至少为 1 件'); return; }
            if (!f.idleDetails || !f.idleDetails.trim()) { this.$message.error('请输入物品描述'); return; }
            if (!this.releasePics.length) { this.$message.error('请至少上传一张物品照片'); return; }

            this.submitting = true;
            const payload = {
                idleName: f.idleName.trim(),
                idleDetails: f.idleDetails.trim(),
                pictureList: JSON.stringify(this.releasePics),
                idlePrice: f.idlePrice,
                idlePlace: f.idlePlace || '校内',
                idleLabel: f.idleLabel,
                idleTags: f.idleTags,
                stock: f.stock
            };
            api.addIdleItem(payload).then((res) => {
                if (res.status_code === 1) {
                    this.$message.success('发布成功！');
                    this.resetRelease();
                    this.view = 'chat';
                    this.messages.push({ role: 'assistant', content: `🎉 发布成功！商品「${payload.idleName}」已提交${f.idleLabel ? '' : ''}，可在「🛍 我的商品」中查看状态。` });
                    this.scrollBottom();
                } else {
                    this.$message.error('发布失败！' + (res.msg || ''));
                }
            }).catch(() => {
                this.$message.error('发布失败，请稍后重试');
            }).finally(() => { this.submitting = false; });
        },
        resetRelease() {
            this.releaseForm = {
                idleName: '', idleLabel: '', idlePrice: 0, stock: 1,
                idlePlace: '', idleTags: '', idleDetails: ''
            };
            this.releasePics = [];
            this.aiDraft = { name: '', details: '', labelId: null, suggestPrice: 0, priceReason: '' };
            this.complianceResult = { riskLevel: '', reasons: [] };
        },

        // ==================== 聊天内图片 ====================
        onChatPickPic(e) {
            const files = Array.from(e.target.files || []);
            e.target.value = '';
            if (!files.length) return;
            // 聊天内上传：自动进入发布视图，逐张排队上传
            this.view = 'release';
            this.$message.success('已进入发布页，可继续添加图片或点「✨ 生成草稿」让 AI 识别');
            const todo = files.slice(0, MAX_PICS);
            const uploadNext = () => {
                const f = todo.shift();
                if (!f) return;
                this.uploadOne(f).finally(uploadNext);
            };
            uploadNext();
        }
    }
};
</script>

<style scoped>
/* ============ 悬浮按钮 ============ */
.ai-fab {
    position: fixed;
    right: 24px;
    bottom: 28px;
    z-index: 3000;
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 10px 16px;
    border-radius: 999px;
    background: linear-gradient(135deg, #409eff, #66b1ff);
    color: #fff;
    font-size: 14px;
    cursor: pointer;
    box-shadow: 0 4px 14px rgba(64, 158, 255, 0.35);
    transition: transform 0.15s ease;
    user-select: none;
}
.ai-fab:hover {
    transform: translateY(-2px);
}
.ai-fab-ico {
    font-size: 18px;
}

/* ============ 面板 ============ */
.ai-panel {
    position: fixed;
    right: 20px;
    bottom: 20px;
    z-index: 3001;
    width: 420px;
    max-width: calc(100vw - 20px);
    height: 620px;
    max-height: calc(100vh - 40px);
    background: #fff;
    border: 1px solid #e6e9ef;
    border-radius: 14px;
    box-shadow: 0 8px 30px rgba(0, 0, 0, 0.16);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    font-family: 'PingFang SC', 'Segoe UI', Arial, sans-serif;
}

.ai-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 14px;
    border-bottom: 1px solid #eef1f6;
    background: #fafcff;
    flex-shrink: 0;
}
.ai-head-title {
    font-size: 15px;
    color: #303133;
}
.ai-head-actions {
    display: flex;
    align-items: center;
    gap: 2px;
}

.ai-body {
    flex: 1;
    overflow-y: auto;
    padding: 14px;
}

/* ============ 登录提示 ============ */
.ai-login-tip {
    text-align: center;
    color: #606266;
    font-size: 14px;
    line-height: 1.8;
    margin-top: 60px;
}
.ai-login-tip p {
    margin-bottom: 16px;
}

/* ============ 聊天消息 ============ */
.ai-chat {
    min-height: 100%;
}
.ai-empty {
    color: #909399;
    font-size: 13px;
}
.ai-empty-tip {
    font-size: 14px;
    color: #606266;
    margin-bottom: 10px;
}
.ai-empty-example {
    line-height: 1.9;
    color: #a0a3ab;
    margin-top: 4px;
}
.ai-chips {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 12px;
}
.ai-chip {
    padding: 6px 12px;
    border-radius: 999px;
    background: #f0f7ff;
    border: 1px solid #cfe4ff;
    color: #2d6cdf;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.15s;
    user-select: none;
}
.ai-chip:hover {
    background: #409eff;
    color: #fff;
    border-color: #409eff;
}

.msg {
    margin-bottom: 12px;
    display: flex;
}
.msg.user {
    justify-content: flex-end;
}
.msg.assistant {
    justify-content: flex-start;
}
.bubble {
    max-width: 86%;
    padding: 9px 12px;
    border-radius: 10px;
    font-size: 14px;
    line-height: 1.65;
    white-space: pre-wrap;
    word-break: break-word;
}
.bubble-user {
    background: #409eff;
    color: #fff;
    border-top-right-radius: 2px;
}
.bubble-ai {
    background: #f2f6fc;
    color: #303133;
    border-top-left-radius: 2px;
}
.ai-inline-link {
    color: #409eff;
    font-weight: 600;
    cursor: pointer;
    text-decoration: underline;
    text-underline-offset: 3px;
}
.ai-inline-link:hover {
    color: #66b1ff;
}
/* 流式输出光标 */
.ai-cursor {
    display: inline-block;
    margin-left: 2px;
    color: #409eff;
    font-weight: 600;
    animation: ai-blink 1s steps(2, start) infinite;
}
@keyframes ai-blink {
    to {
        visibility: hidden;
    }
}

/* ============ 操作卡片 ============ */
.ai-card {
    width: 100%;
    background: #fafbfd;
    border: 1px solid #e8ecf3;
    border-radius: 10px;
    padding: 12px;
    box-sizing: border-box;
}
.ai-card-title {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 10px;
}
.ai-card-empty {
    color: #909399;
    font-size: 13px;
    padding: 8px 0;
}

.ai-goods-item {
    display: flex;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px dashed #e6e9ef;
}
.ai-goods-item:last-child {
    border-bottom: none;
}
.ai-goods-img {
    width: 56px;
    height: 56px;
    border-radius: 8px;
    object-fit: cover;
    background: #eef1f6;
    flex-shrink: 0;
}
.ai-goods-info {
    flex: 1;
    min-width: 0;
}
.ai-goods-name {
    font-size: 14px;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.ai-goods-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin: 4px 0 6px;
}
.ai-goods-price {
    color: #e6a23c;
    font-weight: 600;
    font-size: 14px;
}
.ai-goods-ops {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
}
.ai-goods-ops .el-button {
    margin-left: 0;
}

.ai-notice-item {
    background: #fff;
    border: 1px solid #eef1f6;
    border-radius: 8px;
    padding: 10px 12px;
    margin-bottom: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #303133;
}

.ai-stats-row {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 18px;
    font-size: 14px;
    color: #606266;
    padding: 8px 4px;
}
.ai-stats-row b {
    color: #409eff;
    font-size: 16px;
    margin-left: 2px;
}
.ai-stats-sold {
    font-size: 13px;
    color: #606266;
    background: #fff;
    border-radius: 8px;
    padding: 10px 12px;
    margin-top: 4px;
}
.ai-stats-sold b {
    color: #e6a23c;
}

.ai-order-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px dashed #e6e9ef;
    font-size: 13px;
}
.ai-order-item:last-child {
    border-bottom: none;
}
.ai-order-name {
    flex: 1;
    min-width: 0;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.ai-order-price {
    color: #e6a23c;
    font-weight: 600;
}

.ai-fav-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 9px 10px;
    background: #fff;
    border: 1px solid #eef1f6;
    border-radius: 8px;
    margin-bottom: 8px;
    cursor: pointer;
    font-size: 14px;
}
.ai-fav-item:hover {
    border-color: #409eff;
}
.ai-fav-name {
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* ============ 发布视图 ============ */
.ai-release {
    padding-bottom: 6px;
}
.ai-release-row {
    margin-bottom: 12px;
}
.ai-release-label {
    font-size: 13px;
    color: #606266;
    margin-bottom: 6px;
}
.ai-release-pics {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}
.ai-pic-item {
    position: relative;
    width: 72px;
    height: 72px;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #e6e9ef;
}
.ai-pic-item img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}
.ai-pic-del {
    position: absolute;
    top: 2px;
    right: 2px;
    width: 18px;
    height: 18px;
    line-height: 16px;
    text-align: center;
    background: rgba(0, 0, 0, 0.55);
    color: #fff;
    border-radius: 50%;
    font-size: 12px;
    cursor: pointer;
}
.ai-pic-add {
    width: 72px;
    height: 72px;
    border: 1px dashed #c0c4cc;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #909399;
    cursor: pointer;
    background: #fafbfd;
}
.ai-pic-add:hover {
    border-color: #409eff;
    color: #409eff;
}
.ai-release-ai {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
}
.ai-draft-box {
    margin-top: 8px;
    background: #f0f9eb;
    border: 1px solid #d9efd0;
    border-radius: 8px;
    padding: 10px 12px;
    font-size: 13px;
    color: #303133;
    line-height: 1.8;
}
.ai-draft-price b {
    color: #e6a23c;
    font-size: 16px;
}
.ai-draft-reason {
    color: #909399;
    font-size: 12px;
}
.ai-draft-box .el-button {
    margin-top: 6px;
}
.ai-compliance-box {
    border-radius: 8px;
    padding: 10px 12px;
    font-size: 13px;
    margin-bottom: 12px;
    line-height: 1.7;
}
.ai-compliance-box.lv-HIGH {
    background: #fef0f0;
    border: 1px solid #fbc4c4;
    color: #c0392b;
}
.ai-compliance-box.lv-MEDIUM {
    background: #fdf6ec;
    border: 1px solid #f3d19e;
    color: #b97a1a;
}
.ai-compliance-box.lv-LOW {
    background: #f0f9eb;
    border: 1px solid #d9efd0;
    color: #3f7d2e;
}
.ai-compliance-reasons {
    margin-top: 4px;
}
.ai-release-submit {
    margin-top: 6px;
}

/* ============ 底部输入区 ============ */
.ai-foot {
    border-top: 1px solid #eef1f6;
    padding: 8px 12px 10px;
    flex-shrink: 0;
    background: #fff;
}
.ai-web-row {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 6px;
}
.ai-web-toggle {
    font-size: 12px;
    color: #909399;
    cursor: pointer;
    user-select: none;
}
.ai-web-toggle.on {
    color: #67c23a;
}
.ai-input-row {
    display: flex;
    align-items: center;
    gap: 6px;
}
.ai-upload-btn {
    font-size: 20px;
    cursor: pointer;
    color: #909399;
    flex-shrink: 0;
    line-height: 1;
    padding: 4px;
}
.ai-upload-btn:hover {
    color: #409eff;
}
.ai-input-row .el-button {
    flex-shrink: 0;
    margin-left: 0;
}

/* ============ 小屏适配 ============ */
@media (max-width: 560px) {
    .ai-panel {
        right: 10px;
        bottom: 10px;
        width: calc(100vw - 20px);
        height: calc(100vh - 20px);
        max-height: none;
    }
    .ai-fab {
        right: 14px;
        bottom: 18px;
    }
}
</style>
