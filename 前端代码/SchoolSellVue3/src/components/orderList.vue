<template>
    <div class="main-border">
        <!-- 统一搜索栏 -->
        <admin-search-bar v-model="searchText" placeholder="输入订单号 / 商品名称 / 用户ID 搜索" @search="search" />
        <el-table :data="Order" stripe empty-text="没有匹配的订单" style="width: 100%;color: #5a5c61;">
            <el-table-column prop="userId" label="顾客id" show-overflow-tooltip width="65">
            </el-table-column>
            <el-table-column prop="idleItem.userId" label="商家id" show-overflow-tooltip width="65">
            </el-table-column>
            <el-table-column prop="orderNumber" label="订单号" show-overflow-tooltip width="180">
            </el-table-column>
            <el-table-column prop="idleItem.idleName" label="商品名称" show-overflow-tooltip>
            </el-table-column>
            <el-table-column prop="orderPrice" label="金额" show-overflow-tooltip min-width="60" width="60">
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" show-overflow-tooltip min-width="150" width="150">
            </el-table-column>
            <el-table-column label="订单状态" width="100" show-overflow-tooltip>
                <template #default="scope">
                    <div>{{ orderStatus[scope.row.orderStatus] }}</div>
                </template>
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <div class="button3" @click="deleteOrder(scope.$index)">删除</div>
                </template>
            </el-table-column>
        </el-table>
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
 * - this.$api → import api from '@/api'（getOrderList/deleteOrder 均为 adminRequest，
 *   deleteOrder 由 GET 改 POST /admin/deleteOrder）
 * - slot-scope="scope" → #default="scope"
 * - 删除被注释的死代码：checkOrderTimeout 轮询、分页、computed.filteredOrders、
 *   未使用的 paymentStatus/searchValue 数据项；删除 console.log
 */
import api from '@/api'

export default {
    name: "orderList",
    created() {
        this.getOrder();
    },
    methods: {
        // 展示 = 全量订单 → 按关键字过滤 → 按当前页切片（本地完成，翻页不请求后端）
        renderCurrent() {
            const kw = this.searchText.trim().toLowerCase();
            const size = this.pageSize;
            const filtered = this._order.filter(item => {
                if (!kw) return true;
                // 支持按 订单号 / 商品名称 / 买家id / 商家id 搜索
                const orderNo = String(item.orderNumber || '').toLowerCase();
                const idleName = String((item.idleItem && item.idleItem.idleName) || '').toLowerCase();
                const buyerId = String(item.userId || '');
                const sellerId = String((item.idleItem && item.idleItem.userId) || '');
                return orderNo.includes(kw) || idleName.includes(kw) ||
                    buyerId.includes(kw) || sellerId.includes(kw);
            });
            const totalPages = Math.max(1, Math.ceil(filtered.length / size));
            if (this.nowPage > totalPages) this.nowPage = totalPages;
            const start = (this.nowPage - 1) * size;
            this.Order = filtered.slice(start, start + size);
            this.total = filtered.length;
        },
        search() {
            this.nowPage = 1;
            this.renderCurrent();
        },
        handleCurrentChange(page) {
            this.nowPage = page;
            this.renderCurrent();
        },
        getOrder() {
            api.getOrderList({
                page: 1,
                nums: 10000
            }).then(res => {
                if (res.status_code == 1) {
                    this._order = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                }

            }).catch(() => {
            })
        },
        deleteOrder(index) {
            api.deleteOrder({
                id: this.Order[index].id
            }).then(res => {
                if (res.status_code == 1) {
                    this.getOrder();
                } else {
                    this.$message.error(res.msg)
                }

            }).catch(() => {
            })
        },
    },
    data() {
        return {
            mode: 1,
            nowPage: 1,
            total: 0,
            pageSize: 10,
            searchText: '',
            orderStatus: ['待付款', '待发货', '待收货', '已完成', '已取消'],
            _order: [], // 全量订单（表格展示的是它过滤后的结果）
            Order: []
        }
    },
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
.button1,
.button3 {
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
  margin: 0 4px;
  cursor: pointer;
  transition: opacity 0.2s ease;
}

.button1 {
  background-color: #409EFF;
}

.button3 {
  background-color: #f56c6c;
}

.button1:hover,
.button3:hover {
  opacity: 0.85;
}
</style>
