<template>
    <div>
        <app-head :nickname-value="userStore.userInfo.nickname" :avatarValue="userStore.userInfo.avatar"></app-head>
        <app-body>
            <div v-show="!eidtAddress">
                <div class="user-info-container">
                    <div class="user-info-details">

                        <el-upload action="#" :http-request="uploadAvatar" :file-list="imgFileList"
                            :show-file-list="false" accept="image/*">
                            <el-image style="width: 120px; height: 120px; border-radius: 10px; object-fit: cover;"
                                :src="avatarUrl" fit="cover"></el-image>
                        </el-upload>
                        <div class="user-info-details-text">
                            <div class="container">
                                <div class="user-info-details-text-nickname"
                                    :style="{ color: userStore.isVip ? 'transparent' : 'black', background: userStore.isVip ? 'linear-gradient(to right, red, blue)' : 'none', '-webkit-background-clip': userStore.isVip ? 'text' : 'initial', animation: userStore.isVip ? 'colorChange 5s infinite' : 'none' }">
                                    {{ userStore.userInfo.nickname }}</div>
                                <div
                                    :class="{ 'user-info-details-text-vip': userStore.isVip, 'user-info-details-text-not-vip': !userStore.isVip }">
                                    VIP</div>
                                <div id="user-info-details-text-vip-expire" v-if="userStore.isVip"
                                    style="text-align: center; margin-left: 10px; margin-top: 20px;">
                                    还有: {{ countdownTime }} 到期
                                </div>
                            </div>
                            <div class="user-info-details-text-time">{{ userStore.userInfo.signInTime }} 加入平台</div>
                            <el-dialog @close="finishEdit" title="个人信息" v-model="userInfoDialogVisible"
                                width="400px">
                                <div class="edit-tip"> 昵称</div>
                                <el-input v-model="userStore.userInfo.nickname" :disabled="notUserNicknameEdit"
                                    @change="saveUserNickname">
                                    <template #append>
                                        <el-button type="warning" @click="notUserNicknameEdit = false">
                                            <Edit /> 修改昵称
                                        </el-button>
                                    </template>
                                </el-input>
                                <div class="edit-tip"> 邮箱</div>
                                <el-input v-model="userStore.userInfo.userEmail" :disabled="notUserEmailEdit" title="请输入有效的邮箱地址"
                                    @change="saveUserEmailEdit">
                                    <template #append>
                                        <el-button type="warning" @click="notUserEmailEdit = false">
                                            <Edit /> 添加邮箱
                                        </el-button>
                                    </template>
                                </el-input>

                                <div v-if="userPasswordEdit">
                                    <div class="edit-tip">原密码</div>
                                    <el-input v-model="userPassword1" show-password></el-input>
                                    <div class="edit-tip">新密码</div>
                                    <el-input v-model="userPassword2" show-password></el-input>
                                    <div class="edit-tip">确认新密码</div>
                                    <el-input v-model="userPassword3" show-password></el-input>
                                    <div class="edit-tip"></div>
                                    <el-button @click="savePassword" type="primary" plain> 确认修改密码</el-button>
                                </div>
                                <div v-else>
                                    <div class="edit-tip">密码</div>
                                    <el-input model-value="123456" :disabled="true" show-password>
                                        <template #append>
                                            <el-button type="warning" @click="userPasswordEdit = true">
                                                <Edit /> 修改密码
                                            </el-button>
                                        </template>
                                    </el-input>
                                </div>
                                <template #footer>
                                    <span class="dialog-footer">
                                        <div class="button1" @click="submit" type="primary"> 确认</div>
                                        <div class="button2" @click="userInfoDialogVisible = false"> 取消</div>
                                    </span>
                                </template>
                            </el-dialog>
                            <el-dialog title="个人信息" v-model="vip" width="400px">
                                <img src="@/assets/background.png" style="width: 100%; height: 200px;" />
                                <div class="edit-tip1"> 会员每月100元，可享受8折优惠</div>
                                <template #footer>
                                    <span class="dialog-footer">
                                        <div class="button1" @click="vipUser" type="primary">确认支付</div>
                                        <div class="button2" @click="vip = false">取消</div>
                                    </span>
                                </template>
                            </el-dialog>
                        </div>
                        <div class="user-info-details-text-edit" style="margin-left: 30px;">
                            <el-button type="primary" plain @click="userInfoDialogVisible = true">
                                个人信息</el-button>
                            <el-button type="success" plain @click="eidtAddress = true"> 收货地址</el-button>
                            <el-button type="success" plain @click="vip = true"> 成为会员</el-button>
                        </div>
                    </div>
                </div>
                <div class="center-title">商品中心</div>
                <div class="idle-container">
                    <el-tabs v-model="activeName" @tab-click="handleClick">
                        <el-tab-pane label="我的商品" name="1"></el-tab-pane>
                        <el-tab-pane label="下架商品" name="2"></el-tab-pane>
                        <el-tab-pane label="我的收藏" name="3"></el-tab-pane>
                        <el-tab-pane label="卖出商品" name="4"></el-tab-pane>
                        <el-tab-pane label="购物商品" name="5"></el-tab-pane>
                        <el-tab-pane label="违规商品" name="6"></el-tab-pane>
                        <el-tab-pane label="待审核商品" name="7"></el-tab-pane>
                    </el-tabs>
                    <div class="idle-container-list">
                        <div v-for="(item, index) in dataList[activeName - 1]" :key="index"
                            class="idle-container-list-item">
                            <div class="idle-container-list-item-detile" @click="toDetails(activeName, item)">
                                <el-image style="width: 100px; height: 100px;margin-left: 20px;" :src="imageUrl(item.imgUrl)"
                                    fit="cover">
                                    <template #error>
                                        <div class="image-slot">
                                            <Picture /> 暂无商品
                                        </div>
                                    </template>
                                </el-image>
                                <div class="idle-container-list-item-text">
                                    <div class="idle-container-list-zu">
                                        <div class="idle-container-list-title">
                                            {{ item.idleName }}
                                        </div>
                                        <div class="idle-container-list-idle-time">{{ item.timeStr }}</div>
                                    </div>
                                    <div class="idle-container-list-idle-details">
                                        {{ item.idleDetails }}
                                    </div>
                                    <div class="idle-reason-text"
                                        v-if="(activeName === '6' || activeName === '7') && item.idleReason">
                                        处理说明：{{ item.idleReason }}
                                    </div>

                                    <div class="idle-item-foot">
                                        <div class="idle-price">￥{{ item.idlePrice }}
                                            {{ (activeName === '4' || activeName === '5') ?
                                            orderStatus[item.orderStatus] : '' }}
                                        </div>
                                        <div class="button3"
                                            v-if="activeName !== '4' && activeName !== '5'&& activeName !== '6'&& activeName !== '7'"
                                            type="danger" plain
                                            @click.stop="handle(activeName, item, index)">{{
                                            handleName[activeName - 1] }}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div v-show="eidtAddress" class="address-container">
                <el-page-header class="address-container-back" @back="eidtAddress = false"
                    content="收货地址"></el-page-header>
                <div class="address-container-add">
                    <div class="address-container-add-title"><b> 新添地址：</b></div>

                    <div class="address-container-add-item">
                        <el-input placeholder="请输入收货人姓名" v-model="addressInfo.consigneeName" maxlength="10"
                            show-word-limit>
                            <template #prepend>收货人姓名</template>
                        </el-input>
                    </div>
                    <div class="address-container-add-item">
                        <el-input placeholder="请输入收货人手机号" v-model="addressInfo.consigneePhone"
                            @input="addressInfo.consigneePhone = addressInfo.consigneePhone.replace(/[^\d.]/g, '')" maxlength="11" show-word-limit>
                            <template #prepend>手机号</template>
                        </el-input>
                    </div>
                    <div class="address-container-add-item">
                        <span class="demonstration">学校/校区/楼号</span>
                        <el-cascader :options="options" v-model="selectedOptions" @change="handleAddressChange"
                            :separator="' '">
                        </el-cascader>
                    </div>
                    <div class="address-container-add-item">
                        <el-input placeholder="请输入详细地址（校区、楼号、层数、宿舍号）" v-model="addressInfo.detailAddress" maxlength="50"
                            show-word-limit>
                            <template #prepend>详细地址</template>
                        </el-input>
                    </div>
                    <el-checkbox v-model="addressInfo.defaultFlag" class="custom-checkbox">设置为默认地址</el-checkbox>
                    <div class="button4" style="margin-left: 10px;" @click="saveAddress"> 保存</div>
                </div>
                <div class="address-container-list">
                    <hr>
                    <div style="color: black;padding-left: 10px;font-size: 20px;"><b>已存在的收货地址：</b></div>
                    <br>
                    <el-table stripe :data="addressData" style="width: 100%">
                        <el-table-column prop="consigneeName" label="收货人姓名" width="100">
                        </el-table-column>
                        <el-table-column prop="consigneePhone" label="手机号" width="120">
                        </el-table-column>
                        <el-table-column prop="detailAddressText" label="地址" width="270">
                        </el-table-column>
                        <el-table-column label="是否默认地址" width="110">
                            <template #default="scope">
                                <el-button v-if="!scope.row.defaultFlag" size="small"
                                    @click="handleSetDefault(scope.$index, scope.row)">设为默认
                                </el-button>
                                <div v-else style="padding-left: 10px;color: #409EFF;">{{ scope.row.defaultAddress }}
                                </div>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="250" fixed="right">
                            <template #default="scope">
                                <div class="button1" type="primary" size="small"
                                    @click="handleEdit(scope.$index, scope.row)"> 编辑
                                </div>
                                <div class="button2" type="danger" @click="handleDelete(scope.$index, scope.row)"> 删除
                                </div>
                            </template>
                        </el-table-column>
                    </el-table>
                </div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/*
 * 迁移要点（components/page/me.vue → views/me.vue）：
 * - VIP 开通：删除前端伪造 userStatus=3 的假支付，改调 api.subscribeVip()，
 *   成功后 userStore.setUser(res.data) 刷新；会员判断/标识统一 userStore.isVip
 * - 到期倒计时改基于 userInfo.vipExpireTime（原版基于 updateTime+30 天，且
 *   setVipTime 里 getElementById('vipCountdown') 引用不存在的节点每分钟抛错，已删）
 * - 头像上传改走 api.uploadFile(file)（带 JWT），后端返回 "/image?imageName=xxx"
 *   提取纯文件名入库；修改密码走 POST api.updatePassword
 * - $globalData/$api → Pinia userStore + api；图片地址统一 imageUrl()
 * - 订单超时判断 10 分钟 → 30 分钟（与后端 Constants 一致）
 * - Element Plus：:visible.sync → v-model、slot/slot-scope → #xxx、
 *   el-icon-xxx → <Edit/>/<Picture/>、size mini → small
 * - 倒计时 setInterval 在 beforeUnmount 清理；删除 console.log 与文件尾死代码
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import options from '@/components/country-data.js'
import api from '@/api'
import { useUserStore } from '@/stores/user'
import { imageUrl } from '@/utils/request'

export default {
    name: "me",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            imgFileList: [],
            addressInfo: {
                consigneeName: '',
                consigneePhone: '',
                provinceName: '',
                cityName: '',
                regionName: '',
                detailAddress: '',
                defaultFlag: false
            },
            rules: {
                consigneeName: [
                    { required: true, message: '请输入收货人姓名', trigger: 'blur' },
                    { min: 5, max: 16, message: '姓名长度在 5 到 16 个字符', trigger: 'blur' }
                ],
                consigneePhone: [
                    { required: true, message: '请输入收货人电话', trigger: 'blur' },
                    { min: 5, max: 16, message: '电话号码长度在 5 到 16 个字符', trigger: 'blur' }
                ],
                detailAddressText: [
                    { required: true, message: '请输入详细地址', trigger: 'blur' },
                    { min: 5, max: 16, message: '详细地址长度在 5 到 16 个字符', trigger: 'blur' }
                ]
            },
            activeName: '1',
            handleName: [' 下架', ' 删除', ' 移除', '', '', ''],
            dataList: [
                [],
                [],
                [],
                [],
                [],
                [],
                [],
                [],
            ],
            orderStatus: ['待付款', '待发货', '待收货', '已完成', '已取消'],
            userInfoDialogVisible: false,
            notUserNicknameEdit: true,
            notUserEmailEdit: true,
            userPasswordEdit: false,
            userPassword1: '',
            userPassword2: '',
            userPassword3: '',
            eidtAddress: false,
            vip: false,
            selectedOptions: [],//存放默认值
            options: options,   //存放城市数据,
            addressData: [],
            now: Date.now(), // 驱动 VIP 到期倒计时每分钟刷新
            vipTimer: null
        };
    },
    created() {
        this.userStore = useUserStore();
        if (!this.userStore.userInfo.nickname) {
            this.userStore.refreshUser(api);
        }
        this.getAddressData();
        this.getIdleItemData();
        this.getMyOrder();
        this.getMySoldIdle();
        this.getMyFavorite();
    },
    mounted() {
        this.vipTimer = setInterval(() => {
            this.now = Date.now();
        }, 60000);
    },
    beforeUnmount() {
        clearInterval(this.vipTimer);
    },
    computed: {
        avatarUrl() {
            const avatar = this.userStore.userInfo.avatar;
            return avatar ? imageUrl(avatar) : '';
        },
        countdownTime() {
            const t = this.userStore.userInfo.vipExpireTime;
            if (!t) {
                return '';
            }
            const diff = new Date(t).getTime() - this.now;
            if (diff <= 0) {
                return '已到期';
            }
            const minutes = Math.ceil(diff / (1000 * 60));
            const days = Math.floor(minutes / (24 * 60));
            const hours = Math.floor((minutes % (24 * 60)) / 60);
            const m = minutes % 60;
            return `${days}天 ${hours}小时 ${m}分钟`;
        }
    },
    methods: {
        imageUrl,
        vipUser() {
            if (this.userStore.isVip) {
                this.$message.error("您已经是会员啦！");
            } else {
                // 真实开通：后端写 vip_expire_time，成功后用返回的最新 user 刷新本地状态
                api.subscribeVip().then(res => {
                    if (res.status_code === 1) {
                        this.userStore.setUser(res.data);
                        this.vip = false;
                        this.$message.success("恭喜你成为尊贵的VIP会员");
                    } else {
                        this.$message.error(res.msg);
                    }
                }).catch(() => { });
            }
        },
        getMyFavorite() {
            api.getMyFavorite().then(res => {
                if (res.status_code === 1) {
                    for (let i = 0; i < res.data.length; i++) {
                        let pictureList = JSON.parse(res.data[i].idleItem.pictureList);
                        this.dataList[2].push({
                            favoriteId: res.data[i].id,
                            id: res.data[i].idleItem.id,
                            imgUrl: pictureList.length > 0 ? pictureList[0] : '',
                            idleName: res.data[i].idleItem.idleName,
                            idleDetails: res.data[i].idleItem.idleDetails,
                            timeStr: res.data[i].createTime.substring(0, 10) + " " + res.data[i].createTime.substring(11, 19),
                            idlePrice: res.data[i].idleItem.idlePrice
                        });
                    }
                }
            })
        },
        getMySoldIdle() {
            api.getMySoldIdle().then(res => {
                if (res.status_code === 1) {
                    for (let i = 0; i < res.data.length; i++) {
                        let pictureList = JSON.parse(res.data[i].idleItem.pictureList);
                        this.dataList[3].push({
                            id: res.data[i].id,
                            imgUrl: pictureList.length > 0 ? pictureList[0] : '',
                            idleName: res.data[i].idleItem.idleName,
                            idleDetails: res.data[i].idleItem.idleDetails,
                            timeStr: res.data[i].createTime.substring(0, 10) + " " + res.data[i].createTime.substring(11, 19),
                            idlePrice: res.data[i].orderPrice,
                            orderStatus: res.data[i].orderStatus
                        });

                        if (res.data[i].orderStatus === 1) {
                            this.$message({
                                message: '买家已付款，请及时发货',
                                type: 'success',
                                customClass: 'my-message-box',
                                center: true,
                                showClose: true
                            });
                        }
                    }
                }
            })
        },
        getMyOrder() {
            api.getMyOrder().then(res => {
                if (res.status_code === 1) {
                    for (let i = 0; i < res.data.length; i++) {
                        let pictureList = JSON.parse(res.data[i].idleItem.pictureList);
                        let createTime = new Date(res.data[i].createTime);
                        let currentTime = new Date();
                        let diffInMinutes = Math.abs(currentTime - createTime) / (1000 * 60); // 计算时间差，单位为分钟
                        let orderStatus = res.data[i].orderStatus;
                        // 订单超时时间统一 30 分钟（后端 Constants.ORDER_TIMEOUT_MINUTES）
                        if (orderStatus === 0 && diffInMinutes > 30) {
                            // 未支付且已超时，修改为已取消
                            orderStatus = 4;
                            api.updateOrder({ id: res.data[i].id, orderStatus: orderStatus });
                        }
                        this.dataList[4].push({
                            id: res.data[i].id,
                            imgUrl: pictureList.length > 0 ? pictureList[0] : '',
                            idleName: res.data[i].idleItem.idleName,
                            idleDetails: res.data[i].idleItem.idleDetails,
                            timeStr: res.data[i].createTime.substring(0, 10) + " " + res.data[i].createTime.substring(11, 19),
                            idlePrice: res.data[i].orderPrice,
                            orderStatus: orderStatus
                        });
                    }
                }
            })
        },
        getIdleItemData() {
            api.getAllIdleItem().then(res => {
                if (res.status_code === 1) {
                    for (let i = 0; i < res.data.length; i++) {
                        res.data[i].timeStr = res.data[i].releaseTime.substring(0, 10) +
                            " " + res.data[i].releaseTime.substring(11, 19);
                        let pictureList = JSON.parse(res.data[i].pictureList);
                        res.data[i].imgUrl = pictureList.length > 0 ? pictureList[0] : '';
                        if (res.data[i].idleStatus === 1) {
                            this.dataList[0].push(res.data[i]);
                        } else if (res.data[i].idleStatus === 2) {
                            this.dataList[1].push(res.data[i]);
                        } else if (res.data[i].idleStatus === 4) {
                            this.dataList[5].push(res.data[i]);
                        } else if (res.data[i].idleStatus === 3) {
                            this.dataList[6].push(res.data[i]);
                        }
                    }
                }
            })
        },
        getAddressData() {
            api.getAddress().then(res => {
                if (res.status_code === 1) {
                    let data = res.data;
                    for (let i = 0; i < data.length; i++) {
                        data[i].detailAddressText = data[i].provinceName + data[i].cityName + data[i].regionName + data[i].detailAddress;
                        data[i].defaultAddress = data[i].defaultFlag ? '默认地址' : '设为默认';
                    }
                    this.addressData = data;
                }
            })
        },
        handleClick(tab, event) {
        },
        saveUserNickname() {
            this.notUserNicknameEdit = true;
            api.updateUserPublicInfo({
                nickname: this.userStore.userInfo.nickname
            })
        },
        saveUserEmailEdit() {
            this.notUserEmailEdit = true;
            const emailPattern = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
            if (emailPattern.test(this.userStore.userInfo.userEmail)) {
                api.updateUserPublicInfo({
                    userEmail: this.userStore.userInfo.userEmail
                })
                return 1;
            } else {
                this.$message.error('邮箱格式不对，请重新填写！');
                return 0;
            }
        },
        savePassword() {
            let passwordReg = /^(?![0-9]+$)(?![a-zA-Z]+$)[0-9A-Za-z]{6,16}$/;
            if (!this.userPassword1 || !this.userPassword2) {
                this.$message.error('密码为空！');
            } else if (!passwordReg.test(this.userPassword2)) {
                this.$message.error('密码必须包含大小写字母和数字，长度在6-16之间！');
            } else if (this.userPassword2 === this.userPassword3) {
                api.updatePassword({
                    oldPassword: this.userPassword1,
                    newPassword: this.userPassword2
                }).then(res => {
                    if (res.status_code === 1) {
                        this.userPasswordEdit = false;
                        this.$message({
                            message: '修改成功！',
                            type: 'success'
                        });
                        this.userPassword1 = '';
                        this.userPassword2 = '';
                        this.userPassword3 = '';
                    } else {
                        this.$message.error('旧密码错误，修改失败！');
                    }
                })
            } else {
                this.$message.error('两次输入的密码不一致！');
            }
        },
        submit() {
            if (this.saveUserEmailEdit() || this.userStore.userInfo.userEmail == null) {
                this.userInfoDialogVisible = false;
            } else {
                this.userInfoDialogVisible = true;
            }
        },
        finishEdit() {
            this.notUserNicknameEdit = true;
            this.notUserEmailEdit = true;
            this.userInfoDialogVisible = false;
            this.userPasswordEdit = false;
        },
        handleAddressChange(value) {
            this.addressInfo.provinceName = value[0];
            this.addressInfo.cityName = value[1];
            this.addressInfo.regionName = value[2];
        },
        handleEdit(index, row) {
            this.addressInfo = JSON.parse(JSON.stringify(row));
            this.selectedOptions = ['', '', ''];
            this.selectedOptions[0] = row.provinceName;
            this.selectedOptions[1] = row.cityName;
            this.selectedOptions[2] = row.regionName;
        },
        handleDelete(index, row) {
            this.$confirm('是否确定删除该地址?', '提示', {
                confirmButtonText: '确定',
                cancelButtonText: '取消',
                type: 'warning'
            }).then(() => {
                api.deleteAddress(row).then(res => {
                    if (res.status_code === 1) {
                        this.$message({
                            message: '删除成功！',
                            type: 'success'
                        });
                        this.addressData.splice(index, 1);
                        if (row.defaultFlag && this.addressData.length > 0) {
                            this.addressData[0].defaultFlag = true;
                            this.addressData[0].defaultAddress = '默认地址';
                            this.update({
                                id: this.addressData[0].id,
                                defaultFlag: true
                            });
                        }
                    } else {
                        this.$message.error('系统异常，删除失败！')
                    }
                }).catch(() => {
                    this.$message.error('网络异常！')
                });
            }).catch(() => {
            });

        },
        handleSetDefault(index, row) {
            row.defaultFlag = true;
            this.update(row);
        },
        toDetails(activeName, item) {
            if (activeName === '4' || activeName === '5') {
                this.$router.push({ path: '/order', query: { id: item.id } });
            } else {
                this.$router.push({ path: '/details', query: { id: item.id } });
            }
        },
        handle(activeName, item, index) {
            this.$confirm('是否确认？', '提示', {
                confirmButtonText: '确认',
                cancelButtonText: '取消',
            }).then(() => {
                if (activeName === '1') {
                    api.updateIdleItem({
                        id: item.id,
                        idleStatus: 2
                    }).then(res => {
                        if (res.status_code === 1) {
                            this.dataList[0].splice(index, 1);
                            item.idleStatus = 2;
                            this.dataList[1].unshift(item);
                        } else {
                            this.$message.error(res.msg)
                        }
                    });
                } else if (activeName === '2') {
                    api.deleteGood(item.id).then(res => {
                        if (res.status_code === 1) {
                            this.dataList[1].splice(index, 1);
                        } else {
                            this.$message.error(res.msg)
                        }
                    });
                } else if (activeName === '3') {
                    api.deleteFavorite({
                        id: item.favoriteId
                    }).then(res => {
                        if (res.status_code === 1) {
                            this.$message({
                                message: '已取消收藏！',
                                type: 'success'
                            });
                            this.dataList[2].splice(index, 1);
                        } else {
                            this.$message.error(res.msg)
                        }
                    }).catch(e => {
                    })
                }
            }).catch(() => {
            });

        },
        uploadAvatar({ file }) {
            // 走 api.uploadFile（axios 实例自动带 JWT），替代原 el-upload 直传硬编码地址
            api.uploadFile(file).then(res => {
                if (res.status_code === 1) {
                    // 后端返回 "/image?imageName=xxx"，提取纯文件名入库
                    const avatar = res.data.split('imageName=')[1] || res.data;
                    api.updateUserPublicInfo({
                        avatar: avatar
                    }).then(() => {
                        this.userStore.userInfo.avatar = avatar;
                    })
                } else {
                    this.$message.error(res.msg || '头像上传失败！')
                }
            }).catch(() => {
                this.$message.error('网络异常，头像上传失败！')
            });
            this.imgFileList = [];
        },
        update(data) {
            // 校验本次要提交的 data，而不是页面顶部“新添地址”表单（原实现用 this.addressInfo，
            // 导致从地址列表点“设为默认”/删除默认后自动提升时永远只弹“请填写完整信息”、不调接口）
            const hasFullInfo = data && data.consigneeName && data.consigneePhone &&
                data.provinceName && data.cityName && data.regionName && data.detailAddress;
            // 仅切换默认地址（{ id, defaultFlag: true }）时无需完整字段
            const isDefaultToggle = data && data.id && data.defaultFlag === true;
            if (!hasFullInfo && !isDefaultToggle) {
                this.$message.error('请填写完整信息');
                return;
            }
            api.updateAddress(data).then(res => {
                if (res.status_code === 1) {
                    this.getAddressData();
                    this.$message({
                        message: '修改成功！',
                        type: 'success'
                    });
                } else {
                    this.$message.error('系统异常，修改失败！')
                }
            }).catch(() => {
                this.$message.error('网络异常！')
            })
        },
        saveAddress() {
            if (this.addressInfo.id) {
                this.update(this.addressInfo);
                this.addressInfo = {
                    consigneeName: '',
                    consigneePhone: '',
                    provinceName: '',
                    cityName: '',
                    regionName: '',
                    detailAddress: '',
                    defaultFlag: false
                };
                this.selectedOptions = [];
            } else {
                if (this.addressData.length >= 5) {
                    this.$message.error('已达到最大地址数量！')
                } else {
                    if (this.addressInfo.consigneeName && this.addressInfo.consigneePhone && this.addressInfo.provinceName && this.addressInfo.cityName && this.addressInfo.regionName && this.addressInfo.detailAddress) {
                        api.addAddress(this.addressInfo).then(res => {
                            if (res.status_code === 1) {
                                this.getAddressData();
                                this.$message({
                                    message: '新增成功！',
                                    type: 'success'
                                });
                                this.selectedOptions = [];
                                this.addressInfo = {
                                    consigneeName: '',
                                    consigneePhone: '',
                                    provinceName: '',
                                    cityName: '',
                                    regionName: '',
                                    detailAddress: '',
                                    defaultFlag: false
                                };
                            } else {
                                this.$message.error('系统异常，新增失败！')
                            }
                        }).catch(e => {
                            this.$message.error('网络异常！')
                        });
                    } else {
                        this.$message.error('请填写完整信息')
                    }
                }
            }
        }
    }
}
</script>

<style scoped>
.user-info-container {
    width: 100%;
    height: 200px;
    border-bottom: 15px solid #f6f6f6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: url("../assets/beibei.png") center  / cover no-repeat;
    box-shadow: 1px 2px 4px rgba(0, 0, 0, 0.1); /* 水平偏移量 | 垂直偏移量 | 模糊半径 | 颜色 */
}

