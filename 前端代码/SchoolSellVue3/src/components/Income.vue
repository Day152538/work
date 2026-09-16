<template>
    <div>
        <el-tabs v-model="activeName" @tab-click="handleClick" class="charts">
            <el-card style="
            display: inline-block;
            margin-left: 700px;
            margin-top: 30px;
            font-size: 22px;
            color: red;
          ">总销售：{{ numFilter(total) }}￥</el-card>
            <!--      柱状图-->
            <el-tab-pane label="各类收入柱状图" name="bar">
                <div ref="bar" style="width: 1200px; height: 500px; margin: auto auto"></div>
            </el-tab-pane>
            <!--      饼图-->
            <el-tab-pane label="各类收入饼图" name="pie">
                <div ref="pie" style="width: 1200px; height: 470px; margin: 10px -100px"></div>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - 原代码绕过 API 层用 axios 直连 http://localhost:9321/api/income/chart（无 admin_token，
 *   且硬编码地址）→ 改为 api.getIncomeChart()（adminRequest，携带 admin_token，
 *   后端 /api/income/chart 已要求管理员）
 * - 响应结构改为统一 ResultVo：res.status_code === 1，数据在 res.data.categoryIncomes
 * - Vue3 移除 filters：{{ total | numFilter }} → {{ numFilter(total) }}
 * - echarts 实例与 window resize 监听在 beforeUnmount 中 dispose/removeEventListener 清理，
 *   修复原版切换 tab 后 chart 实例与监听泄漏的问题
 */
import * as echarts from "echarts";
import api from '@/api'

export default {
    name: "IncomeChart",
    data() {
        return {
            categoryIncomes: [],
            categoryNames: [],
            incomes: [],
            activeName: "bar",
            totalAll: 0,
            total: 0,
        };
    },
    methods: {
        handleClick() {
            this.total = this.totalAll;
            // 两个图表容器在 mounted 时就 init，但被隐藏的 tab-pane 初始尺寸为 0×0，
            // 切到该面板后 echarts 不会自动感知容器尺寸变化，画布一直空白。
            // Element Plus 的 tab-click 先于 v-model/面板显示触发，故需等渲染完成后统一 resize。
            this.$nextTick(() => this.$nextTick(() => {
                this.barChart && this.barChart.resize();
                this.pieChart && this.pieChart.resize();
            }));
        },
        // 原 filters.numFilter：截取到小数点后两位
        numFilter(value) {
            let realVal = Number(value).toFixed(2);
            return Number(realVal);
        },
        handleResize() {
            this.barChart && this.barChart.resize();
            this.pieChart && this.pieChart.resize();
        },
    },

    mounted() {
        this.barChart = echarts.init(this.$refs.bar);
        this.pieChart = echarts.init(this.$refs.pie);
        window.addEventListener('resize', this.handleResize);

        const barOption = {
            tooltip: {
                trigger: "item",
            },
            title: {
                text: "收入统计柱状图",
                x: "left",
            },
            label: {
                show: true, //是否显示
                position: "top",
            },
            xAxis: {
                type: "category",
                data: [],
            },
            yAxis: {
                type: "value",
            },
            series: [
                {
                    data: [],
                    type: "bar",
                    itemStyle: {
                        // 柱状图的颜色
                        color: 'blue'
                    }
                },
            ],
        };
        const pieOption = {
            tooltip: {
                trigger: "item",
            },

            title: {
                text: "收入统计饼图",
                x: "center",
            },
            series: [
                {
                    type: "pie",
                    data: [],
                },
            ],
        };
        // 渲染柱状图和饼图（管理端接口，带 admin_token）
        api.getIncomeChart()
            .then((res) => {
                if (res.status_code === 1) {
                    let categoryIncomes = res.data.categoryIncomes;
                    let categoryNames = categoryIncomes.map((item) => {
                        return item.categoryName;
                    });
                    let incomes = categoryIncomes.map((item) => {
                        return item.categoryIncome;
                    });
                    barOption.xAxis.data = categoryNames;
                    barOption.series[0].data = incomes;
                    this.barChart.setOption(barOption);

                    for (let i = 0; i < categoryNames.length; i++) {
                        let item = { value: incomes[i], name: categoryNames[i] };
                        pieOption.series[0].data.push(item);
                    }
                    this.pieChart.setOption(pieOption);

                    // 计算总和
                    let sum = incomes.reduce((acc, curr) => acc + curr, 0);
                    this.total = sum;
                    this.totalAll = sum;
                }
            })
            .catch(() => {
                // 网络错误/401 已由 adminRequest 拦截器统一提示
            });
    },
    beforeUnmount() {
        window.removeEventListener('resize', this.handleResize);
        if (this.barChart) {
            this.barChart.dispose();
            this.barChart = null;
        }
        if (this.pieChart) {
            this.pieChart.dispose();
            this.pieChart = null;
        }
    },
};
</script>
<style scoped>

</style>
