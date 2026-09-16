<template>
    <div class="main-container">
        <div class="main-content">
            <slot></slot>
        </div>
    </div>
</template>

<script>
/*
 * 迁移要点（components/common/AppPageBody.vue → components/AppPageBody.vue）：
 * - 纯插槽布局组件，无脚本逻辑变更
 * - scoped style 中背景图相对路径随目录层级变化：../../assets → ../assets
 */
export default {
    name: "PageBody"
}
</script>

<style scoped>
.main-container {
    margin-top: 70px;
    display: flex;
    justify-content: center;
    background-color: #f2f6f8;
    background: url("../assets/bobyback.png") center top / cover no-repeat;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4); /* Add shadow */

}

.main-content {
    width: 1000px;
    background-color: rgba(255, 255, 255, 0.8);
    /* 白色背景，% 透明度 */
    min-height: 90vh;
    margin-top: 15px;
    border-top-left-radius: 20px;
    border-top-right-radius: 20px;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.4); /* Add shadow */
}
</style>
