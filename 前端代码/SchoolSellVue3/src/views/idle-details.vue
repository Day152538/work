<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="idle-details-container">
                <div class="details-header">
                    <div class="details-header-user-info">
                        <el-image style="width: 80px; height: 80px;border-radius: 5px;"
                            :src="imageUrl(idleItemInfo.user.avatar)" fit="cover"></el-image>
                        <div class="details-header-user-info-main">
                            <div class="details-header-user-info-nickname"> 商家：{{ idleItemInfo.user.nickname }}</div>
                            <el-button v-if="!isMaster && idleItemInfo.idleStatus === 1 && idleItemInfo.userId"
                                size="small" type="warning" plain @click="contactSeller"
                                style="margin-top: 8px; width: fit-content;">💬 私信卖家</el-button>
                        </div>
                    </div>
                    <div class="details-header-buy">
                        <div v-if="!isMaster && nonMasterStateText" style="color: red;font-size: 16px;">
                            {{ nonMasterStateText }}
                        </div>
                        <div class="inner-content">
                            <div class="button1" v-if="isMaster" type="primary" plain @click="openEditDialog">编辑商品</div>
                            <div class="button2" v-if="canBuy" type="danger" plain
                                @click="buyButton(idleItemInfo)"> 立即购买</div>
                            <div class="button1" v-if="canBuy" type="primary" plain
                                @click="favoriteButton(idleItemInfo)">{{ isFavorite ? ' 取消收藏' : ' 收藏' }}</div>
                            <div class="button3" v-if="canTakeDown" type="danger"
                                @click="changeStatus(idleItemInfo, 2)" plain> 下架</div>
                            <div class="button2" v-if="canRelist" type="primary"
                                @click="changeStatus(idleItemInfo, 1)" plain> 重新上架</div>
                            <div v-if="soldOutAndDown" style="color: red;font-size: 16px;">
                                已售罄，编辑补充库存后可重新上架
                            </div>
                        </div>
                    </div>
                </div>

                <div class="details-info">
                    <div class="details-reason" v-if="idleReasonText">
                        <b>处理说明：</b>{{ idleReasonText }}
                    </div>
                    <div class="details-info-title-row">
                        <div class="details-info-title">物品名称：{{ idleItemInfo.idleName }}</div>
                        <div class="details-info-price">价格: ￥{{ idleItemInfo.idlePrice }}</div>
                        <div class="details-info-price-vip">会员价格: ￥{{ (idleItemInfo.idlePrice * 0.8).toFixed(2) }}</div>
                    </div>
                    <div class="details-tags" v-if="idleTagList.length">
                        <span class="detail-tag" v-for="(tag, i) in idleTagList" :key="i"
                            @click="searchByTag(tag)">#{{ tag }}</span>
                    </div>
                    <div class="details-sales" v-if="idleItemInfo.stock > 1 || idleItemInfo.salesCount > 0">
                        已售 {{ idleItemInfo.salesCount || 0 }} 件 · 剩余库存
                        {{ idleItemInfo.stock || 0 }} 件
                    </div>
                    <div class="details-info-describe">物品描述：</div>
                    <div class="details-info-main" v-html="idleItemInfo.idleDetails">
                    </div>
                    <div class="details-info-describe_pic">物品图片：</div>
                    <div class="details-picture">
                        <el-image v-for="(imgUrl, i) in idleItemInfo.pictureList" :key="i"
                            style="width: 33%;height: 30%; margin-bottom: 2px;" :src="imageUrl(imgUrl)"
                            fit="cover"></el-image>
                    </div>
                </div>

                <div class="message-container" id="replyMessageLocation">
                    <div class="message-title">商品评论</div>
                    <div class="message-send">
                        <div v-if="isReply" style="padding-bottom: 10px;">
                            <el-button type="info" @click="cancelReply" style="color: blue; font-size: 16px;">回复：{{ replyData.toMessage }}
                                @{{ replyData.toUserNickname }} <el-icon class="el-icon--right"><Close /></el-icon></el-button>
                        </div>
                        <el-input type="textarea" autosize placeholder="留言提问..." v-model="messageContent"
                            maxlength="200" show-word-limit>
                        </el-input>
                        <div class="message-send-button">
                            <el-button @click="sendMessage" type="success"> 评论</el-button>
                        </div>
                    </div>
                    <div>
                        <div v-for="(mes, index) in messageList" :key="index" class="message-container-list">
                            <div class="message-container-list-left">
                                <el-image style="width: 55px; height: 55px;border-radius: 5px;"
                                    :src="imageUrl(mes.fromU.avatar)" fit="contain"></el-image>
                                <div class="message-container-list-text">
                                    <div class="message-nickname">{{ mes.fromU.nickname }}
                                        {{ mes.toU.nickname ? ' @' + mes.toU.nickname + '：' +
                                        mes.toM.content.substring(0, 10) +
                                        (mes.toM.content.length > 10 ? '...' : '') : '' }}</div>
                                    <div class="message-content">{{ mes.content }}</div>
                                    <div class="message-time">{{ mes.createTime }}</div>
                                </div>
                            </div>
                            <div class="message-container-list-right">
                                <el-button style="float: right;" plain @click="replyMessage(index)"> 回复</el-button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <!-- 编辑商品弹窗 -->
            <el-dialog v-model="editDialogVisible" title="编辑商品信息">
                <el-form :model="editedItem" label-width="120px">
                    <el-form-item label="物品名称">
                        <el-input v-model="editedItem.idleName"></el-input>
                    </el-form-item>
                    <el-form-item label="物品价格">
                        <el-input v-model="editedItem.idlePrice"></el-input>
                    </el-form-item>
                    <el-form-item label="库存数量（件）">
                        <el-input-number v-model="editedItem.stock" :min="1" :max="9999"
                            :precision="0" controls-position="right"></el-input-number>
                    </el-form-item>
                    <el-form-item label="物品描述">
                        <el-input type="textarea" v-model="editedItem.idleDetails"></el-input>
                    </el-form-item>
                    <el-form-item label="物品标签">
                        <el-input v-model="editedItem.idleTags" placeholder="多个标签用逗号分隔，如：可爱,萌宠"
                            maxlength="80"></el-input>
                    </el-form-item>
                </el-form>
                <div class="release-idle-container-picture-title">上传物品照片</div>
                <el-upload :http-request="uploadRequest" :on-preview="fileHandlePreview"
                    :on-remove="fileHandleRemove" :show-file-list="showFileList"
                    :limit="7" :on-exceed="handleExceed" accept="image/*" drag multiple>
                    <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
                    <div class="el-upload__text"><em>点击上传</em></div>
                </el-upload>
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="editDialogVisible = false">取消</el-button>
                        <el-button type="primary" @click="saveEditedItem">保存</el-button>
                    </span>
                </template>
            </el-dialog>
            <el-dialog v-model="imgDialogVisible">
                <img width="100%" :src="imageUrl(dialogImageUrl)" alt="">
            </el-dialog>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 身份判断弃用 cookie shUserId（可伪造）→ Pinia userStore（JWT 登录态）；
 *    /details 为公开路由，token 存在但 store 未恢复时主动调 refreshUser
 * 2. VIP 折扣判断 userStatus===3 → userStore.isVip（基于 vipExpireTime）
 * 3. 图片地址全部 imageUrl()（兼容旧数据 "/image?imageName=xxx" 前缀）
 * 4. 编辑弹窗上传改 api.uploadFile 自定义 http-request（原硬编码 action= localhost:9321/file/
 *    且不带 JWT）；并修复原 bug：打开编辑弹窗未用现有图片初始化 imgList，不传图保存会把 pictureList 清空
 * 5. 删除 jQuery 动画 → 原生 scrollTo；:visible.sync → v-model；slot="footer" → #footer
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { imageUrl } from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 兼容旧数据：历史 pictureList 里存的是 "/image?imageName=xxx"，统一剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
    name: "idle-details",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            userStore: null,
            editDialogVisible: false,
            imgDialogVisible: false,
            showFileList: true,
            imgList: [],
            dialogImageUrl: '',
            editedItem: {
                idleName: '',
                idlePrice: 0,
                stock: 1,
                idleDetails: '',
                idleTags: ''
            },
            messageContent: '',
            toUser: null,
            toMessage: null,
            isReply: false,
            replyData: {
                toUserNickname: '',
                toMessage: ''
            },
            messageList: [],
            idleItemInfo: {
                id: '',
                idleName: '',
                idleDetails: '',
                pictureList: [],
                idlePrice: 0,
                idlePlace: '',
                idleLabel: '',
                idleStatus: -1,
                userId: '',
                user: {
                    userStatus: '',
                    avatar: '',
                    nickname: '',
                    signInTime: ''
                },
            },
            isFavorite: false,
            favoriteId: 0
        };
    },
    computed: {
        // 商品标签：idle_tags 逗号/顿号/空格分隔，去空
        idleTagList() {
            const raw = this.idleItemInfo.idleTags || '';
            return raw.split(/[,，、\s]+/).map(t => t.trim()).filter(Boolean);
        },
        // 审核未通过/违规原因：仅在状态 4（未通过/违规下架）时展示
        idleReasonText() {
            if (this.idleItemInfo.idleStatus !== 4) return '';
            return (this.idleItemInfo.idleReason || '').trim();
        },
        // 是否商品主人：以 JWT 恢复出的用户 id 判断（原版读可伪造的 cookie）
        isMaster() {
            if (!this.userStore || !this.userStore.userInfo || !this.userStore.userInfo.id) return false;
            return String(this.userStore.userInfo.id) === String(this.idleItemInfo.userId);
        },
        // 库存相关（多件可售）：详情未加载完成时视为有货，避免误显示“售罄”
        hasStock() {
            return this.idleItemInfo.stock == null || this.idleItemInfo.stock > 0;
        },
        isSoldOut() {
            return this.idleItemInfo.stock != null && this.idleItemInfo.stock <= 0;
        },
        // 买家在售时可购买；卖家有货时可下架 / 下架且有货时可重新上架；售罄不可再上架
        canBuy() {
            return !this.isMaster && this.idleItemInfo.idleStatus === 1 && this.hasStock;
        },
        canTakeDown() {
            return this.isMaster && this.idleItemInfo.idleStatus === 1 && this.hasStock;
        },
        canRelist() {
            return this.isMaster && this.idleItemInfo.idleStatus === 2 && this.hasStock;
        },
        soldOutAndDown() {
            return this.isMaster && this.idleItemInfo.idleStatus === 2 && this.isSoldOut;
        },
        // 买家视角的商品状态文案
        nonMasterStateText() {
            if (this.isMaster) return '';
            // 详情未加载完成(idleStatus 默认 -1)时不显示任何状态文案
            if (this.idleItemInfo.idleStatus == null || this.idleItemInfo.idleStatus < 0) return '';
            if (this.idleItemInfo.idleStatus === 1) {
                return this.isSoldOut ? '商品已售罄' : '';
            }
            if (this.isSoldOut) return '商品已售罄';
            if (this.idleItemInfo.idleStatus === 2) return '商品已售出';
            if (this.idleItemInfo.idleStatus === 3) return '商品审核中';
            if (this.idleItemInfo.idleStatus === 4) return '商品未通过审核';
            return '商品已下架';
        }
    },
    created() {
        this.userStore = useUserStore();
        // 公开页面：有 token 但内存无登录态时恢复一次，保证"编辑/购买/VIP价"判断可用
        if (localStorage.getItem('token') && !this.userStore.isLogin) {
            this.userStore.refreshUser(api).catch(() => { });
        }
    },
    mounted() {
        let id = this.$route.query.id;
        api.getIdleItem({
            id: id
        }).then(res => {
            if (res.data) {
                let list = res.data.idleDetails.split(/\r?\n/);
                let str = '';
                for (let i = 0; i < list.length; i++) {
                    str += '<p>' + list[i] + '</p>';
                }
                res.data.idleDetails = str;
                res.data.pictureList = JSON.parse(res.data.pictureList);
                this.idleItemInfo = res.data;
                // 已登录才查询收藏状态（/favorite/check 需鉴权）；
                // 游客保持 isFavorite=false，按钮显示“收藏”而不是“取消收藏”
                if (this.userStore && this.userStore.isLogin) {
                    this.checkFavorite();
                }
                this.getAllIdleMessage();
            }
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    },
    methods: {
        imageUrl,
        // 处理文件移除事件
        fileHandleRemove(file) {
            // 从图片列表中移除被移除的文件（自定义上传里已把文件名记到 file.response.data）
            const name = file.response && file.response.data;
            const i = this.imgList.indexOf(name);
            if (i > -1) {
                this.imgList.splice(i, 1);
            }
        },
        // 处理文件预览事件
        fileHandlePreview(file) {
            this.dialogImageUrl = file.response.data;
            this.imgDialogVisible = true;
        },
        // 自定义上传：走 api.uploadFile（携带 JWT），后端返回 "/image?imageName=xxx"
        uploadRequest(option) {
            api.uploadFile(option.file).then(res => {
                if (res.status_code === 1) {
                    const name = toImageName(res.data);
                    this.imgList.push(name);
                    // 让 el-upload 记录 response，供预览/移除使用
                    option.onSuccess({ data: name });
                } else {
                    this.$message.error(res.msg || '图片上传失败');
                    option.onError(new Error(res.msg));
                }
            }).catch(() => {
                this.$message.error('图片上传失败');
                option.onError(new Error('upload failed'));
            });
        },
        handleExceed(files, fileList) {
            // 提示文件数量超出限制信息
            this.$message.warning(`限制7张图片，本次选择了 ${files.length} 张图，共选择了 ${files.length + fileList.length} 张图`);
        },
        openEditDialog() {
            // 打开编辑弹窗并初始化编辑数据
            this.editedItem = {
                idleName: this.idleItemInfo.idleName,
                idlePrice: this.idleItemInfo.idlePrice,
                stock: this.idleItemInfo.stock != null ? this.idleItemInfo.stock : 1,
                idleDetails: this.idleItemInfo.idleDetails.replace(/<p>/g, '').replace(/<\/p>/g, ''),
                idleTags: this.idleItemInfo.idleTags || ''
            };
            // 修复原版 bug：用现有图片初始化 imgList，避免不传图保存时清空 pictureList
            this.imgList = (this.idleItemInfo.pictureList || []).map(toImageName);
            this.editDialogVisible = true;
        },
        saveEditedItem() {
            if (!this.editedItem.stock || this.editedItem.stock < 1) {
                this.$message.error('库存数量至少为 1 件');
                return;
            }
            // 保存成功后关闭弹窗
            this.editDialogVisible = false;
            api.updateIdleItem({
                id: this.idleItemInfo.id, idleStatus: 3,
                idleName: this.editedItem.idleName, idlePrice: this.editedItem.idlePrice,
                stock: this.editedItem.stock, idleDetails: this.editedItem.idleDetails,
                idleTags: this.editedItem.idleTags, pictureList: JSON.stringify(this.imgList)
            }).then(response => {
                if (response.status_code === 1) {
                    this.$message({
                        message: '修改成功，待审核！',
                        type: 'success'
                    });
                }
            }).catch(() => {
                this.$message.error('更新失败');
            });
        },
        getAllIdleMessage() {
            api.getAllIdleMessage({
                idleId: this.idleItemInfo.id
            }).then(res => {
                if (res.status_code === 1) {
                    this.messageList = res.data;
                }
            }).catch(() => {
            })
        },
        checkFavorite() {
            api.checkFavorite({
                idleId: this.idleItemInfo.id
            }).then(res => {
                // 接口返回空=未收藏，返回收藏 id=已收藏，两种状态都明确赋值，
                // 不再依赖 data() 里的初始默认值（避免游客看到“取消收藏”）
                this.favoriteId = res.data || 0;
                this.isFavorite = !!res.data;
            }).catch(() => {
            })
        },
        // 需登录操作的统一前置校验：已登录直接放行；有 token 但内存未恢复则先尝试恢复；
        // 无 token（游客）提示并跳登录页。返回是否可继续操作。
        async ensureLogin() {
            if (this.userStore && this.userStore.isLogin) return true;
            if (localStorage.getItem('token')) {
                // token 过期/无效时，request 拦截器会提示“登录已过期”并跳登录页
                const ok = await this.userStore.refreshUser(api).catch(() => false);
                return !!ok;
            }
            this.$message.warning('请先登录后再操作');
            this.$router.push({ path: '/login' });
            return false;
        },
        replyMessage(index) {
            const el = document.getElementById('replyMessageLocation');
            if (el) {
                window.scrollTo({ top: el.offsetTop - 600, behavior: 'smooth' });
            }
            this.isReply = true;
            this.replyData.toUserNickname = this.messageList[index].fromU.nickname;
            this.replyData.toMessage = this.messageList[index].content.substring(0, 10) + (this.messageList[index].content.length > 10 ? '...' : '');
            this.toUser = this.messageList[index].userId;
            this.toMessage = this.messageList[index].id;
        },
        changeStatus(idle, status) {
            api.updateIdleItem({
                id: idle.id,
                idleStatus: status
            }).then(res => {
                if (res.status_code === 1) {
                    this.idleItemInfo.idleStatus = status;
                } else {
                    this.$message.error(res.msg)
                }
            });
        },
        // 点击标签：跳转搜索页查找同标签商品（复用关键字搜索接口，后端已支持匹配标签）
        searchByTag(tag) {
            this.$router.push({ path: '/search', query: { searchValue: tag } });
        },
        // 下单前咨询：买家从商品详情直接联系卖家（商品级会话）
        async contactSeller() {
            if (!(await this.ensureLogin())) return;
            const sellerId = this.idleItemInfo.userId;
            if (!sellerId || !this.idleItemInfo.id) return;
            this.$router.push({
                path: '/chat',
                query: { itemId: this.idleItemInfo.id, otherUserId: sellerId, from: 'details' }
            });
        },
        async buyButton(idleItemInfo) {
            // 未登录点击“立即购买”给出提示并引导登录，而不是静默失败
            if (!(await this.ensureLogin())) return;
            if (this._buying) return; // 防重复提交
            this._buying = true;
            let orderPrice = idleItemInfo.idlePrice;
            // VIP 折扣改看 userStore.isVip（原版 userStatus===3 可被伪造）
            if (this.userStore && this.userStore.isVip) {
                orderPrice *= 0.8;
            }
            // 幂等键：UUID，后端 Redis SETNX 防重复下单（Redis 不可用时降级内存）
            const requestId = (crypto && crypto.randomUUID) ? crypto.randomUUID()
                : 'id-' + Date.now() + '-' + Math.random().toString(36).slice(2);
            api.addOrder({
                idleId: idleItemInfo.id,
                orderPrice: orderPrice,
            }, { requestId }).then(res => {
                if (res.status_code === 1) {
                    this.$router.push({ path: '/order', query: { id: res.data.id } });
                } else {
                    this.$message.error(res.msg)
                }
            }).catch(() => {
            }).finally(() => {
                this._buying = false;
            });
        },
        async favoriteButton(idleItemInfo) {
            // 未登录点击收藏给出提示并引导登录，而不是静默失败
            if (!(await this.ensureLogin())) return;
            if (this.isFavorite) {
                api.deleteFavorite({
                    id: this.favoriteId
                }).then(res => {
                    if (res.status_code === 1) {
                        this.$message({
                            message: '已取消收藏！',
                            type: 'success'
                        });
                        this.isFavorite = false;
                    } else {
                        this.$message.error(res.msg)
                    }
                }).catch(() => {
                })
            } else {
                api.addFavorite({
                    idleId: idleItemInfo.id
                }).then(res => {
                    if (res.status_code === 1) {
                        this.$message({
                            message: '已收藏！',
                            type: 'success'
                        });
                        this.isFavorite = true;
                        this.favoriteId = res.data;
                    } else {
                        this.$message.error(res.msg)
                    }
                }).catch(() => {
                })
            }
        },
        cancelReply() {
            this.isReply = false;
            this.toUser = this.idleItemInfo.userId;
            this.toMessage = null;
            this.replyData.toUserNickname = '';
            this.replyData.toMessage = '';
        },
        async sendMessage() {
            let content = this.messageContent.trim();
            // 评论需登录：游客发送前给出明确提示
            if (!(await this.ensureLogin())) return;
            if (this.toUser == null) {
                this.toUser = this.idleItemInfo.userId;
            }
            if (content) {
                let contentList = content.split(/\r?\n/);
                let contenHtml = contentList[0];
                for (let i = 1; i < contentList.length; i++) {
                    contenHtml += '<br>' + contentList[i];
                }
                api.sendMessage({
                    idleId: this.idleItemInfo.id,
                    content: contenHtml,
                    toUser: this.toUser,
                    toMessage: this.toMessage
                }).then(res => {
                    if (res.status_code === 1) {
                        this.$message({
                            message: '留言成功！',
                            type: 'success'
                        });
                        this.messageContent = '';
                        this.cancelReply();
                        this.getAllIdleMessage();
                    } else {
                        this.$message.error("留言失败！" + res.msg);
                    }
                }).catch(() => {
                    this.$message.error("留言失败！");
                });

            } else {
                this.$message.error("留言为空！");
            }
        }
    },
}
</script>

<style scoped>
.idle-details-container {
    min-height: 85vh;
}

.details-header {
    min-height: 80px;
    border-bottom: 10px solid #f6f6f6;
    display: flex;
    justify-content: space-between;
    padding: 20px;
    align-items: center;
}

.details-header-user-info {
    display: flex;
    align-items: center;
}

.details-header-user-info-main {
    margin-left: 12px;
    display: flex;
    flex-direction: column;
    justify-content: center;
}

.details-header-user-info-nickname {
    font-size: 24px;
    font-weight: 600;
    margin: 0;
}

.details-header-buy {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: center;
    gap: 8px;
    max-width: 340px;
}

.details-header-buy .inner-content {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
}

.details-info {
    padding: 20px 50px;
}

.details-info-title-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 16px;
}

