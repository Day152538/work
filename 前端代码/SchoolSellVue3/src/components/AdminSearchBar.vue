<template>
    <div class="admin-search-bar">
        <el-input v-model="search" :placeholder="placeholder" clearable class="admin-search-input"
            @keyup.enter="handleSearch" @clear="handleClear"></el-input>
        <el-button type="primary" plain class="admin-search-btn" @click="handleSearch">搜索</el-button>
    </div>
</template>

<script>
/**
 * 管理端统一的搜索栏组件：输入框 + 搜索按钮。
 * - 样式在所有管理列表页保持一致
 * - 回车 / 点搜索 → 触发 search 事件（携带当前文本）
 * - 点击清空（clearable 的 ×）→ 触发 search('')，用于恢复完整列表
 */
export default {
    name: 'AdminSearchBar',
    props: {
        modelValue: { type: String, default: '' },
        placeholder: { type: String, default: '请输入搜索关键词' }
    },
    emits: ['update:modelValue', 'search'],
    computed: {
        search: {
            get() {
                return this.modelValue
            },
            set(value) {
                this.$emit('update:modelValue', value)
            }
        }
    },
    methods: {
        handleSearch() {
            this.$emit('search', this.search)
        },
        handleClear() {
            // clearable 的 ×：值已清空，通知父级展示全部
            this.$emit('search', '')
        }
    }
}
</script>

<style scoped>
.admin-search-bar {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 14px;
}

.admin-search-input {
    width: 300px;
}
</style>