.my-message-box {
    width: 400px;
    height: 200px;
    font-size: 16px;
    /* 其他样式设置 */
}

.user-info-details {
    display: flex;
    height: 140px;
    align-items: center;
    margin: 20px 40px;
}

.user-info-details-text {
    margin-left: 20px;
}

@keyframes colorChange {
    0% {
        color: red;
    }

    50% {
        color: blue;
    }

    100% {
        color: red;
    }
}

.user-info-details-text-nickname {
    font-size: 26px;
    font-weight: 600;
    margin: 10px 0;
}

.user-info-details-text-time {
    font-size: 14px;
    margin-bottom: 10px;
}

.user-info-splace {
    margin-right: 90px;
}

.idle-container {
    padding: 0 20px;
    box-shadow: 1px 2px 4px rgba(0, 0, 0, 0.1); /* 水平偏移量 | 垂直偏移量 | 模糊半径 | 颜色 */
    background: url("../assets/xiaoyxi.png") center top / cover no-repeat;
}

.idle-container-list {
    min-height: 55vh;
}

.idle-container-list-item {
    border-bottom: 1px solid #eeeeee;
    cursor: pointer;
}

.idle-container-list-item:last-child {
    border-bottom: none;
}

.idle-container-list-item-detile {
    height: 120px;
    display: flex;
    align-items: center;
}