/* 审核未通过/违规处理说明（卖家与访客可见） */
.details-reason {
    background: #fdf6ec;
    border: 1px solid #fae3c3;
    border-radius: 8px;
    color: #b45309;
    font-size: 15px;
    line-height: 1.6;
    padding: 10px 14px;
    margin-bottom: 16px;
}

.details-info-title {
    flex: 1;
    min-width: 0;
    font-size: 22px;
    font-weight: 600;
    line-height: 1.5;
    overflow-wrap: break-word;
}

.details-info-price {
    color: red;
    font-size: 32px;
    font-weight: 600;
    flex-shrink: 0;
}

.details-info-price-vip {
    color: red;
    font-size: 25px;
    font-weight: 400;
    flex-shrink: 0;
}

/* 商品标签：显示在物品名称下方，可点击跳转搜索 */
.details-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
}

.detail-tag {
    display: inline-block;
    padding: 3px 12px;
    background: #ecf5ff;
    color: #409EFF;
    border-radius: 14px;
    font-size: 14px;
    cursor: pointer;
    transition: background-color 0.2s ease;
}

.detail-tag:hover {
    background: #d9ecff;
}

.details-header-buy .inner-content {
    display: flex;
    align-items: center;
}

.details-info-describe {
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 20px;
    color: #9b6060
}

