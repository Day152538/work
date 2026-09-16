<template>
    <div class="header" :class="{ dark: !isDayTime }" :style="headerStyle">
        <div class="header-container">
            <div class="app-name">
                <router-link to="/">
                    <img src="@/assets/background.png" style="width: 40px; height: 40px; margin-right: 8px;" />
                    <b class="app-name-text">闲品校园CampusTrade</b>
                </router-link>

            </div>
            <div class="search-container">
                <el-input placeholder="输入你想搜索的商品..." v-model="searchValue" @keyup.enter="searchIdle"
                    size="small">
                    <template #append>
                        <el-button @click="searchIdle">
                            <Search style="width: 20px; height: 20px; color: white;" />
                        </el-button>
                    </template>
                </el-input>
            </div>
            <router-link v-if="!isLogin" class="user-name-text" to="/login">
                <div class="button1" style="margin-top: 7px; font-size: 15px;">
                    注册/登录</div>
            </router-link>
            <!-- placement=bottom-end：菜单右边缘对齐头像右缘，紧贴头像下方；
                 popper strategy=fixed：按视口定位，避免页面滚动/整页 zoom 缩放后
                 浮层相对头像发生错位（不同页面滚动高度不同时尤为明显） -->
            <el-dropdown trigger="click" placement="bottom-end"
                :popper-options="{ strategy: 'fixed' }" v-else>
                <div style="cursor:pointer;display: flex;align-items: center;">
                    <div class="avatar_set" :class="{ 'avatar_set-vip': isVip }">
                        <b>

                            {{ nicknameValue ? nicknameValue : nickname }}
                        </b>
                    </div>
                    <el-avatar :src="avatar" shape="square" :size="60"></el-avatar>
                </div>
                <template #dropdown>
                    <el-dropdown-menu>
                        <el-dropdown-item>
                            <div @click="toRelease">上传商品</div>
                        </el-dropdown-item>
                        <el-dropdown-item divided>
                            <div @click="toMe">个人中心</div>
                        </el-dropdown-item>
                        <el-dropdown-item divided>
                            <div @click="toMessage"> 消息</div>
                        </el-dropdown-item>
                        <el-dropdown-item divided>
                            <div @click="toAiChat"> 🤖 AI 助手</div>
                        </el-dropdown-item>
                        <el-dropdown-item divided style="color: red;">
                            <div @click="loginOut"> 退出登录</div>
                        </el-dropdown-item>
                    </el-dropdown-menu>
                </template>
            </el-dropdown>
            <div v-if="isDayTime" class="sun">
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
                <div class="ray"></div>
            </div>

            <div v-else class="moon">
            </div>
        </div>
    </div>
</template>
<script>
/*
 * 迁移要点（components/common/AppHeader.vue → components/AppHeader.vue）：
 * - 登录态/昵称/头像改读 Pinia userStore（isLogin、userInfo），
 *   头像地址统一 imageUrl()，不再拼 $store.state.baseApi
 * - 退出登录：api.logout()（结果可忽略）+ userStore.logout() + 跳 /login
 * - @keyup.enter.native → @keyup.enter；slot="dropdown" → #dropdown；
 *   el-icon-search 字体图标 → 全局注册的 <Search/> 组件
 * - mounted 里的 setInterval 在 beforeUnmount 清理（原版泄漏）
 * - headerStyle 内联背景图改用顶部 import 的资源（Vite 无 require）
 */
import api from '@/api'
import { useUserStore } from '@/stores/user'
import { imageUrl } from '@/utils/request'
import backImg from '@/assets/back.png'

