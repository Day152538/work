<template>
    <div style="background: white;min-height:100vh;">
        <el-header>
            <div class="header">
                <div class="app-name">
                    <router-link to="/platform-admin">
                    </router-link>
                </div>
                <span class="app-title">
                    <span
                        style="color: white;display: inline-block;margin-top: 5px; margin-left: 150px; font-size: 24px;">
                        欢迎使用闲品校园CampusTrade管理系统<b>{{ admin.nickname }}</b>
                    </span>
                </span>
                <div class="app-logOut">
                    <el-button style="margin-right: 100px" type="primary" @click="logout"> 退出账号</el-button>
                </div>
            </div>
        </el-header>
        <el-container>
            <div class="mainBody" background-color="black">
                <el-aside style="background-color: rgb(16 16 16);position: fixed;height: 800px;">
                    <el-col :span="24">
                        <el-menu default-active="1" class="el-menu-vertical-demo" @select="handleSelect"
                            text-color="white" style="font-weight: bolder ;">
                            <el-menu-item index="1" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span> 商品交易管理</span>
                            </el-menu-item>
                            <el-menu-item index="2" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span> 商品订单管理</span>
                            </el-menu-item>
                            <el-menu-item index="3" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span> 人员信息管理</span>
                            </el-menu-item>
                            <el-menu-item index="4" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span> 商品分类管理</span>
                            </el-menu-item>
                            <el-menu-item index="5" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span> 轮播图管理</span>
                            </el-menu-item>
                            <el-menu-item index="8" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span>平台数据可视化</span>
                            </el-menu-item>
                            <el-menu-item index="9" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span>公告通知管理</span>
                            </el-menu-item>
                            <el-menu-item index="6" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;">
                                <span>反馈信息</span>
                            </el-menu-item>
                            <el-menu-item index="7" class="el-item-menu" active-text-color="skyblue"
                                style="padding-left: 30px;" @click="$router.push('/index')">
                                <span>返回前端</span>
                            </el-menu-item>
                        </el-menu>
                    </el-col>
                </el-aside>
                <el-main style="margin-left: 300px;">
                    <IdleGoods v-if="mode == 1"></IdleGoods>
                    <orderList v-if="mode == 2"></orderList>
                    <userList v-if="mode == 3"></userList>
                    <cartList v-if="mode == 4"></cartList>
                    <carousel v-if="mode == 5"></carousel>
                    <example v-if="mode == 6"></example>
                    <DataBoard v-if="mode == 8"></DataBoard>
                    <notice v-if="mode == 9"></notice>

                </el-main>
            </div>
        </el-container>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - 兄弟组件相对路径 ../common/xxx.vue → '@/components/xxx.vue'（路径映射）
 * - $sta.adminName（非响应式全局变量）→ Pinia useUserStore().adminInfo
 * - 退出登录：api.adminLoginOut() 后无论成败都清 admin_token（userStore.adminLogout()）
 *   并跳转 /login-admin，替代原 $api.loginOut + $sta 手动清空
 * - 删除 el-menu-item 上已废弃的 slot="title"；删除 console.log
 * - 修复原 CSS 中 `s .app-name` 的选择器手误（多余的 s 导致样式失效）
 */
import IdleGoods from '@/components/IdleGoods.vue'
import orderList from '@/components/orderList.vue'
import userList from '@/components/userList.vue'
import cartList from '@/components/cartList.vue'
import carousel from '@/components/carousel.vue'
import DataBoard from '@/components/DataBoard.vue'
import notice from '@/components/notice.vue'
import example from '@/components/example.vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'

export default {
    name: "platform-admin",
    components: {
        IdleGoods,
        orderList,
        userList,
        cartList,
        carousel,
        DataBoard,
        notice,
        example
    },
    data() {
        return {
            mode: 1,
            admin: {
                nickname: '管理员',
            },
            userStore: null,
        }
    },
    created() {
        this.userStore = useUserStore();
        this.admin.nickname = this.userStore.adminInfo?.adminName || '管理员';
    },
    methods: {
        logout() {
            api.adminLoginOut().catch(() => {
                // 退出接口失败也继续清理本地登录态
            }).finally(() => {
                // 清除 admin_token + adminInfo（store action 内部处理）
                this.userStore.adminLogout();
                this.$router.push({ path: '/login-admin' });
            });
        },
        handleSelect(val) {
            if (this.mode !== val) {
                this.mode = val
            }
        },
    },
}
</script>

<style scoped>
@keyframes gradientAnimation {
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

.header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    min-width: 100vw;
    height: 70px;
    background: linear-gradient(to right, #000000, #1e0394);
    /* 渐变色背景 */
    background-size: 200% 100%;
    animation: gradientAnimation 5s ease infinite;
    /* 5秒钟的动画，无限循环 */
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: lightgrey solid 2px;
    z-index: 1000;
    background: url("../assets/bei.jpg") center top / cover no-repeat;
}

.app-name {
    display: flex;
    justify-content: center;
    align-items: center;
    min-width: 11vw;
    flex: 1;
    height: 100%;
    border-right: 1px solid #fff;


}

.app-name a {
    color: #FFFFFF;
    font-size: 18px;
    font-weight: 800;
    text-decoration: none;
    padding: 10px 20px 0 0;
}

.app-title {
    display: flex;
    justify-content: center;
    flex: 8;
    color: black;
    font-size: 28px;
}

.app-logOut {
    display: flex;
    flex: 1;
    justify-content: flex-end;
    align-items: center;
}

.mainBody {
    display: flex;
    width: 100%;
    background: url("../assets/biejing.png") center top / cover no-repeat;
}

aside {
    /* flex: 1; */
    box-sizing: content-box;
    min-height: calc(100vh - 100px);
    background-color: rgb(255, 255, 255);
    border-top: 1px solid lightgrey;
    border-right: 1px solid lightgrey;
}

main {
    flex: 9;
}
.el-menu-vertical-demo{
    background: url("../assets/ce1.png") center top / cover no-repeat;}
.foot {
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 58px;
    background-color: #ffffff;
}
</style>
