<template>
    <div class="db-root">
        <!-- 网格线装饰（科技风大屏底纹） -->
        <div class="db-grid"></div>

        <!-- 顶部标题栏 -->
        <div class="db-head">
            <div class="db-title">
                <span class="db-title-cn">平台数据可视化</span>
                <span class="db-title-en">PLATFORM DATA BOARD</span>
            </div>
            <div class="db-head-right">
                <span class="db-time">{{ now }}</span>
                <el-button class="db-refresh" :loading="loading" size="small" @click="loadAll">
                    ⟳ 刷新数据
                </el-button>
            </div>
        </div>

        <!-- KPI 指标卡 -->
        <div class="db-kpis">
            <div v-for="k in kpiList" :key="k.key" class="db-kpi">
                <div class="db-kpi-ico">{{ k.icon }}</div>
                <div class="db-kpi-body">
                    <div class="db-kpi-num">{{ formatNum(kpis[k.key]) }}<span v-if="k.unit" class="db-kpi-unit">{{ k.unit }}</span></div>
                    <div class="db-kpi-label">{{ k.label }}</div>
                </div>
            </div>
        </div>

        <!-- 图表网格 -->
        <div class="db-charts">
            <!-- 各类收入柱状图 -->
            <div class="db-chart">
                <div class="db-chart-head"><span class="db-dot"></span>各类收入对比<span class="db-tag">BAR</span></div>
                <div ref="bar" class="db-chart-body"></div>
            </div>
            <!-- 收入构成环形图 -->
            <div class="db-chart">
                <div class="db-chart-head"><span class="db-dot"></span>收入构成占比<span class="db-tag">DONUT</span></div>
                <div ref="donut" class="db-chart-body"></div>
            </div>
            <!-- 商品状态分布 -->
            <div class="db-chart">
                <div class="db-chart-head"><span class="db-dot"></span>商品状态分布<span class="db-tag">ROSE</span></div>
                <div ref="goods" class="db-chart-body"></div>
            </div>
            <!-- 订单状态分布 -->
            <div class="db-chart">
                <div class="db-chart-head"><span class="db-dot"></span>订单状态分布<span class="db-tag">RING</span></div>
                <div ref="orders" class="db-chart-body"></div>
            </div>
            <!-- 用户状态分布 -->
            <div class="db-chart">
                <div class="db-chart-head"><span class="db-dot"></span>用户状态分布<span class="db-tag">PIE</span></div>
                <div ref="users" class="db-chart-body"></div>
            </div>
            <!-- 最新公告 -->
            <div class="db-chart db-notice">
                <div class="db-chart-head"><span class="db-dot"></span>最新公告<span class="db-tag">NOTICE</span></div>
                <div class="db-chart-body db-notice-body">
                    <div v-if="notices.length" class="db-notice-list">
                        <div v-for="(n, i) in notices" :key="i" class="db-notice-item">
                            <span class="db-notice-ico">📢</span>
                            <span class="db-notice-text">{{ n.content }}</span>
                            <span v-if="n.createTime" class="db-notice-time">{{ n.createTime }}</span>
                        </div>
                    </div>
                    <div v-else class="db-empty">暂无公告</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
/**
 * 平台数据可视化（DataBoard，2026-09 重写自 Income.vue）
 *
 * 数据全部来自既有管理端接口（admin_token），不改后端：
 *  - 商品：/admin/idleList?status=1/2/3/4  → 状态分布 + 在售数 + 商品总量
 *  - 用户：/admin/userList?status=0/1       → 正常/封禁分布
 *  - 订单：/admin/orderList                 → 订单状态分布 + 已支付销售额
 *  - 收入：/api/income/chart                → 各类收入（柱状 + 环形）
 *  - 公告：/notices                         → 最新公告
 *
 * 视觉：深色科技风大屏（深蓝黑渐变 + 青色霓虹边框 + 网格底纹 + 发光图表），
 * 交互：KPI 数字滚动、图表 tooltip、卡片 hover 发光、实时时钟、手动刷新。
 */
import * as echarts from 'echarts'
import api from '@/api'

// 科技风通用色
const C = {
    cyan: '#00e5ff',
    blue: '#4facfe',
    purple: '#7c6cff',
    text: '#b8d6ff',
    textDim: '#5f7ea8',
    gridLine: 'rgba(0,229,255,0.08)'
}
// 分类渐变（柱状图）
const BAR_GRAD = [
    ['#00e5ff', '#0090ff'],
    ['#4facfe', '#7c4dff'],
    ['#00d4a0', '#00a0e5'],
    ['#ffb84d', '#ff7a3d'],
    ['#ff6fb5', '#b04dff'],
    ['#67e8f9', '#3b82f6'],
    ['#a3e635', '#22d3ee']
]