.details-info-describe_pic {
    margin-top: 20px;
    font-size: 22px;
    font-weight: 600;
    margin-bottom: 20px;
    color: #eb5c5c
}

.details-sales {
    color: #888888;
    font-size: 14px;
    margin-bottom: 20px;
}

/* 创建一个炫酷的背景 */
.details-info-main {
    padding: 5px;
    padding-left: 10px;
    font-size: 17px;
    color: #121212;
    line-height: 160%;
    /* 使用线性渐变创建背景，从左上到右下 */
    background: linear-gradient(-45deg, rgba(238, 119, 82, 0.5), rgba(231, 60, 126, 0.5), rgba(35, 166, 213, 0.5), rgba(35, 213, 171, 0.5));

    /* 设置渐变的速度 */
    background-size: 400% 400%;

    /* 设置动画名称和持续时间 */
    animation: gradientBG 15s ease infinite;
    border-radius: 5px;
}

/* 定义动画 */
@keyframes gradientBG {
    0% {
        background-position: 0% 50%;
    }

    50% {
        background-position: 100% 50%;
    }

    100% {
        background-position: 0% 50%;
    }
}

.details-picture {
    margin: 20px 0;
    display: flex;

}

.message-container {
    min-height: 100px;
    border-top: 10px solid #f6f6f6;
    padding: 20px;
}

