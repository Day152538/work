<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="announcement-bar" v-if="show && announcementList.length > 0">
                <div class="scrolling-text-container" :style="{ animationDuration: animationDuration }">
                    <span v-for="notice in announcementList" :key="notice.id">{{ notice.content }}</span>
                </div>
            </div>
            <div class="slider">
                <el-carousel ref="carouselRef" height="370px" style=" width: 1000px;" :autoplay="true"
                    :interval="autoPlayInterval">
                    <el-carousel-item v-for="carousel in carousels" :key="carousel.id">
                        <img style="height: 370px; width: 1000px; object-fit: cover; background-color: black;"
                            :src="carouselImg(carousel)" @click="toDetails1(carousel)" />
                    </el-carousel-item>
                </el-carousel>
                <div class="controls">
                    <button class="control-button prev" @click="prev"></button>
                    <button class="control-button next" @click="next"></button>
                </div>
            </div>
            <div class="he" style="min-height: 85vh;">
                <el-tabs v-model="labelName" type="card" @tab-click="handleClick">
                    <el-tab-pane label="全部" name="0" class="red"></el-tab-pane>
                    <el-tab-pane v-for="item in typeList" :key="item.id" :label="item.name"
                        :name="item.id + ''"></el-tab-pane>
                </el-tabs>
                <div style="margin: 0 20px;">
                    <el-row :gutter="30">
                        <el-col :span="6" v-for="(idle, index) in idleList" :key="index">
                            <div class="idle-card" @click="toDetails(idle)">
                                <el-image style="width: 100%; height: 160px" :src="imageUrl(idle.imgUrl)"
                                    fit="cover">
                                    <template #error>
                                        <div class="image-slot">加载失败</div>
                                    </template>
                                </el-image>
                                <div class="idle-title">
                                    商品：{{ idle.idleName }}
                                </div>
                                <el-row style="margin: 5px 10px;">
                                    <el-col :span="14">
                                        <div class="idle-price" style="font-size: medium;">价格：￥{{ idle.idlePrice }}
                                        </div>
                                    </el-col>
                                    <el-col :span="10">
                                        <div class="idle-place">{{ idle.idlePlace }}</div>
                                    </el-col>
                                </el-row>
                                <div class="idle-sales"><template
                                        v-if="idle.salesCount > 0">已售 {{ idle.salesCount }} 件 · </template>库存
                                    {{ idle.stock }} 件</div>
                            </div>
                        </el-col>
                    </el-row>
                </div>
                <div class="paging">
                    <el-pagination background @current-change="handleCurrentChange" v-model:current-page="currentPage"
                        :page-size="12" layout="prev, pager, next, jumper" :total="totalItem">
                    </el-pagination>
                </div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 删除了"以用户身份调管理端 getOrderList/updateOrder 批量取消超时订单"的越权逻辑
 *    （后端 /admin/** 已要求 admin 角色 JWT，原逻辑必然 401，且属于越权操作）
 * 2. 轮播图列表改走公开接口 GET /carousel/all（原 axios 硬编码 localhost:9321）
 * 3. 图片地址全部改用 imageUrl()，公告改走 api.getNotice()
 * 4. el-pagination :current-page.sync → v-model:current-page；el-image error 具名插槽改 #error
 * 5. 删除手写 setInterval 自动轮播（el-carousel 自带 autoplay），按钮改调 el-carousel 实例方法
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { request, imageUrl } from '@/utils/request'

// 兼容旧数据：历史 pictureList 里存的是 "/image?imageName=xxx"，统一剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
    name: "index",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            labelName: '0',
            idleList: [],
            currentPage: 1,
            totalItem: 1,
            typeList: [],
            carousels: [],
            autoPlayInterval: 3000,
            show: true,
            announcementList: [],
            animationDuration: "60s"
        };
    },
    created() {
        // 刷新/回退到带 query 的地址时，按 URL 恢复标签与页码（watch $route 不处理首次进入）
        this.labelName = this.$route.query.labelName || '0';
        this.currentPage = parseInt(this.$route.query.page) ? parseInt(this.$route.query.page) : 1;
        this.findIdleTiem(this.currentPage)
        api.listType({ begin: 0, size: 999 }).then((res) => {
            this.typeList = res.data;
        });
        request.get('/carousel/all').then((res) => {
            if (res.status_code === 1) {
                this.carousels = res.data || [];
            }
        }).catch(() => {
            this.$message.error('轮播图加载失败');
        });
        this.fetchPopupContent();
    },
    watch: {
        $route(to) {
            this.labelName = to.query.labelName || '0';
            let val = parseInt(to.query.page) ? parseInt(to.query.page) : 1;
            this.currentPage = val;
            this.findIdleTiem(val);
        }
    },
    methods: {
        imageUrl,
        // 轮播图首图地址（后端 /carousel/all 返回的图片字段是 imgs=商品 picture_list，
        // 兼容旧数据的 img 字段）
        carouselImg(carousel) {
            let raw = '';
            if (Array.isArray(carousel.imgs) && carousel.imgs.length > 0) {
                raw = carousel.imgs[0];
            } else {
                raw = carousel.imgs || carousel.img || '';
            }
            try {
                const list = JSON.parse(raw);
                return list.length > 0 ? imageUrl(toImageName(list[0])) : '';
            } catch (e) {
                // raw 不是 JSON 时按纯文件名处理
                return raw ? imageUrl(toImageName(raw)) : '';
            }
        },
        fetchPopupContent() {
            api.getNotice().then(res => {
                if (res.status_code === 1) {
                    this.announcementList = res.data || [];
                }
            }).catch(() => {
                this.$message.error('公告加载失败');
            });
        },
        next() {
            this.$refs.carouselRef && this.$refs.carouselRef.next();
        },
        prev() {
            this.$refs.carouselRef && this.$refs.carouselRef.prev();
        },
        toDetails1(idle) {
            this.$router.push({ path: '/details', query: { id: idle.goodId } });
        },
        findIdleTiem(page) {
            const loading = this.$loading({
                lock: true,
                text: '加载数据中',
                background: 'rgba(0, 0, 0, 0)'
            });
            const handleList = (res) => {
                let list = res.data.list;
                for (let i = 0; i < list.length; i++) {
                    list[i].timeStr = list[i].releaseTime.substring(0, 10) + " " + list[i].releaseTime.substring(11, 19);
                    let pictureList = JSON.parse(list[i].pictureList);
                    list[i].imgUrl = pictureList.length > 0 ? toImageName(pictureList[0]) : '';
                }
                this.idleList = list;
                this.totalItem = res.data.count;
            };
            if (this.labelName > 0) {
                api.findIdleTiemByLable({
                    idleLabel: this.labelName,
                    page: page,
                    nums: 12
                }).then(res => {
                    handleList(res);
                }).catch(() => {
                    this.$message.error('数据加载失败');
                }).finally(() => {
                    loading.close();
                })
            } else {
                api.findIdleTiem({
                    page: page,
                    nums: 12
                }).then(res => {
                    handleList(res);
                }).catch(() => {
                    this.$message.error('数据加载失败');
                }).finally(() => {
                    loading.close();
                })
            }
        },
        handleClick(tab) {
            // Element Plus 的 tab-click 先于 v-model 更新触发，此时 this.labelName 仍是旧值，
            // 若用它跳转，第一次点击永远跳到上一个分类（表现为“要点两次才生效”）。
            // 因此直接取被点击标签自带的 name（pane.props.name / paneName）。
            const labelName = String((tab && (tab.props && tab.props.name)) || (tab && tab.paneName) || this.labelName);
            this.$router.replace({ query: { page: 1, labelName } });
        },
        handleCurrentChange(val) {
            this.$router.replace({ query: { page: val, labelName: this.labelName } });
        },
        toDetails(idle) {
            this.$router.push({ path: '/details', query: { id: idle.id } });
        }
    }
}
</script>

<style scoped>
/* ============ 类型栏目：胶囊式渐变选中（更唯美） ============ */
.he :deep(.el-tabs__header) {
    margin: 16px 20px 14px;
    border-bottom: none;
}
.he :deep(.el-tabs__nav) {
    border: none !important;
    display: inline-flex;
    flex-wrap: wrap;
    gap: 8px;
    float: none;
}
.he :deep(.el-tabs__item) {
    height: 34px;
    line-height: 34px;
    padding: 0 20px;
    border: none !important;
    border-radius: 999px;
    background: #f4f7fb;
    color: #606266;
    font-size: 14px;
    transition: all 0.25s ease;
    margin: 0 !important;
}
.he :deep(.el-tabs__item:hover) {
    color: #409eff;
    background: #ecf5ff;
}
.he :deep(.el-tabs__item.is-active) {
    background: linear-gradient(135deg, #4facfe, #00c6fb);
    color: #ffffff;
    font-weight: 600;
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.35);
}
/* 隐藏默认的激活下划线 */
.he :deep(.el-tabs__active-bar) {
    display: none;
}
.he :deep(.el-tabs__nav-wrap::after) {
    display: none;
}