export default {
    name: 'Header',
    props: ['searchInput', 'nicknameValue', 'avatarValue'],
    data() {
        return {
            searchValue: this.searchInput,
            isDayTime: new Date().getHours() >= 6 && new Date().getHours() < 18,
            headerTimer: null
        };
    },
    created() {
        this.userStore = useUserStore();
        // 兜底：有 token 但内存还没有用户资料时主动恢复一次，
        // 避免“登录成功跳首页”等场景顶栏仍停留在“注册/登录”
        if (localStorage.getItem('token') && !this.userStore.isLogin) {
            this.userStore.refreshUser(api).catch(() => { });
        }
    },
    mounted() {
        this.checkTime();
        this.updateHeaderBackground();
        this.headerTimer = setInterval(this.updateHeaderBackground, 60000); // 每分钟更新一次
    },
    beforeUnmount() {
        clearInterval(this.headerTimer);
    },
    watch: {
        // 父级（搜索页）路由变化更新关键字后，保持输入框内容同步
        searchInput(value) {
            this.searchValue = value || '';
        }
    },
    computed: {
        isLogin() {
            // 只要存在用户 token 就视为已登录：即使 userInfo 还没恢复，
            // 也先显示头像菜单（可点击进入个人中心等），而不是卡在“注册/登录”
            return this.userStore.isLogin || !!localStorage.getItem('token');
        },
        isVip() {
            return this.userStore && this.userStore.isVip;
        },
        nickname() {
            return this.userStore.userInfo.nickname || (this.isLogin ? '用户' : '登录');
        },
        avatar() {
            const avatar = this.userStore.userInfo.avatar;
            return avatar ? imageUrl(avatar) : '/avatar.jpg';
        },
        headerStyle() {
            if (this.isDayTime) {
                return {
                    backgroundColor: 'rgba(255, 255, 255)',
                    backgroundImage: `url(${backImg})`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center right',
                    color: 'black'
                };
            } else {
                return {
                    backgroundColor: 'black',
                    backgroundImage: 'none',
                    color: 'white'
                };
            }
        }
    },
    methods: {
        updateHeaderBackground() {
            var currentTime = new Date().getHours();
            this.isDayTime = currentTime >= 6 && currentTime < 18;
        },
        checkTime() {
            const now = new Date();
            const hour = now.getHours();
            if (hour >= 6 && hour < 18) {
                this.isDayTime = true;
            }
        },
        searchIdle() {
            const query = { searchValue: this.searchValue };
            if ('/search' !== this.$route.path) {
                this.$router.push({ path: '/search', query });
                return;
            }
            // 已在搜索页：search.vue 监听 $route 会自动重新拉取列表，直接 replace 即可。
            // 旧实现这里还调了 router.go(0) 整页刷新 → 二次全量加载、页面闪烁、滚动丢失。
            if (String(this.$route.query.searchValue || '') !== String(this.searchValue)) {
                this.$router.replace({ path: '/search', query });
            }
        },
        toMe() {
            if ('/me' !== this.$route.path) {
                this.$router.push({ path: '/me' });
            }
        },
        toMessage() {
            if ('/message' !== this.$route.path) {
                this.$router.push({ path: '/message' });
            }
        },
        toMessage1() {
            if ('/message1' !== this.$route.path) {
                this.$router.push({ path: '/message1' });
            }
        },
        toRelease() {
            if ('/release' !== this.$route.path) {
                this.$router.push({ path: '/release' });
            }
        },
        toAiChat() {
            // AI 助手已改为右下角悬浮面板：派发全局事件，由 App.vue 内的 AiAssistant 组件打开
            window.dispatchEvent(new CustomEvent('open-ai-assistant'));
        },
        loginOut() {
            // 调后端登出（结果可忽略，本地登录态照常清理），再统一回登录页
            api.logout().catch(() => { });
            this.userStore.logout();
            this.$router.push({ path: '/login' });
        }
    }
};
</script>
<style scoped>
.header {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    width: 100%;
    height: 80px;
    background-color: rgba(255, 255, 255);
    display: flex;
    justify-content: center;
    border-bottom: #ffffff00 solid 2px;
    z-index: 1000;
    overflow: hidden; /* 裁剪太阳光线的装饰性溢出，避免缩放时出现横向滚动条 */
    background: url("../assets/back.png") center right/ cover no-repeat;

}

.moon {
    width: 40px;
    height: 40px;
    background-color: #f0f0f0; /* 月亮的颜色 */
    border-radius: 50%; /* 让月亮呈圆形 */
    position: relative;
    overflow: hidden; /* 隐藏月亮外部的部分 */
}

.moon::before {
    content: '';
    position: absolute;
    width: 100%;
    height: 100%;
    background-color: black; /* 将背景色设置为透明 */
    border-radius: 50%; /* 让伪元素也呈圆形 */
    transform: translateX(-40%) rotate(-75deg); /* 调整月牙的位置 */
}

.sun {
    position: relative;
    width: 35px;
    height: 35px;
    margin-left: 30px;
    margin-top: 20px;
    background-color: yellow;
    border-radius: 50%;

}

.ray {
    position: absolute;
    width: 3px;
    height: 20px;
    border-left: 5px solid transparent; /* 左边为透明，形成三角形 */
    border-right: 5px solid transparent; /* 右边为透明，形成三角形 */
    border-bottom: 27px solid yellow; /* 底部为黄色，形成三角形 */
    bottom: 50%;
    left: 50%;
    transform-origin: bottom center;
    animation: rotate 6s linear infinite;
}

/* 为每条光线设置不同的初始角度和动画延迟 */
.ray:nth-child(1) {
    transform: translateX(-50px) rotate(0deg);
    animation-delay: -0.6s; /* 6s / 10 */
}

.ray:nth-child(2) {
    transform: translateX(-25px) rotate(36deg); /* 360° / 10 */
    animation-delay: -1.2s; /* 2 * (6s / 10) */
}

.ray:nth-child(3) {
    transform: translateX(0) rotate(72deg);
    animation-delay: -1.8s; /* 3 * (6s / 10) */
}

.ray:nth-child(4) {
    transform: translateX(25px) rotate(108deg);
    animation-delay: -2.4s; /* 4 * (6s / 10) */
}

.ray:nth-child(5) {
    transform: translateX(-50px) rotate(144deg);
    animation-delay: -3s; /* 5 * (6s / 10) */
}

.ray:nth-child(6) {
    transform: translateX(-25px) rotate(180deg);
    animation-delay: -3.6s; /* 6 * (6s / 10) */
}

