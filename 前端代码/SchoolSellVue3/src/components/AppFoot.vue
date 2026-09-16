<template>
    <div class="foot-container">
        <div class="foot-inner">
            <div class="author">
                <img src="@/assets/background.png" style="width: 32px; height: 32px; margin-right: 8px;" />
                闲品校园CampusTrade
            </div>
            <div class="copyright-info">
                © 2026 闲品校园CampusTrade 版权所有
            </div>
            <div class="contact-info">
                邮箱: <a href="mailto:2974452736@qq.com">2974452736@qq.com</a>
                <br>
                联系我们: <a href="tel:1234567890">123-456-7890</a>
            </div>
        </div>
    </div>
</template>

<script>
/*
 * 迁移要点（components/common/AppFoot.vue → components/AppFoot.vue）：
 * - 纯静态组件，无脚本逻辑变更
 * - scoped style 中背景图相对路径随目录层级变化：../../assets → ../assets
 */
export default {
    name: "Foot"
}
</script>

<style scoped>
.foot-container {
    width: 100%;
    display: flex;
    justify-content: center;
    height: 66px;
    padding-top: 10px;
    padding-bottom: 20px;
    background-color: #20232a;
}

/* 页脚内容固定 1000px 居中，与页眉、中间内容同宽，缩放时不再重排 */
.foot-inner {
    width: 1000px;
    height: 100%;
    display: flex;
    justify-content: space-between;
}

.author,
.copyright-info {
    width: 33.33%;
    color: #ffffff;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    text-align: center;
    font-size: 18px;
}

.contact-info {
    width: 33.33%;
    color: #ffffff;
    font-size: 16px;
    text-align: center;
    line-height: 1.9;
    padding-top: 10px;
}

.contact-info a {
    color: #79bbff;
    text-decoration: none;
}
</style>