export default {
    name: 'DataBoard',
    data() {
        return {
            loading: false,
            now: '',
            kpis: { goodsTotal: 0, goodsOnSale: 0, userTotal: 0, orderTotal: 0, saleAmount: 0, incomeTotal: 0 },
            kpiList: [
                { key: 'goodsTotal', icon: '📦', label: '商品总量', unit: '' },
                { key: 'goodsOnSale', icon: '🛒', label: '在售商品', unit: '' },
                { key: 'userTotal', icon: '👥', label: '注册用户', unit: '' },
                { key: 'orderTotal', icon: '🧾', label: '订单总量', unit: '' },
                { key: 'saleAmount', icon: '💰', label: '成交销售额', unit: '元' },
                { key: 'incomeTotal', icon: '📈', label: '平台总收入', unit: '元' }
            ],
            notices: [],
            // echarts 实例
            charts: {},
            timers: []
        }
    },
    mounted() {
        this.updateTime()
        this.timers.push(setInterval(this.updateTime, 1000))
        this.initCharts()
        this.loadAll()
        window.addEventListener('resize', this.resizeAll)
    },
    beforeUnmount() {
        window.removeEventListener('resize', this.resizeAll)
        this.timers.forEach((t) => clearInterval(t))
        this.timers = []
        Object.keys(this.charts).forEach((k) => {
            this.charts[k] && this.charts[k].dispose()
            this.charts[k] = null
        })
    },
    methods: {
        updateTime() {
            const d = new Date()
            const p = (n) => String(n).padStart(2, '0')
            this.now = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
        },
        formatNum(v) {
            return Number(v || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
        },
        // ==================== 图表初始化 ====================
        initCharts() {
            this.charts.bar = echarts.init(this.$refs.bar)
            this.charts.donut = echarts.init(this.$refs.donut)
            this.charts.goods = echarts.init(this.$refs.goods)
            this.charts.orders = echarts.init(this.$refs.orders)
            this.charts.users = echarts.init(this.$refs.users)
        },
        resizeAll() {
            Object.keys(this.charts).forEach((k) => this.charts[k] && this.charts[k].resize())
        },
        // 通用 tooltip（科技风）
        tip() {
            return {
                backgroundColor: 'rgba(6,20,42,0.94)',
                borderColor: 'rgba(0,229,255,0.45)',
                borderWidth: 1,
                textStyle: { color: '#d8f2ff', fontSize: 12 },
                extraCssText: 'box-shadow:0 0 18px rgba(0,229,255,0.25);border-radius:8px;'
            }
        },
        // ==================== 数据加载 ====================
        loadAll() {
            if (this.loading) return
            this.loading = true
            Promise.all([
                api.getGoods({ status: 1, page: 1, nums: 10000 }),
                api.getGoods({ status: 2, page: 1, nums: 10000 }),
                api.getGoods({ status: 3, page: 1, nums: 10000 }),
                api.getGoods({ status: 4, page: 1, nums: 10000 }),
                api.getUserData({ status: 0, page: 1, nums: 10000 }),
                api.getUserData({ status: 1, page: 1, nums: 10000 }),
                api.getOrderList({ page: 1, nums: 10000 }),
                api.getIncomeChart(),
                api.getNotice()
            ]).then(([s1, s2, s3, s4, u0, u1, orders, income, notices]) => {
                const goods = [s1, s2, s3, s4].map((r) => (r.status_code === 1 ? r.data.list || [] : []))
                const users = [u0, u1].map((r) => (r.status_code === 1 ? r.data.list || [] : []))
                const orderList = orders.status_code === 1 ? orders.data.list || [] : []
                const inc = income.status_code === 1 && income.data ? income.data.categoryIncomes || [] : []
                const nt = notices.status_code === 1 ? notices.data || [] : []

                this.notices = Array.isArray(nt) ? nt.slice(0, 5) : []
                this.renderAll(goods, users, orderList, inc)
            }).catch(() => {
                // 网络/401 已由 adminRequest 拦截器统一提示
            }).finally(() => {
                this.loading = false
            })
        },
        renderAll(goods, users, orderList, inc) {
            // ---- KPI ----
            const goodsTotal = goods[0].length + goods[1].length + goods[2].length + goods[3].length
            const userTotal = users[0].length + users[1].length
            let saleAmount = 0
            const orderStatus = [0, 0, 0, 0, 0] // 待付款/待发货/待收货/已完成/已取消
            orderList.forEach((o) => {
                const st = Number(o.orderStatus)
                if (st >= 0 && st <= 4) orderStatus[st]++
                if (Number(o.paymentStatus) === 1) saleAmount += Number(o.orderPrice) || 0
            })
            const incomeTotal = inc.reduce((a, c) => a + (Number(c.categoryIncome) || 0), 0)

            this.animateKpi('goodsTotal', goodsTotal)
            this.animateKpi('goodsOnSale', goods[0].length)
            this.animateKpi('userTotal', userTotal)
            this.animateKpi('orderTotal', orderList.length)
            this.animateKpi('saleAmount', Math.round(saleAmount))
            this.animateKpi('incomeTotal', Math.round(incomeTotal))

            // ---- 图表 ----
            this.setBar(inc)
            this.setDonut(inc, incomeTotal)
            this.setGoodsRose([
                { name: '在售', value: goods[0].length },
                { name: '下架', value: goods[1].length },
                { name: '待审核', value: goods[2].length },
                { name: '违规', value: goods[3].length }
            ])
            this.setOrdersRing([
                { name: '待付款', value: orderStatus[0] },
                { name: '待发货', value: orderStatus[1] },
                { name: '待收货', value: orderStatus[2] },
                { name: '已完成', value: orderStatus[3] },
                { name: '已取消', value: orderStatus[4] }
            ])
            this.setUsersPie([
                { name: '正常', value: users[0].length },
                { name: '封禁', value: users[1].length }
            ])
        },
        // KPI 数字滚动
        animateKpi(key, target, duration = 900) {
            const start = Number(this.kpis[key]) || 0
            if (start === target) return
            const t0 = performance.now()
            const step = (t) => {
                const p = Math.min(1, (t - t0) / duration)
                const eased = 1 - Math.pow(1 - p, 3)
                this.kpis[key] = Math.round(start + (target - start) * eased)
                if (p < 1) requestAnimationFrame(step)
            }
            requestAnimationFrame(step)
        },
        // ==================== 各图表 option ====================
        setBar(inc) {
            const names = inc.map((i) => i.categoryName)
            const values = inc.map((i) => Number(i.categoryIncome) || 0)
            this.charts.bar.setOption({
                backgroundColor: 'transparent',
                tooltip: Object.assign(this.tip(), {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(0,229,255,0.06)' } },
                    valueFormatter: (v) => '￥' + Number(v).toLocaleString()
                }),
                grid: { left: 14, right: 16, top: 30, bottom: 8, containLabel: true },
                xAxis: {
                    type: 'category',
                    data: names,
                    axisLine: { lineStyle: { color: C.gridLine } },
                    axisTick: { show: false },
                    axisLabel: { color: C.text, fontSize: 11 }
                },
                yAxis: {
                    type: 'value',
                    axisLabel: { color: C.textDim, fontSize: 10 },
                    splitLine: { lineStyle: { color: C.gridLine } }
                },
                series: [{
                    type: 'bar',
                    data: values.map((v, i) => ({
                        value: v,
                        itemStyle: {
                            borderRadius: [6, 6, 0, 0],
                            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                                { offset: 0, color: BAR_GRAD[i % BAR_GRAD.length][0] },
                                { offset: 1, color: BAR_GRAD[i % BAR_GRAD.length][1] }
                            ]),
                            shadowColor: 'rgba(0,229,255,0.45)',
                            shadowBlur: 12
                        }
                    })),
                    barWidth: 26,
                    label: {
                        show: true,
                        position: 'top',
                        color: '#8fd8ff',
                        fontSize: 10,
                        formatter: (p) => (Number(p.value) ? Number(p.value).toLocaleString() : '')
                    }
                }]
            })
        },
        setDonut(inc, total) {
            this.charts.donut.setOption({
                backgroundColor: 'transparent',
                tooltip: Object.assign(this.tip(), {
                    trigger: 'item',
                    formatter: (p) => `${p.name}：￥${Number(p.value).toLocaleString()}<br/>占比 ${p.percent}%`
                }),
                legend: {
                    orient: 'vertical',
                    right: 8,
                    top: 'center',
                    itemWidth: 10,
                    itemHeight: 10,
                    icon: 'circle',
                    textStyle: { color: C.text, fontSize: 11 },
                    formatter: (name) => name
                },
                series: [{
                    type: 'pie',
                    radius: ['48%', '72%'],
                    center: ['38%', '50%'],
                    avoidLabelOverlap: true,
                    itemStyle: {
                        borderColor: 'rgba(10,20,45,0.9)',
                        borderWidth: 2,
                        shadowColor: 'rgba(0,229,255,0.25)',
                        shadowBlur: 8
                    },
                    label: { show: false },
                    data: inc.map((i, idx) => ({
                        name: i.categoryName,
                        value: Number(i.categoryIncome) || 0,
                        itemStyle: { color: BAR_GRAD[idx % BAR_GRAD.length][0] }
                    })),
                    // 中心发光文字：总收入
                    graphic: [{
                        type: 'text',
                        left: '38%',
                        top: '42%',
                        style: {
                            text: '总收入',
                            fill: C.textDim,
                            fontSize: 11,
                            textAlign: 'center'
                        }
                    }, {
                        type: 'text',
                        left: '38%',
                        top: '50%',
                        style: {
                            text: '￥' + Number(total).toLocaleString(),
                            fill: '#00e5ff',
                            fontSize: 16,
                            fontWeight: 700,
                            textAlign: 'center',
                            textShadowColor: 'rgba(0,229,255,0.6)',
                            textShadowBlur: 8
                        }
                    }]
                }]
            })
        },
        setGoodsRose(data) {
            const total = data.reduce((a, d) => a + d.value, 0)
            this.charts.goods.setOption({
                backgroundColor: 'transparent',
                tooltip: Object.assign(this.tip(), {
                    trigger: 'item',
                    formatter: (p) => `${p.name}：${p.value} 件（${p.percent}%）`
                }),
                legend: {
                    bottom: 0,
                    itemWidth: 10,
                    itemHeight: 10,
                    icon: 'circle',
                    textStyle: { color: C.text, fontSize: 11 }
                },
                series: [{
                    type: 'pie',
                    roseType: 'radius',
                    radius: ['16%', '66%'],
                    center: ['50%', '44%'],
                    itemStyle: {
                        borderRadius: 5,
                        borderColor: 'rgba(10,20,45,0.9)',
                        borderWidth: 2,
                        shadowColor: 'rgba(0,229,255,0.18)',
                        shadowBlur: 8
                    },
                    label: {
                        color: C.text,
                        fontSize: 10,
                        formatter: '{b}\n{d}%'
                    },
                    labelLine: { lineStyle: { color: 'rgba(0,229,255,0.35)' } },
                    data: total === 0
                        ? [{ name: '暂无数据', value: 1, itemStyle: { color: '#1c3050' } }]
                        : data.map((d, idx) => ({
                            ...d,
                            itemStyle: { color: BAR_GRAD[idx % BAR_GRAD.length][0] }
                        }))
                }]
            })
        },
        setOrdersRing(data) {
            const total = data.reduce((a, d) => a + d.value, 0)
            this.charts.orders.setOption({
                backgroundColor: 'transparent',
                tooltip: Object.assign(this.tip(), {
                    trigger: 'item',
                    formatter: (p) => `${p.name}：${p.value} 单（${p.percent}%）`
                }),
                legend: {
                    orient: 'vertical',
                    right: 8,
                    top: 'center',
                    itemWidth: 10,
                    itemHeight: 10,
                    icon: 'circle',
                    textStyle: { color: C.text, fontSize: 11 }
                },
                series: [{
                    type: 'pie',
                    radius: ['42%', '66%'],
                    center: ['38%', '50%'],
                    itemStyle: {
                        borderColor: 'rgba(10,20,45,0.9)',
                        borderWidth: 2,
                        shadowColor: 'rgba(124,108,255,0.3)',
                        shadowBlur: 8
                    },
                    label: { show: false },
                    data: total === 0
                        ? [{ name: '暂无数据', value: 1, itemStyle: { color: '#1c3050' } }]
                        : data.map((d, idx) => ({
                            ...d,
                            itemStyle: { color: ['#00e5ff', '#4facfe', '#7c6cff', '#00d4a0', '#ff8c5a'][idx % 5] }
                        })),
                    graphic: [{
                        type: 'text',
                        left: '38%',
                        top: '42%',
                        style: {
                            text: '总订单',
                            fill: C.textDim,
                            fontSize: 11,
                            textAlign: 'center'
                        }
                    }, {
                        type: 'text',
                        left: '38%',
                        top: '50%',
                        style: {
                            text: total.toLocaleString(),
                            fill: '#7c6cff',
                            fontSize: 16,
                            fontWeight: 700,
                            textAlign: 'center',
                            textShadowColor: 'rgba(124,108,255,0.6)',
                            textShadowBlur: 8
                        }
                    }]
                }]
            })
        },
        setUsersPie(data) {
            const total = data.reduce((a, d) => a + d.value, 0)
            this.charts.users.setOption({
                backgroundColor: 'transparent',
                tooltip: Object.assign(this.tip(), {
                    trigger: 'item',
                    formatter: (p) => `${p.name}用户：${p.value} 人（${p.percent}%）`
                }),
                legend: {
                    bottom: 0,
                    itemWidth: 10,
                    itemHeight: 10,
                    icon: 'circle',
                    textStyle: { color: C.text, fontSize: 11 }
                },
                series: [{
                    type: 'pie',
                    radius: ['0%', '62%'],
                    center: ['50%', '44%'],
                    itemStyle: {
                        borderRadius: 6,
                        borderColor: 'rgba(10,20,45,0.9)',
                        borderWidth: 2,
                        shadowColor: 'rgba(0,212,160,0.25)',
                        shadowBlur: 8
                    },
                    label: {
                        color: C.text,
                        fontSize: 11,
                        formatter: '{b}：{d}%'
                    },
                    data: total === 0
                        ? [{ name: '暂无数据', value: 1, itemStyle: { color: '#1c3050' } }]
                        : [
                            { name: '正常', value: data[0].value, itemStyle: { color: '#00d4a0' } },
                            { name: '封禁', value: data[1].value, itemStyle: { color: '#ff6b6b' } }
                        ]
                }]
            })
        }
    }
}
</script>