.ray:nth-child(7) {
    transform: translateX(0) rotate(216deg);
    animation-delay: -4.2s; /* 7 * (6s / 10) */
}

.ray:nth-child(8) {
    transform: translateX(25px) rotate(252deg);
    animation-delay: -4.8s; /* 8 * (6s / 10) */
}

.ray:nth-child(9) {
    transform: translateX(50px) rotate(288deg);
    animation-delay: -5.4s; /* 9 * (6s / 10) */
}

.ray:nth-child(10) {
    transform: translateX(0) rotate(324deg); /* 注意这里我改为了324deg而不是360deg，因为我们通常不会在旋转动画中用到完整的360deg */
    animation-delay: -6s; /* 10 * (6s / 10) */
}

/* 添加更多光线时，继续修改角度和动画延迟 */

@keyframes rotate {
    from {
        transform: translateX(-50%) rotate(0deg);
    }

    to {
        transform: translateX(-50%) rotate(360deg);
    }
}

/* 太阳/月亮作为 header-container 最后一个 flex 子项，恒排在登录按钮右侧，
   任何宽度/缩放下都不会与登录按钮重叠 */
.sun,
.moon {
    margin-left: 25px; /* 与登录按钮的间距（旋转光线最长伸出约30px，25px足够） */
    margin-top: 0;     /* 清除原 .sun 的 margin-top，配合 align-items:center 垂直居中 */
}

.header-container {
    width: 1000px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.app-name a {
    color: #163f7d;
    font-size: 24px;
    text-decoration: none;
    display: flex;
    align-items: center;
}

.app-name-text {
    color: #163f7d;
    font-size: 26px;
    font-weight: 700;
    display: inline-block;
    letter-spacing: 1px;
}

/* 黑夜模式（18:00-6:00 黑色背景）：深蓝文字看不清 → logo/昵称变白色 */
.header.dark .app-name a,
.header.dark .app-name-text {
    color: #ffffff;
}
.header.dark .avatar_set {
    color: #ffffff;
}

.search-container {
    margin-top: 5px;
    width: 500px;
}

/* ============ 搜索框美化：蓝青渐变胶囊一体式 ============ */
.search-container :deep(.el-input-group) {
    border-radius: 999px;
    overflow: hidden;
    box-shadow: 0 4px 14px rgba(64, 158, 255, 0.20);
    transition: box-shadow 0.25s ease, transform 0.25s ease;
}
.search-container :deep(.el-input-group:hover) {
    box-shadow: 0 6px 22px rgba(64, 158, 255, 0.32);
    transform: translateY(-1px);
}
.search-container :deep(.el-input__wrapper) {
    border-radius: 999px 0 0 999px;
    box-shadow: none !important;
    background: #ffffff;
}
.search-container :deep(.el-input__inner) {
    font-size: 13px;
    color: #303133;
    letter-spacing: 0.5px;
}
.search-container :deep(.el-input-group__append) {
    background: linear-gradient(135deg, #4facfe, #00c6fb);
    border: none;
    border-radius: 0 999px 999px 0;
    padding: 0;
    box-shadow: none;
}
.search-container :deep(.el-input-group__append .el-button) {
    background: transparent;
    border: none;
    margin: 0;
    padding: 0 20px;
    height: 100%;
    border-radius: 0 999px 999px 0;
}
.search-container :deep(.el-input-group__append .el-button:hover) {
    background: rgba(255, 255, 255, 0.18);
}
.search-container :deep(.el-input-group__append .el-button:focus) {
    outline: none;
}

.user-name-text {
    font-size: 16px;
    font-weight: 600;
    color: hsl(210, 84%, 25%);
    cursor: pointer;
    text-decoration: none;
}

.avatar_set {
    font-size: 22px;
    font-weight: 600;
    color: #163f7d;
    padding-right: 8px;
    max-width: 160px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

/* 会员（VIP）专属：昵称炫彩渐变流动效果 */
.avatar_set-vip {
    background-image: -webkit-linear-gradient(left, blue, #66ffff 10%, #cc00ff 20%, #CC00CC 30%, #CCCCFF 40%, #00FFFF 50%, #CCCCFF 60%, #CC00CC 70%, #CC00FF 80%, #66FFFF 90%, blue 100%);
    -webkit-text-fill-color: transparent;
    /* 将字体设置成透明色 */
    background-clip: text;
    /* 裁剪背景图，使文字作为裁剪区域向外裁剪 */
    background-size: 200% 100%;
    animation: masked-animation 4s linear infinite;
}

@keyframes masked-animation {
    0% {
        background-position: 0 0;
    }

    100% {
        background-position: -100% 0;
    }
}

.button1 {
    width: 100px;
    height: 40px;
    font-size: 20px;
    color: white;
    text-align: center;
    background-color: #ff8c00;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    justify-content: center; /* 水平居中 */
    align-items: center; /* 垂直居中 */

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease; /* 添加过渡效果 */

}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
    background-color: #ffa733; /* 颜色变亮 */
}
</style>
