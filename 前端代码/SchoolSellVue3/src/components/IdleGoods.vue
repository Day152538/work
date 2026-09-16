<template>
    <div class="main-border">
        <!-- 统一搜索栏 -->
        <admin-search-bar v-model="searchText" placeholder="输入商品名称搜索" @search="search" />
        <el-menu default-active="1" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="1">通审核商品</el-menu-item>
            <el-menu-item index="2">待审核商品</el-menu-item>
            <el-menu-item index="3">违规商品</el-menu-item>
        </el-menu>
        <el-table v-if="mode == 1" :data="onlineGoods" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column prop="releaseTime" label="商品日期" width="200">
            </el-table-column>
            <el-table-column prop="idleName" label="商品名称" show-overflow-tooltip>
            </el-table-column>
            <el-table-column prop="user.nickname" label="商家" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column prop="idlePrice" label="价格" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column label="操作" min-width="230">
                <template #default="scope">
                    <div class="op-btns">
                        <div class="btn btn-danger" @click="askReason('offline', scope.$index)">违规下架</div>
                        <div class="btn btn-info" @click="showPopup(scope.row)">详情</div>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <el-table v-show="mode == 2" :data="offlineGoods" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column prop="releaseTime" label="商品日期" width="200">
            </el-table-column>
            <el-table-column prop="idleName" label="商品名称" show-overflow-tooltip>
            </el-table-column>
            <el-table-column prop="user.nickname" label="商家" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column prop="idlePrice" label="价格" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column label="操作" min-width="300">
                <template #default="scope">
                    <div class="op-btns">
                        <div class="btn btn-success" @click="relistOfflineGoods(scope.$index)">审核通过</div>
                        <div class="btn btn-danger" @click="askReason('reject', scope.$index)">未通过</div>
                        <div class="btn btn-info" @click="showPopup(scope.row)">详情</div>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <el-table v-show="mode == 3" :data="offlineGoods1" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column prop="releaseTime" label="商品日期" width="200">
            </el-table-column>
            <el-table-column prop="idleName" label="商品名称" show-overflow-tooltip>
            </el-table-column>
            <el-table-column prop="user.nickname" label="商家" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column prop="idlePrice" label="价格" show-overflow-tooltip min-width="100" width="100">
            </el-table-column>
            <el-table-column label="操作" min-width="300">
                <template #default="scope">
                    <div class="op-btns">
                        <div class="btn btn-warn" @click="deleteGoods(scope.$index)">删除</div>
                        <div class="btn btn-success" @click="relistOfflineGoods1(scope.$index)">重新上架</div>
                        <div class="btn btn-info" @click="showPopup(scope.row)">详情</div>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <!-- Popup component -->
        <el-dialog v-model="popupVisible" title="商品详情" :width="500">
            <div v-if="currentItem">
                <!-- 商品详情内容 -->
                <p>商品id: {{ currentItem.id }}</p>
                <p>商品名称: {{ currentItem.idleName }}</p>
                <p>商家: {{ currentItem.user.nickname }}</p>
                <p>价格: {{ currentItem.idlePrice }}</p>
                <!-- 只渲染实际上传的图片：上传几张显示几张，没有的不显示 -->
                <img v-for="(img, i) in currentItemImages" :key="i"
                    :src="imageUrl(img)" alt="商品图片"
                    style="width: 33%">
                <p>详情: {{ currentItem.idleDetails }}</p>
                <p v-if="currentItem.idleReason" style="color: #e6a23c;">
                    <b>处理原因：</b>{{ currentItem.idleReason }}
                </p>
            </div>
            <template #footer>
                <span class="dialog-footer">
                    <el-button type="primary" @click="handleDialogConfirm">关闭</el-button>
                </span>
            </template>
        </el-dialog>
        <!-- 违规下架 / 审核未通过 原因填写 -->
        <el-dialog v-model="reasonDialogVisible" title="填写处理原因" :width="480">
            <el-input v-model="reasonText" type="textarea" :rows="3" maxlength="200" show-word-limit
                placeholder="请填写原因，卖家可在个人中心与商品详情中看到（如：图片与实物不符、含违禁信息、描述违规等）">
            </el-input>
            <template #footer>
                <span class="dialog-footer">
                    <el-button @click="reasonDialogVisible = false">取消</el-button>
                    <el-button type="primary" @click="confirmReason">确定处理</el-button>
                </span>
            </template>
        </el-dialog>
        <!-- 分页（本地切片，翻页无需重新请求） -->
        <div class="list-paging">
            <el-pagination background @current-change="handleCurrentChange"
                v-model:current-page="nowPage" :page-size="pageSize"
                layout="total, prev, pager, next" :total="total">
            </el-pagination>
        </div>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. this.$api → import api from '@/api'（getGoods/updateGoods/deleteGood，
 *    管理端接口走 adminRequest 携带 admin_token）
 * 2. 图片地址 $store.state.baseApi + img → imageUrl()（兼容旧数据 "/image?imageName=xxx"）
 * 3. 模板内 this.mode → mode（Vue3 模板不建议 this）；slot-scope → #default；
 *    :visible.sync → v-model；slot="footer" → #footer
 * 4. 删除 console.log 与死代码（旧版 handleCurrentChange 分页已注释掉、
 *    handleDialogClose 未被引用）
 */