<style scoped>
/* ==================== 大屏根容器 ==================== */
.db-root {
    position: relative;
    min-height: 640px;
    padding: 18px 20px 24px;
    border-radius: 14px;
    background: linear-gradient(135deg, #0a1128 0%, #0e1a38 55%, #0a142e 100%);
    border: 1px solid rgba(0, 229, 255, 0.18);
    box-shadow: 0 0 30px rgba(0, 229, 255, 0.06), 0 12px 36px rgba(0, 0, 0, 0.35);
    overflow: hidden;
    color: #d8f2ff;
    font-family: 'PingFang SC', 'Segoe UI', Arial, sans-serif;
}

/* 网格底纹 */
.db-grid {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background:
        linear-gradient(rgba(0, 229, 255, 0.045) 1px, transparent 1px),
        linear-gradient(90deg, rgba(0, 229, 255, 0.045) 1px, transparent 1px);
    background-size: 42px 42px;
    mask-image: radial-gradient(ellipse at 50% 0%, rgba(0, 0, 0, 0.9), transparent 75%);
}

/* ==================== 顶部标题栏 ==================== */
.db-head {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 14px;
    margin-bottom: 16px;
    border-bottom: 1px solid rgba(0, 229, 255, 0.14);
}
.db-title-cn {
    font-size: 20px;
    font-weight: 700;
    letter-spacing: 3px;
    background: linear-gradient(90deg, #7ff7ff, #4facfe 60%, #00e5ff);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    text-shadow: 0 0 22px rgba(0, 229, 255, 0.35);
}
.db-title-en {
    margin-left: 10px;
    font-size: 11px;
    letter-spacing: 4px;
    color: #3d6a9e;
}
.db-head-right {
    display: flex;
    align-items: center;
    gap: 14px;
}
.db-time {
    font-size: 13px;
    letter-spacing: 1px;
    color: #7fb8e8;
    font-variant-numeric: tabular-nums;
}
.db-refresh {
    border: 1px solid rgba(0, 229, 255, 0.4);
    background: rgba(0, 229, 255, 0.08);
    color: #7ff7ff;
    border-radius: 999px;
}
.db-refresh:hover {
    background: rgba(0, 229, 255, 0.2);
    color: #ffffff;
}

/* ==================== KPI 指标卡 ==================== */
.db-kpis {
    position: relative;
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    gap: 12px;
    margin-bottom: 14px;
}
.db-kpi {
    position: relative;
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 14px 14px 12px;
    border-radius: 12px;
    background: rgba(14, 30, 62, 0.72);
    border: 1px solid rgba(0, 229, 255, 0.16);
    overflow: hidden;
    transition: border-color 0.25s ease, box-shadow 0.25s ease, transform 0.25s ease;
}
.db-kpi::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: linear-gradient(90deg, transparent, #00e5ff, transparent);
    opacity: 0.6;
}
.db-kpi:hover {
    border-color: rgba(0, 229, 255, 0.55);
    box-shadow: 0 0 18px rgba(0, 229, 255, 0.22), inset 0 0 20px rgba(0, 229, 255, 0.05);
    transform: translateY(-2px);
}
.db-kpi-ico {
    font-size: 24px;
    filter: drop-shadow(0 0 8px rgba(0, 229, 255, 0.5));
}
.db-kpi-num {
    font-size: 24px;
    font-weight: 800;
    line-height: 1.15;
    font-variant-numeric: tabular-nums;
    background: linear-gradient(180deg, #9ef4ff, #4facfe);
    -webkit-background-clip: text;
    background-clip: text;
    color: transparent;
    text-shadow: 0 0 16px rgba(0, 229, 255, 0.25);
}
.db-kpi-unit {
    font-size: 11px;
    font-weight: 400;
    margin-left: 3px;
    color: #5f8fc4;
    background: none;
    -webkit-text-fill-color: #5f8fc4;
}
.db-kpi-label {
    font-size: 12px;
    color: #8fb4dd;
    letter-spacing: 1px;
    margin-top: 1px;
}

/* ==================== 图表网格 ==================== */
.db-charts {
    position: relative;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 14px;
}
.db-chart {
    position: relative;
    background: rgba(13, 28, 58, 0.6);
    border: 1px solid rgba(0, 229, 255, 0.14);
    border-radius: 12px;
    padding: 12px 12px 8px;
    overflow: hidden;
    transition: border-color 0.25s ease, box-shadow 0.25s ease;
}
.db-chart:hover {
    border-color: rgba(0, 229, 255, 0.45);
    box-shadow: 0 0 20px rgba(0, 229, 255, 0.12), inset 0 0 24px rgba(0, 229, 255, 0.04);
}
.db-chart-head {
    display: flex;
    align-items: center;
    gap: 7px;
    font-size: 13px;
    font-weight: 600;
    color: #a9d4ff;
    letter-spacing: 1px;
    padding-bottom: 8px;
    border-bottom: 1px dashed rgba(0, 229, 255, 0.12);
}
.db-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #00e5ff;
    box-shadow: 0 0 10px #00e5ff;
    animation: db-pulse 2s ease-in-out infinite;
}
@keyframes db-pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.35; }
}
.db-tag {
    margin-left: auto;
    font-size: 9px;
    letter-spacing: 2px;
    color: #3d7ab0;
    border: 1px solid rgba(0, 229, 255, 0.25);
    border-radius: 999px;
    padding: 1px 8px;
}
.db-chart-body {
    height: 300px;
    width: 100%;
}