.idle-container-list-item-text {
    margin-left: 10px;
    height: 120px;
    max-width: 800px;
    transition: background-color 0.3s ease; /* 添加过渡效果 */
}

.idle-container-list-item-text:hover {
    background-color: #f0f0f0; /* 悬浮时的背景颜色 */
}

.idle-container-list-title {
    font-weight: 600;
    font-size: 18px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.idle-container-list-idle-details {
    font-size: 14px;
    color: #555555;
    padding-top: 5px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

/* 违规/审核未通过商品的处理说明 */
.idle-reason-text {
    font-size: 13px;
    color: #b45309;
    background: #fdf6ec;
    border-radius: 4px;
    padding: 4px 8px;
    margin-top: 6px;
    display: inline-block;
    max-width: 100%;
}

.idle-container-list-idle-time {
    font-size: 13px;
    padding-top: 5px;
}

.idle-price {
    font-size: 20px;
    padding-top: 5px;
    color: red;
    font-weight: bold;
    margin-left: 620px;
}

.edit-tip {
    font-size: 14px;
    margin: 10px 5px;
}

.edit-tip1 {
    font-size: 24px;
    margin: 10px 5px;
    color: red;
    text-align: center;
}

.address-container {
    height: 97%;
    padding: 10px 20px;
    background: url("../assets/jing.png") center / cover no-repeat;
}

.address-container-back {
    margin-bottom: 10px;
}

.address-container-add-title {
    color: #409EFF;
    padding: 10px;
    color: black;
    font-size: 20px;
}

.address-container-add-item {
    margin-bottom: 20px;
}

.demonstration {
    color: #666666;
    font-size: 14px;
    padding: 10px;
}

.address-container-add {
    padding: 0 50px;
}

.address-container-list {
    padding: 30px 50px;
}

.idle-item-foot {
    width: 800px;
    display: flex;
    justify-content: space-between;
    margin-top: 10px;
}

.button1 {
    width: 70px;
    height: 36px;
    font-size: 15px;
    color: white;
    text-align: center;
    background-color: #409EFF;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    justify-content: center; /* 水平居中 */
    float: left;
    margin-right: 15px;
    align-items: center; /* 垂直居中 */

    /* 设置鼠标悬浮样式 */
    cursor: pointer;
    transition: background-color 0.3s ease; /* 添加过渡效果 */

}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
    background-color: #66b1ff; /* 颜色变亮 */
}

.button2 {
    width: 70px;
    height: 36px;
    font-size: 15px;
    color: white;
    text-align: center;
    background-color: #bcd462;
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
.button2:hover {
    background-color: #d2e493; /* 颜色变亮 */
}

.dialog-footer {
    display: flex;
    justify-content: space-between; /* 水平间距平均分布 */
    margin-top: 20px; /* 为了和上面的表单区域有一定的间距 */
}

.button3 {
    width: 60px;
    height: 40px;
    font-size: 14px;
    color: white;
    text-align: center;
    background-color: #d47b62;
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
.button3:hover {
    background-color: #ca9d42; /* 颜色变亮 */
}

.idle-container-list-zu {
    margin-top: 5px;
    display: flex;
    flex-direction: row;
    justify-content: space-between; /* 可根据需要调整对齐方式 */
    align-items: center; /* 可根据需要调整垂直对齐方式 */
}

.center-title {
    text-align: center; /* 文本居中 */
    font-size: 24px; /* 字体大小 */
    font-weight: bold; /* 字体加粗 */
    color: #333; /* 字体颜色 */
    margin-top: 20px; /* 上边距 */
    margin-bottom: 10px;
}

.button4 {
    display: inline-block; /* 行内块元素 */
    padding: 10px 20px; /* 上下左右内边距 */
    background-color: #4CAF50; /* 背景颜色 */
    color: white; /* 文字颜色 */
    border: none; /* 边框 */
    border-radius: 10px; /* 圆角 */
    text-align: center; /* 文本居中 */
    text-decoration: none; /* 文本修饰 */
    font-size: 16px; /* 字体大小 */
    margin: 0 auto; /* 水平居中 */
    cursor: pointer; /* 鼠标指针 */
    margin-left: 100px;
}

/* 悬停时改变按钮颜色 */
.button4:hover {
    background-color: #45a049;
}

.user-info-details-text-vip {
    color: #ffeb00;
    margin-top: 20px;
    margin-left: 10px;
    font-size: 17px;
    font-weight: bolder;
}

.user-info-details-text-not-vip {
    color: #a0a0a0;
    ;
    margin-top: 20px;
    margin-left: 10px;
    font-size: 17px;
    font-weight: bolder;
}

.container {
    display: flex;
}

.user-info-details-text-vip-expire {
    margin-top: 3px;
    margin-left: 10px;
}
</style>
