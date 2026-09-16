<template>
    <div>
        <app-head :searchInput="searchValue"></app-head>
        <app-body>
            <div style="min-height: 85vh;">
                <div style="margin: 0 20px;padding-top: 20px;">
                    <div style="text-align: center;color: #555555;padding: 20px;" v-if="idleList.length === 0">暂无匹配的物品...
                    </div>
                    <el-row :gutter="30">
                        <div class="price"><el-button @click="sortByIdlePrice" style="margin-left: 20px;margin-bottom: 5px;float:right;">按价格排序</el-button></div>
                        <el-col :span="6" v-for="(idle, index) in idleList" :key="index">
                            <div class="idle-card" @click="toDetails(idle)">
                                <el-image style="width: 100%; height: 160px" :src="imageUrl(idle.imgUrl)"
                                    fit="cover">
                                    <template #error>
                                        <div class="image-slot">加载失败。。。</div>
                                    </template>
                                </el-image>
                                <div class="idle-title">
                                    商品：{{ idle.idleName }}
                                </div>
                                <el-row style="margin: 5px 10px;">
                                    <el-col :span="14">
                                        <div class="idle-price">价格：￥{{ idle.idlePrice }}</div>
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
                    <div class="paging">
                        <el-pagination background @current-change="handleCurrentChange"
                            v-model:current-page="currentPage" :page-size="12"
                            layout="total, prev, pager, next, jumper" :total="totalItem">
                        </el-pagination>
                    </div>
                </div>
            </div>

        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 图片地址 $store.state.baseApi + imgUrl → imageUrl()（含旧数据 "/image?imageName=xxx" 兼容剥离）
 * 2. 兄弟组件相对路径 → '@/components/xxx.vue'
 * 3. el-image 旧 slot="error" → 具名插槽 #error；删除调试 console.log 与死代码（handleClick/分页注释块）
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { imageUrl } from '@/utils/request'

// 兼容旧数据：历史 pictureList 里存的是 "/image?imageName=xxx"，统一剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
    name: "search",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            idleList: [],
            currentPage: 1,
            searchValue: '',
            totalItem: 1,
            sortOrder: 'asc'
        };
    },
    created() {
        // 刷新/回退时按 URL 恢复页码（无 page 参数则默认第 1 页）
        this.findIdleTiem(this.$route.query.page, this.$route.query.searchValue);
        this.searchValue = this.$route.query.searchValue;
    },
    watch: {
        $route(to) {
            this.searchValue = to.query.searchValue;
            this.findIdleTiem(to.query.page, to.query.searchValue);
        }
    },
    methods: {
        imageUrl,
        findIdleTiem(page, findValue) {
            const p = page ? parseInt(page) : 1;
            this.currentPage = p;
            api.findIdleTiem({
                page: p,
                nums: 12,
                findValue: findValue
            }).then(res => {
                let list = res.data.list;
                for (let i = 0; i < list.length; i++) {
                    list[i].timeStr = list[i].releaseTime.substring(0, 10) + " " + list[i].releaseTime.substring(11, 19);
                    let pictureList = JSON.parse(list[i].pictureList);
                    list[i].imgUrl = pictureList.length > 0 ? toImageName(pictureList[0]) : '';
                }
                this.idleList = list;
                this.totalItem = res.data.count;
            }).catch(() => {
                this.$message.error('搜索失败，请稍后重试');
            })
        },
        handleCurrentChange(val) {
            this.$router.replace({ query: { page: val, searchValue: this.searchValue } });
        },
        toDetails(idle) {
            this.$router.push({ path: '/details', query: { id: idle.id } });
        },
        sortByIdlePrice() {
            if (this.sortOrder === 'asc') {
                this.idleList.sort((a, b) => a.idlePrice - b.idlePrice);
                this.sortOrder = 'desc';
            } else {
                this.idleList.sort((a, b) => b.idlePrice - a.idlePrice);
                this.sortOrder = 'asc';
            }
        },
    }
}
</script>

<style scoped>
.idle-card {
    height: 275px;
    border: #eeeeee solid 3px;
    margin-bottom: 15px;
    cursor: pointer;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.23);
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

.price {
    width: 100%;
    float: right;
}

.paging {
    display: flex;
    justify-content: center;
    height: 60px;
    align-items: center;
}
</style>