/* ==================== 公告面板 ==================== */
.db-notice-body {
    overflow-y: auto;
    padding: 6px 2px;
}
.db-notice-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
}
.db-notice-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    padding: 9px 10px;
    border-radius: 8px;
    background: rgba(0, 229, 255, 0.05);
    border: 1px solid rgba(0, 229, 255, 0.1);
    font-size: 13px;
    line-height: 1.55;
    transition: background 0.2s ease, border-color 0.2s ease;
}
.db-notice-item:hover {
    background: rgba(0, 229, 255, 0.1);
    border-color: rgba(0, 229, 255, 0.4);
}
.db-notice-ico {
    flex-shrink: 0;
}
.db-notice-text {
    flex: 1;
    min-width: 0;
    color: #cfe6ff;
    word-break: break-word;
}
.db-notice-time {
    flex-shrink: 0;
    font-size: 11px;
    color: #51749e;
    font-variant-numeric: tabular-nums;
}
.db-empty {
    color: #45658c;
    font-size: 13px;
    text-align: center;
    padding: 70px 0;
}

/* ==================== 响应式 ==================== */
@media (max-width: 1400px) {
    .db-kpis { grid-template-columns: repeat(3, 1fr); }
    .db-charts { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 900px) {
    .db-kpis { grid-template-columns: repeat(2, 1fr); }
    .db-charts { grid-template-columns: 1fr; }
}
</style>