.idle-card {
    height: 275px;
    border: 1px solid #e8e8e8;
    border-radius: 10px;
    margin-bottom: 15px;
    cursor: pointer;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    transition: box-shadow 0.2s ease, transform 0.2s ease;
}

.idle-card:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
}

.idle-sales {
    color: #999999;
    font-size: 12px;
    margin: 0 10px;
    text-align: right;
}

.paging {
    display: flex;
    justify-content: center;
    height: 60px;
    align-items: center;
}

.idle-title {
    font-size: 18px;
    font-weight: 600;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    margin: 10px;
}

.idle-price {
    font-size: 16px;
    color: red;
}

.idle-place {
    font-size: 13px;
    color: #666666;
    float: right;
    padding-right: 20px;

}

.idle-time {
    color: #666666;
    font-size: 12px;
    margin: 0 10px;
}

.user-nickname {
    color: #999999;
    font-size: 12px;
    display: flex;
    align-items: center;
    height: 30px;
    padding-left: 10px;
}

.user-info {
    margin-top: 10px;
    float: right;
    padding: 5px 10px;
    height: 30px;
    display: flex;
}

.slider {
    position: relative;
    overflow: hidden;
    width: 1000px;
    /* Adjust according to your needs */
}

.controls {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 100%;
    display: flex;
    justify-content: space-between;
    z-index: 1;
}