import api from '@/api'
import { imageUrl } from '@/utils/request'

// 兼容旧数据：历史 pictureList 里存的是 "/image?imageName=xxx"，统一剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
    name: "IdleGoods",
    data() {
        return {
            mode: 1,
            nowPage: 1,
            total: 0,
            pageSize: 10,
            onlineGoods: [],
            offlineGoods: [],
            offlineGoods1: [],
            popupVisible: false, // Flag to control popup visibility
            currentItem: null, // Current item to display in the popup
            // 违规下架 / 审核未通过原因填写
            reasonDialogVisible: false,
            reasonText: '',
            reasonTarget: null, // { type: 'offline'|'reject', index: n }
            // 全量原始列表（表格展示的是它按关键字过滤后的结果，避免二次搜索叠加过滤）
            _onlineGoods: [],
            _offlineGoods: [],
            _offlineGoods1: [],
            searchText: '', // 存储搜索关键词
        }
    },
    computed: {
        // 当前商品已上传的图片列表：解析 pictureList 得到实际数组，
        // 避免写死索引越界（如只上传一张时 [1]/[2] 为 undefined）
        currentItemImages() {
            if (!this.currentItem) return [];
            try {
                const list = JSON.parse(this.currentItem.pictureList);
                return (Array.isArray(list) ? list : []).map(toImageName);
            } catch (e) {
                return [];
            }
        }
    },
    created() {
        this.getOnlineGoods();
        this.getOfflineGoods();
        this.getOfflineGoods1();
    },
    methods: {
        imageUrl,
        // 展示 = 当前模式的全量 → 按关键字过滤 → 再按当前页切片。
        // 过滤/翻页都在本地完成（后端接口不支持关键字，且数据量小），翻页不重新请求。
        renderCurrent() {
            const kw = this.searchText.trim().toLowerCase();
            const size = this.pageSize;
            const toPage = (full) => {
                const filtered = full.filter(item =>
                    !kw || String(item.idleName || '').toLowerCase().includes(kw)
                );
                // 删除/过滤后若当前页超出范围则收回到最后一页
                const totalPages = Math.max(1, Math.ceil(filtered.length / size));
                if (this.nowPage > totalPages) this.nowPage = totalPages;
                const start = (this.nowPage - 1) * size;
                return { rows: filtered.slice(start, start + size), total: filtered.length };
            };
            let pageResult;
            if (this.mode == 1) {
                pageResult = toPage(this._onlineGoods);
                this.onlineGoods = pageResult.rows;
            } else if (this.mode == 2) {
                pageResult = toPage(this._offlineGoods);
                this.offlineGoods = pageResult.rows;
            } else {
                pageResult = toPage(this._offlineGoods1);
                this.offlineGoods1 = pageResult.rows;
            }
            this.total = pageResult.total;
        },
        // 搜索 / 清空关键字：回到第 1 页重新过滤
        search() {
            this.nowPage = 1;
            this.renderCurrent();
        },
        // 翻页（本地切片）
        handleCurrentChange(page) {
            this.nowPage = page;
            this.renderCurrent();
        },
        handleSelect(val) {
            if (this.mode !== val) {
                this.mode = val;
                if (val == 1) {
                    this.nowPage = 1;
                    this.getOnlineGoods();
                }
                if (val == 2) {
                    this.nowPage = 1;
                    this.getOfflineGoods();
                }
                if (val == 3) {
                    this.nowPage = 1;
                    this.getOfflineGoods1();
                }
            }
        },
        showPopup(item) {
            this.currentItem = item;
            this.popupVisible = true;
        },
        handleDialogConfirm() {
            // 处理确定按钮点击事件
            this.popupVisible = false;  // 假设点击确定也关闭对话框
        },
        // 打开原因填写弹窗（违规下架 / 审核未通过必填原因）
        askReason(type, index) {
            this.reasonTarget = { type, index };
            this.reasonText = '';
            this.reasonDialogVisible = true;
        },
        // 确认填写原因并执行状态更新
        confirmReason() {
            const reason = (this.reasonText || '').trim();
            if (!reason) {
                this.$message.error('请填写处理原因，卖家才能知道具体问题');
                return;
            }
            const t = this.reasonTarget;
            if (!t) return;
            this.reasonDialogVisible = false;
            if (t.type === 'offline') {
                this.doUpdate(this.onlineGoods[t.index].id, 4, reason, () => this.getOnlineGoods());
            } else if (t.type === 'reject') {
                this.doUpdate(this.offlineGoods[t.index].id, 4, reason, () => this.getOfflineGoods());
            }
        },
        // 统一状态更新：status + reason（reason 可空，如审核通过/重新上架）
        doUpdate(id, status, reason, refresh) {
            api.updateGoods({
                id,
                status,
                reason: reason || ''
            }).then(res => {
                if (res.status_code === 1) {
                    refresh();
                } else {
                    this.$message.error(res.msg);
                }
            }).catch(() => {
                this.$message.error('操作失败');
            });
        },
        relistOfflineGoods(i) {
            if (this.offlineGoods[i]) {
                this.doUpdate(this.offlineGoods[i].id, 1, '', () => this.getOfflineGoods());
            }
        },
        relistOfflineGoods1(i) {
            if (this.offlineGoods1[i]) {
                this.doUpdate(this.offlineGoods1[i].id, 1, '', () => this.getOfflineGoods1());
            }
        },
        deleteGoods(i) {
            api.deleteGood(this.offlineGoods1[i].id).then(res => {
                if (res.status_code == 1) {
                    this.getOfflineGoods1();
                } else {
                    this.$message.error(res.msg)
                }

            }).catch(() => {
                this.$message.error('删除失败');
            })
        },
        //正常商品
        getOnlineGoods() {
            api.getGoods({
                status: 1,
                page: 1,
                nums: 10000
            }).then(res => {
                if (res.status_code == 1) {
                    this._onlineGoods = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                    this.$router.push({ path: '/login-admin' });
                }
            }).catch(() => {
                this.$message.error('商品列表加载失败');
            })
        },
        //违规商品
        getOfflineGoods1() {
            api.getGoods({
                status: 4,
                page: 1,
                nums: 10000
            }).then(res => {
                if (res.status_code == 1) {
                    this._offlineGoods1 = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                    this.$router.push({ path: '/login-admin' });
                }
            }).catch(() => {
                this.$message.error('商品列表加载失败');
            })
        },
        //审核商品
        getOfflineGoods() {
            api.getGoods({
                status: 3,
                page: 1,
                nums: 10000
            }).then(res => {
                if (res.status_code == 1) {
                    this._offlineGoods = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                }
            }).catch(() => {
                this.$message.error('商品列表加载失败');
            })
        }
    }

}
</script>