.message-title {
    font-size: 20px;
    font-weight: 600;
    margin-bottom: 20px;
}

.message-send {
    min-height: 60px;
}

.message-send-button {
    margin-top: 10px;
    display: flex;
    justify-content: flex-end;
}

.message-container-list {
    min-height: 60px;
    border-top: 1px solid #eeeeee;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 15px 0;
}

.message-container-list:first-child {
    border-top: none;
}

.message-container-list-left {
    width: 850px;
    display: flex;
}

.message-container-list-right {
    width: 100px;
}

.message-container-list-text {
    margin-left: 10px;
}

.message-nickname {
    font-weight: 600;
    font-size: 18px;
    padding-bottom: 5px;

}

.message-content {
    font-size: 16px;
    padding-bottom: 15px;
    color: #555555;
    width: 770px;
}

.message-time {
    font-size: 13px;
    color: #555555;
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
    margin-left: 10px;

    justify-content: center;
    /* 水平居中 */
    align-items: center;

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease;

}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
    background-color: #66b1ff;
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
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    margin-left: 10px;
    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease;
}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button2:hover {
    background-color: #d2e493;
}

.dialog-footer {
    display: flex;
    justify-content: space-between;
    margin-top: 20px;
}

.button3 {
    width: 120px;
    height: 50px;
    font-size: 20px;
    color: white;
    text-align: center;
    background-color: #d47b62;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    margin-left: 10px;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease;
}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button3:hover {
    background-color: #ca9d42;
}
</style>