.control-button {
    background: none;
    border: none;
    font-size: 2rem;
    color: #fff;
    text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.5);
    cursor: pointer;
    outline: none;
}

.control-button.prev {
    margin-left: 20px;
}

.control-button.next {
    margin-right: 20px;
}

.popup {
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background-color: #ffffff;
    padding: 20px;
    border: 1px solid #dcdcdc;
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
    z-index: 999;
    max-width: 50%; /* 最大宽度限制 */
    overflow-y: auto; /* 滚动条 */
}

.popup-content {
    text-align: center;
}

.popup button {
    margin-top: 20px;
    padding: 10px 20px;
    background-color: #007bff;
    color: #ffffff;
    border: none;
    border-radius: 5px;
    cursor: pointer;
    transition: background-color 0.3s ease;
}

.popup button:hover {
    background-color: #0056b3;
}

.announcement-bar {
    background-color: #ffffff; /* 通告条背景色 */
    color: #ff0000; /* 文字颜色 */
    overflow: hidden;
    border-top-left-radius: 5px;
    border-top-right-radius: 5px;
    height: 20px;
}

.scrolling-text-container {
    display: inline-block;
    white-space: nowrap;
    animation: scrolling linear infinite; /* 滚动动画 */
}

@keyframes scrolling {
    0% {
        transform: translateX(20%); /* 将初始状态设置为从容器左边缘开始 */
    }

    100% {
        transform: translateX(-100%);
    }
}
</style>
