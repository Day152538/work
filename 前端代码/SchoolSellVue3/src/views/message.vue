<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="message-container">
                <div class="message-container-title"><b> 消息中心</b></div>
                <el-tabs v-model="activeTab" class="hub-tabs">
                    <!-- Tab1：私信会话（下单前咨询 + 下单后沟通，按商品聚合） -->
                    <el-tab-pane label="私信会话" name="conversations">
                        <div v-if="conversations.length === 0" class="empty">
                            暂无会话。在商品详情点「私信卖家」即可与卖家咨询。
                        </div>
                        <div v-for="(c, i) in conversations" :key="i" class="conv-row" @click="openChat(c)">
                            <el-avatar :size="48" shape="square" :src="imageUrl(c.otherAvatar)">
                                <el-icon :size="24"><UserFilled /></el-icon>
                            </el-avatar>
                            <div class="conv-main">
                                <div class="conv-title">
                                    <b>{{ c.otherNickname }}</b>
                                    <el-tag size="small" type="info" effect="plain">{{ c.itemName }}</el-tag>
                                    <el-badge v-if="c.unread > 0" :value="c.unread" class="unread-badge" />
                                </div>
                                <div class="conv-last">
                                    <span class="conv-content">{{ c.lastContent }}</span>
                                    <span class="conv-time">{{ shortTime(c.lastTime) }}</span>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>

                    <!-- Tab2：商品留言（别人在“我的商品”下留的言） -->
                    <el-tab-pane label="商品留言" name="idle">
                        <div v-if="meslist.length === 0" class="empty">暂无商品留言。</div>
                        <div v-for="(mes, index) in meslist" :key="index" class="list-row">
                            <el-avatar :size="48" shape="square" :src="imageUrl(mes.fromU.avatar)">
                                <el-icon :size="24"><UserFilled /></el-icon>
                            </el-avatar>
                            <div class="conv-main">
                                <div class="conv-title"><b>{{ mes.fromU.nickname }}</b>
                                    <el-tag size="small" type="info" effect="plain">{{ mes.idle.idleName }}</el-tag>
                                </div>
                                <div class="conv-last"><span class="conv-content">{{ mes.content }}</span>
                                    <span class="conv-time">{{ shortTime(mes.createTime) }}</span></div>
                            </div>
                            <div class="row-action">
                                <el-button type="primary" link size="small" @click="toDetails(mes.idle.id)">查看商品</el-button>
                                <el-button type="danger" link size="small" @click="deleteIdleMessage(mes.id)">删除</el-button>
                            </div>
                        </div>
                    </el-tab-pane>

                    <!-- Tab3：系统/管理员消息 + 反馈入口 -->
                    <el-tab-pane label="系统消息" name="admin">
                        <div style="margin-bottom: 12px;">
                            <el-button type="warning" plain size="small" @click="$router.push('/message1')">
                                去信息反馈（提交给管理员）
                            </el-button>
                        </div>
                        <div v-if="adminMeslist.length === 0" class="empty">暂无系统/管理员消息。</div>
                        <div v-for="mes in adminMeslist" :key="mes.id" class="list-row admin-row">
                            <div class="conv-main">
                                <div class="conv-last"><span class="conv-content">{{ mes.userMessage }}</span></div>
                            </div>
                            <div class="row-action">
                                <el-button type="danger" link size="small" @click="deleteAdminMessage(mes.id)">删除</el-button>
                            </div>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 消息中心（整合版，2026-09）
 * Tab1 私信会话：买家↔卖家按商品聚合的私信（下单前可咨询）
 * Tab2 商品留言：别人在我商品下的留言（原“商品信息”）
 * Tab3 系统消息：管理员/平台通知 + 反馈入口
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { imageUrl } from '@/utils/request'

export default {
    name: "message",
    components: { AppHead, AppBody, AppFoot },
    data() {
        return {
            activeTab: 'conversations',
            conversations: [],
            meslist: [],
            adminMeslist: []
        };
    },
    created() {
        this.loadConversations();
        this.loadIdleMessages();
        this.loadAdminMessages();
    },
    methods: {
        imageUrl,
        loadConversations() {
            api.chatConversations().then(res => {
                if (res.status_code === 1) {
                    this.conversations = res.data || [];
                }
            }).catch(() => { });
        },
        loadIdleMessages() {
            api.getAllMyMessage().then(res => {
                if (res.status_code === 1) {
                    for (const mes of res.data) {
                        let imgList = [];
                        try { imgList = JSON.parse(mes.idle.pictureList); } catch (e) { imgList = []; }
                        mes.idle.imgUrl = imgList && imgList.length ? imgList[0] : '';
                        mes.content = String(mes.content || '').split('<br>').join(' ');
                    }
                    this.meslist = res.data;
                }
            });
        },
        loadAdminMessages() {
            api.getUserMessageList().then(res => {
                if (res.status_code === 1) this.adminMeslist = res.data;
            });
        },
        openChat(c) {
            this.$router.push({
                path: '/chat',
                query: { itemId: c.itemId, otherUserId: c.otherUserId, from: 'message' }
            });
        },
        toDetails(id) {
            this.$router.push({ path: '/details', query: { id } });
        },
        deleteIdleMessage(id) {
            api.deleteMessage(id).then(() => {
                this.$message.success('留言删除成功');
                this.loadIdleMessages();
            }).catch(() => this.$message.error('删除失败'));
        },
        deleteAdminMessage(id) {
            api.deleteUserMessage(id).then(() => {
                this.$message.success('消息删除成功');
                this.loadAdminMessages();
            }).catch(() => this.$message.error('删除失败'));
        },
        shortTime(t) {
            return t ? String(t).substring(5, 16) : '';
        }
    }
}
</script>

<style scoped>
.message-container {
    min-height: 85vh;
    padding: 0 24px 30px;
}

.message-container-title {
    padding: 20px 0;
    text-align: center;
    font-size: 25px;
}

.hub-tabs {
    background: #fff;
    border-radius: 10px;
    padding: 6px 18px;
    box-shadow: 0 1px 8px rgba(0, 0, 0, 0.05);
}

.empty {
    text-align: center;
    color: #a0a3ab;
    padding: 40px 0;
}

.conv-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 4px;
    border-bottom: 1px solid #f2f4f8;
    cursor: pointer;
}

.conv-row:hover {
    background: #f7f9fc;
}

.conv-main {
    flex: 1;
    min-width: 0;
}

.conv-title {
    display: flex;
    align-items: center;
    gap: 8px;
}

.conv-title b {
    font-size: 15px;
}

.unread-badge {
    margin-left: 4px;
}

.conv-last {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    margin-top: 4px;
    font-size: 13px;
    color: #888;
}

.conv-content {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.conv-time {
    flex-shrink: 0;
    color: #b0b3bb;
}

.list-row {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 4px;
    border-bottom: 1px solid #f2f4f8;
}

.admin-row .conv-last {
    color: #333;
}

.row-action {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex-shrink: 0;
}
</style>
