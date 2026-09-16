<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="order-page-container">
                <div class="idle-info-container" @click="toDetails(orderInfo.idleItem.id)">
                    <el-image
                            style="width: 150px; height: 150px;"
                            :src="imageUrl(orderInfo.idleItem.imgUrl)"
                            fit="cover"></el-image>
                    <div class="idle-info-title">{{orderInfo.userId==userId?'商品':'商品'}}：{{orderInfo.idleItem.idleName}}</div>
                    <div class="idle-info-price">￥{{orderInfo.orderPrice}}</div>
                    <div class="order-info-time">支付倒计时：{{timeLeft}}</div>

                </div>
                <div class="address-container" @click.stop="selectAddressDialog" :style="orderInfo.userId==userId&&orderInfo.orderStatus===0?'cursor: pointer;':''">
                    <div class="address-title">收货地址: {{addressInfo.consigneeName}} {{addressInfo.consigneePhone}}</div>
                    <div class="address-detials">{{addressInfo.detailAddress}}</div>
                    <el-button v-if="!addressInfo.detailAddress" @click.stop="selectAddressDialog" type="primary" plain>选择收货地址</el-button>
                </div>
                <el-dialog
                        title="选择地址"
                        v-model="addressDialogVisible"
                        width="800px">
                    <el-table
                            stripe
                            empty-text="没有地址信息，请到个人中心填写地址"
                            :data="addressData"
                            style="width: 100%">
                        <el-table-column
                                prop="consigneeName"
                                label="收货人姓名"
                                width="120">
                        </el-table-column>
                        <el-table-column
                                prop="consigneePhone"
                                label="手机号"
                                width="140">
                        </el-table-column>
                        <el-table-column
                                prop="detailAddressText"
                                label="地址">
                        </el-table-column>
                        <el-table-column label=" " width="120">
                            <template #default="scope">
                                <el-button
                                        size="small"
                                        @click="selectAddress(scope.$index, scope.row)">选择
                                </el-button>
                            </template>
                        </el-table-column>
                    </el-table>
                </el-dialog>
                <div class="order-info-container">
                    <div class="order-info-title">订单信息（{{orderStatus[orderInfo.orderStatus]}}）：</div>
                    <div class="order-info-item">订单编号：{{orderInfo.orderNumber}}</div>
                    <div class="order-info-item">支付状态：{{orderInfo.paymentStatus===0?'未支付':'已支付'}}</div>
                    <div class="order-info-item">订单创建时间：{{orderInfo.createTime.substring(0, 10) + ' ' +
                        orderInfo.createTime.substring(11, 19)}}
                    </div>
                    <div class="order-info-item">支付时间：{{orderInfo.paymentTime?orderInfo.paymentTime.substring(0, 10) + ' ' +
                        orderInfo.paymentTime.substring(11, 19):''}}
                    </div>
                </div>
                <div class="menu">
                    <el-button v-if="orderInfo.id" type="info" plain @click="openChat">💬 私信对方</el-button>
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===0" type="danger" plain @click="changeOrderStatus(4,orderInfo)"> 放弃支付</el-button>
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===0" type="primary" plain @click="changeOrderStatus(1,orderInfo)"> 立即支付</el-button>
                    <el-button v-if="userId==orderInfo.idleItem.userId&&orderInfo.orderStatus===1" type="primary" plain @click="changeOrderStatus(2,orderInfo)"> 发货</el-button>
                    <el-button v-if="userId==orderInfo.userId&&orderInfo.orderStatus===2" type="primary" plain @click="changeOrderStatus(3,orderInfo)"> 确认收货</el-button>
                </div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/*
 * 迁移要点（components/page/order.vue → views/order.vue）：
 * - 当前用户 id 改从 Pinia userStore 取（原版读明文 Cookie shUserId，
 *   改 Cookie 即可冒充他人操作订单，已删除 getCookie）
 * - 支付倒计时统一 30 分钟（原版首算 30 分钟、interval 里却按 10 分钟，不一致），
 *   interval 存句柄并在 beforeUnmount / 支付完成时 clearInterval（原版泄漏）
 * - 图片地址 $store.state.baseApi → imageUrl()
 * - Element Plus：:visible.sync → v-model、slot-scope → #default、mini → small
 * - $api → api；toDetails 的相对路径 'details' 改 '/details'（vue-router 4 下
 *   相对路径解析依据变化，避免跳到 /order/details）
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import { imageUrl } from '@/utils/request'

export default {
    name: "order",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            timeLeft: '',
            addressDialogVisible: false,
            addressData: [],
            orderStatus: ['待付款', '待发货', '待收货', '已完成', '已取消'],
            orderInfo: {
                createTime: "",
                id: 0,
                idleId: 0,
                idleItem: {
                    id: '',
                    idleName: '',
                    idleDetails: '',
                    pictureList: [],
                    idlePrice: 0,
                    idlePlace: '',
                    idleLabel: '',
                    idleStatus: -1,
                    userId: '',
                },
                orderNumber: "",
                orderPrice: 0,
                orderStatus: 0,
                paymentStatus: 0,
                paymentTime: "",
                paymentWay: "",
                userId: 0
            },
            addressInfo: {
                id: '',
                update: false,
                consigneeName: '',
                consigneePhone: '',
                detailAddress: ''
            },
            userId: '',
            countdownTimer: null
        };
    },
    created() {
        this.userStore = useUserStore();
        this.userId = this.userStore.userInfo.id;
        let orderId = this.$route.query.id;
        api.getOrder({
            id: orderId
        }).then(res => {
            if (res.status_code === 1) {
                if (res.data.idleItem) {
                    let imgList = JSON.parse(res.data.idleItem.pictureList);
                    if (imgList.length > 0) {
                        res.data.idleItem.imgUrl = imgList[0];
                    } else {
                        res.data.idleItem.imgUrl = '';
                    }
                } else {
                    res.data.idleItem = {
                        idleName: '',
                        imgUrl: ''
                    }
                }
                this.orderInfo = res.data;

                api.getOrderAddress({
                    orderId: this.orderInfo.id
                }).then(res => {
                    if (res.data) {
                        this.addressInfo = res.data;
                        this.addressInfo.update = true;
                    } else {
                        this.getAddressData();
                    }
                    this.calculateTimeLeft();
                })
            }
        })
    },
    beforeUnmount() {
        clearInterval(this.countdownTimer);
    },
    methods: {
        imageUrl,
        calculateTimeLeft() {
            clearInterval(this.countdownTimer);
            if (this.orderInfo.orderStatus === 0) {
                const createTime = new Date(this.orderInfo.createTime);
                if (isNaN(createTime.getTime())) {
                    return;
                }
                this.updateTimeLeft();
                this.countdownTimer = setInterval(this.updateTimeLeft, 1000);
            } else {
                this.timeLeft = '';
            }
        },
        updateTimeLeft() {
            // 订单超时时间统一 30 分钟
            const createTime = new Date(this.orderInfo.createTime).getTime();
            const diff = 30 * 60 * 1000 - (Date.now() - createTime);
            if (diff <= 0) {
                clearInterval(this.countdownTimer);
                this.timeLeft = '已超时';
                return;
            }
            const minutes = Math.floor(diff / (1000 * 60));
            const seconds = Math.floor((diff / 1000) % 60);
            this.timeLeft = `${minutes} 分, ${seconds} 秒`;
        },
        toDetails(id) {
            this.$router.replace({ path: '/details', query: { id: id } });
        },
        // 打开订单买卖双方私信窗口（下单后可联系对方沟通发货/收货等）
        openChat() {
            if (this.orderInfo.id) {
                this.$router.push({ path: '/chat', query: { orderId: this.orderInfo.id } });
            }
        },
        selectAddressDialog() {
            if (this.orderInfo.userId == this.userId && this.orderInfo.orderStatus === 0) {
                this.addressDialogVisible = true;
                if (this.addressData.length === 0) {
                    this.getAddressData();
                }
            }
        },
        getAddressData() {
            api.getAddress().then(res => {
                if (res.status_code === 1) {
                    let data = res.data;
                    for (let i = 0; i < data.length; i++) {
                        data[i].detailAddressText = data[i].provinceName + data[i].cityName + data[i].regionName + data[i].detailAddress;
                    }
                    this.addressData = data;
                    if (!this.addressInfo.update) {
                        for (let i = 0; i < data.length; i++) {
                            if (data[i].defaultFlag) {
                                this.selectAddress(i, data[i]);
                            }
                        }
                    }
                }
            })
        },
        selectAddress(i, item) {
            this.addressDialogVisible = false;
            this.addressInfo.consigneeName = item.consigneeName;
            this.addressInfo.consigneePhone = item.consigneePhone;
            this.addressInfo.detailAddress = item.detailAddressText;
            if (this.addressInfo.update) {
                api.updateOrderAddress({
                    id: this.addressInfo.id,
                    consigneeName: item.consigneeName,
                    consigneePhone: item.consigneePhone,
                    detailAddress: item.detailAddressText
                })
            } else {
                api.addOrderAddress({
                    orderId: this.orderInfo.id,
                    consigneeName: item.consigneeName,
                    consigneePhone: item.consigneePhone,
                    detailAddress: item.detailAddressText
                }).then(res => {
                    if (res.status_code === 1) {
                        this.addressInfo.update = true;
                        this.addressInfo.id = res.data.id;
                    } else {
                        this.$message.error(res.msg)
                    }
                })
            }

        },
        changeOrderStatus(orderStatus, orderInfo) {
            if (orderStatus === 1) {
                if (!this.addressInfo.detailAddress) {
                    this.$message.error('请选择地址！');
                } else {
                    this.$confirm('模拟支付，是否确认支付', '支付订单', {
                        confirmButtonText: '支付',
                        cancelButtonText: '放弃',
                        type: 'warning'
                    }).then(() => {
                        api.updateOrder({
                            id: orderInfo.id,
                            orderStatus: orderStatus,
                            paymentStatus: 1,
                            paymentWay: '支付宝',
                        }).then(res => {
                            if (res.status_code === 1) {
                                this.$message({
                                    message: '支付成功！',
                                    type: 'success'
                                });
                                this.orderInfo.orderStatus = orderStatus;
                                this.orderInfo.paymentStatus = 1;
                                this.orderInfo.paymentWay = '支付宝';
                                this.orderInfo.paymentTime = res.data.paymentTime;
                                this.timeLeft = '';
                                clearInterval(this.countdownTimer);
                                this.$router.push({ path: '/me' });
                            }
                        });
                    }).catch(() => {
                    });
                }
            } else {
                api.updateOrder({
                    id: orderInfo.id,
                    orderStatus: orderStatus,
                }).then(res => {
                    if (res.status_code === 1) {
                        this.$message({
                            message: '操作成功！',
                            type: 'success'
                        });
                        this.orderInfo.orderStatus = orderStatus;

                        this.timeLeft = '';
                        clearInterval(this.countdownTimer);
                        this.$router.push({ path: '/' });
                    }
                });
            }
        },
    }

}
</script>

<style scoped>
.order-page-container {
    min-height: 85vh;
}

.idle-info-container {
    width: 96%;
    display: flex;
    border-bottom: 20px solid #f6f6f6;
    padding: 20px;
    cursor: pointer;
}

.idle-info-title {
    margin-top: 50px;
    text-align: center;
    font-size: 24px;
    font-weight: 600;
    max-width: 750px;
    margin-left: 10px;
}

.idle-info-price {
    margin-top: 50px;
    text-align: center;
    font-size: 30px;
    font-weight: 600;
    color: red;
    margin-left: 100px;
}

.address-container {
    min-height: 60px;
    padding: 20px;
    border-bottom: 20px solid #f6f6f6;

}

.address-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 10px;
}

.address-detials {
    font-size: 16px;
    color: #444444;
}

.order-info-container {
    padding: 20px;
}

.order-info-item {
    margin: 10px 0;
    font-size: 14px;
    color: #444444;
}

.menu {
    margin-left: 20px;
}

.order-info-time {
    font-size: 25px;
    color: red;
    font-weight: bolder;
    margin-left: 170px;
}
</style>