<style scoped>
.main-border {
    background-color: #FFF;
    padding: 10px 30px;
    box-shadow: 0 1px 15px -6px rgba(0, 0, 0, .5);
    border-radius: 5px;
}

.block {
    display: flex;
    position: relative;
    justify-content: center;
    padding-top: 15px;
    padding-bottom: 10px;
    width: 100%;
}

.el-dialog {
    border-radius: 40px;
    /* 圆角 */
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    /* 阴影 */
}

.el-dialog__header {
    background-color: #f5f7fa;
    padding: 10px 20px;
    border-bottom: 1px solid #e9eef3;
}

.el-dialog__body {
    /* 自定义内容区域样式 */
    padding: 20px;
    /* 内边距 */
}

.dialog-footer {
    /* 自定义底部样式 */
    text-align: right;
    /* 文本右对齐 */
    padding: 10px;
    /* 内边距 */
    border-top: 1px solid #e9eef3;
    /* 上边框 */
}

.search-bar {
    width: 700px;
    height: 50px;
}

/* 操作按钮：统一尺寸、字号与圆角；窄屏自动换行，不再被挤压变形 */
.op-btns {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-items: center;
    justify-content: center;
}

.btn {
    min-width: 74px;
    height: 32px;
    padding: 0 12px;
    font-size: 14px;
    color: white;
    text-align: center;
    border-radius: 6px;
    box-sizing: border-box;
    display: flex;
    justify-content: center;
    align-items: center;
    white-space: nowrap;
    cursor: pointer;
    transition: opacity 0.2s ease;
}

.btn:hover {
    opacity: 0.85;
}

.btn-success {
    background-color: #67c23a;
}

.btn-danger {
    background-color: #f56c6c;
}

.btn-info {
    background-color: #409EFF;
}

.btn-warn {
    background-color: #e6a23c;
}
</style>
