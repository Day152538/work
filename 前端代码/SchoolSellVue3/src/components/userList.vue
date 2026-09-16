<template>
    <div class="main-border">
        <!-- 统一搜索栏 -->
        <admin-search-bar v-model="searchText" placeholder="输入用户账号 / 昵称搜索" @search="search" />
        <el-menu default-active="1" class="el-menu-demo" mode="horizontal" @select="handleSelect">
            <el-menu-item index="1">正常用户</el-menu-item>
            <el-menu-item index="2">违规用户</el-menu-item>
            <el-menu-item index="3">管理员</el-menu-item>

            <div v-show="mode == 3" class="addAdminButton">
                <div class="button3" @click="adminRegVisible = true"> 添加管理员</div>
                <el-dialog title="添加管理员" v-model="adminRegVisible" width="25%">
                    <span style="margin-left: 10px">新增管理员名称</span>
                    <el-input v-model="adminName" maxlength="8" placeholder="请输入管理员名称" style="padding: 10px 0" clearable
                        required></el-input>
                    <span style="margin-left: 10px">新增管理员账号</span>
                    <el-input v-model="adminAccount" minlength="8" maxlength="10" placeholder="请输入管理员账户"
                        style="padding: 10px 0" clearable required></el-input>
                    <span style="margin-left: 10px">新增管理员密码</span>
                    <el-input v-model="adminPassword" minlength="8" placeholder="请输入管理员密码" style="padding: 10px 0"
                        show-password required></el-input>
                    <span style="margin-left: 10px">确认管理员密码</span>
                    <el-input v-model="adminRePassword" minlength="10" placeholder="请再次输入管理员密码" style="padding: 10px 0"
                        show-password required></el-input>
                    <template #footer>
                        <span class="dialog-footer">
                            <div class="button1" @click="regAdmin"> 添加</div>
                            <div class="button2" @click="adminRegVisible = false"> 取消</div>
                        </span>
                    </template>
                </el-dialog>
            </div>
        </el-menu>
        <el-table v-show="mode == 1" :data="userData" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column label="头像" width="100">
                <template #default="scope">
                    <el-avatar shape="square" :size="70" :src="avatarUrl(scope.row.avatar)"></el-avatar>
                </template>
            </el-table-column>
            <el-table-column prop="accountNumber" label="用户账号" show-overflow-tooltip min-width="180" width="180">
            </el-table-column>
            <el-table-column prop="nickname" label="用户昵称" show-overflow-tooltip min-width="150" width="150">
            </el-table-column>
            <el-table-column prop="signInTime" label="注册时间" show-overflow-tooltip width="220">
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <div style="display: flex; align-items: center;justify-content: center;">
                        <div class="button3" @click="sealUser(scope.$index)">
                            封号</div>
                        <div class="button1" @click="resetPassword(scope.row)">
                            重置密码</div>
                    </div>
                </template>
            </el-table-column>
        </el-table>
        <el-table v-show="mode == 2" :data="badUserData" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column label="头像" width="100">
                <template #default="scope">
                    <el-avatar shape="square" :size="70" :src="avatarUrl(scope.row.avatar)"></el-avatar>
                </template>
            </el-table-column>
            <el-table-column prop="accountNumber" label="用户账号" show-overflow-tooltip min-width="180" width="180">
            </el-table-column>
            <el-table-column prop="nickname" label="用户昵称" show-overflow-tooltip width="150">
            </el-table-column>
            <el-table-column prop="signInTime" label="注册时间" show-overflow-tooltip width="220">
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <div style="display: flex; align-items: center;justify-content: center;">
                        <div class="button2" @click="unsealUser(scope.$index)">
                            解封</div>
                        <el-popconfirm @confirm="deleteUsers(scope.row)" title="确定删除？">
                            <template #reference>
                                <div class="button3" style="margin-left: 10px;">删除</div>
                            </template>
                        </el-popconfirm>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <el-table v-show="mode == 3" :data="userManage" stripe style="width: 100%;color: #5a5c61;">
            <el-table-column prop="accountNumber" label="管理员账号" show-overflow-tooltip width="200">
            </el-table-column>
            <el-table-column prop="adminName" label="管理名称">
            </el-table-column>
            <el-table-column label="操作">
                <template #default="scope">
                    <div style="display: flex; align-items: center;justify-content: center;">
                        <!-- 修改按钮 -->
                        <div class="button1" @click="handleEdit(scope.row)">修改</div>

                        <!-- 删除按钮 -->
                        <el-popconfirm @confirm="handleDelete(scope.row)" title="确定删除？">
                            <template #reference>
                                <div class="button3" style="margin-left: 10px;">删除</div>
                            </template>
                        </el-popconfirm>
                    </div>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="dialogVisible" title="修改管理员信息">
            <el-form :model="editForm" label-width="80px">
                <el-form-item label="管理员账号">
                    <el-input v-model="editForm.accountNumber" disabled></el-input>
                </el-form-item>
                <el-form-item label="管理名称">
                    <el-input v-model="editForm.adminName"></el-input>
                </el-form-item>
                <el-form-item label="密码">
                    <el-input v-model="editForm.adminPassword"></el-input>
                </el-form-item>
            </el-form>
            <template #footer>
                <div class="dialog-footer">
                    <div class="button1" @click="updateAdministrator">确定</div>
                    <div class="button2" @click="dialogVisible = false">取消</div>
                </div>
            </template>
        </el-dialog>

        <!-- 重置密码弹窗 -->
        <el-dialog title="重置密码" v-model="resetPasswordDialogVisible" width="30%">
            <span style="margin-left: 10px">请输入新密码</span>
            <el-input v-model="newPassword" type="password" placeholder="请输入新密码" style="padding: 10px 0"
                show-password></el-input>
            <span style="margin-left: 10px">请确认新密码</span>
            <el-input v-model="confirmPassword" type="password" placeholder="请再次输入新密码" style="padding: 10px 0"
                show-password></el-input>
            <template #footer>
                <span class="dialog-footer">
                    <div class="button1" @click="confirmResetPassword">确认</div>
                    <div class="button2" @click="resetPasswordDialogVisible = false">取消</div>
                </span>
            </template>
        </el-dialog>

        <!-- 分页（本地切片，随当前 Tab 变化，翻页无需重新请求） -->
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
 * - this.$api → import api from '@/api'；所有管理操作走 admin 接口
 * - 越权隐患整改：重置密码 userRet1(/user/reset1-password 任意人重置任意账号)
 *   → api.adminResetPassword(/admin/user/reset-password)；删除用户 deleteUser(/user/delete/{id})
 *   → api.adminDeleteUser(/admin/user/delete/{id})，均需 admin_token
 * - 头像地址：$store.state.baseApi + avatar → imageUrl()（utils/request），兼容
 *   后端存的 "/image?imageName=xxx" 与纯文件名两种格式
 * - Element Plus：:visible.sync → v-model、slot-scope → #default、slot="footer" → #footer、
 *   popconfirm slot="reference" → #reference
 * - 删除 console.log、未使用的 messageData 与死代码 handleCurrentChange（分页已被注释）
 */
import api from '@/api'
import { imageUrl } from '@/utils/request'

export default {
    name: "userList",
    created() {
        this.getUserData();
    },
    methods: {
        avatarUrl(avatar) {
            if (!avatar) return '';
            // 后端默认头像存的是 "/image?imageName=xxx"，用户上传的存纯文件名
            const m = /imageName=([^&]+)/.exec(avatar);
            return imageUrl(m ? m[1] : avatar.replace(/^\//, ''));
        },
        handleEdit(row) {
            this.editForm = {
                id: row.id,
                accountNumber: row.accountNumber,
                adminName: row.adminName,
                adminPassword: ''
            };
            this.dialogVisible = true;
        },
        handleDelete(row) {
            api.deleteAdministrator(row.id).then(() => {
                this.getUserManage();
                this.$message.success('删除成功');
            }).catch(() => {
                this.$message.error('删除失败');
            });
        },
        updateAdministrator() {
            if (this.editForm.adminPassword) {
                api.updateAdministrator(this.editForm).then(() => {
                    this.getUserManage();
                    this.dialogVisible = false;
                    this.$message.success('修改成功');
                }).catch(() => {
                    this.$message.error('修改失败');
                });
            }
            else {
                this.$message.error('请输入完整信息');
            }
        },
        // 展示 = 当前模式的全量 → 按关键字过滤 → 按当前页切片（本地完成，翻页不请求后端）
        renderCurrent() {
            const kw = this.searchText.trim().toLowerCase();
            const size = this.pageSize;
            const matchUser = (item) => !kw ||
                String(item.nickname || '').toLowerCase().includes(kw) ||
                String(item.accountNumber || '').toLowerCase().includes(kw);
            const matchAdmin = (item) => !kw ||
                String(item.adminName || '').toLowerCase().includes(kw) ||
                String(item.accountNumber || '').toLowerCase().includes(kw);
            const toPage = (full, match) => {
                const filtered = full.filter(match);
                const totalPages = Math.max(1, Math.ceil(filtered.length / size));
                if (this.nowPage > totalPages) this.nowPage = totalPages;
                const start = (this.nowPage - 1) * size;
                return { rows: filtered.slice(start, start + size), total: filtered.length };
            };
            let pageResult;
            if (this.mode == 1) {
                pageResult = toPage(this._userData, matchUser);
                this.userData = pageResult.rows;
            } else if (this.mode == 2) {
                pageResult = toPage(this._badUserData, matchUser);
                this.badUserData = pageResult.rows;
            } else {
                pageResult = toPage(this._userManage, matchAdmin);
                this.userManage = pageResult.rows;
            }
            this.total = pageResult.total;
        },
        // 搜索 / 清空关键字：回到第 1 页重新过滤
        search() {
            this.nowPage = 1;
            this.renderCurrent();
        },
        // 翻页（本地切片）
        handleCurrentChange(page) {
            this.nowPage = page;
            this.renderCurrent();
        },
        resetPassword(user) {
            this.currentUser = user;
            this.resetPasswordDialogVisible = true;
        },
        confirmResetPassword() {
            if (this.newPassword !== this.confirmPassword) {
                this.$message.error('两次输入的密码不一致');
                return;
            }
            if (!this.newPassword) {
                this.$message.error('请填写完整信息');
                return;
            }
            // 管理员重置用户密码：走 /admin/user/reset-password（原 userRet1 无鉴权，已删除）
            api.adminResetPassword({
                id: this.currentUser.id,
                newPassword: this.newPassword
            }).then(() => {
                this.$message.success('密码重置成功');
                this.resetPasswordDialogVisible = false;
            }).catch(() => {
                this.$message.error('密码重置失败');
            });
        },
        handleSelect(val) {
            if (this.mode !== val) {
                this.mode = val
                this.nowPage = 1;
                if (val == 1) {
                    this.getUserData();
                }
                if (val == 2) {
                    this.getBadUserData();
                }
                if (val == 3) {
                    this.getUserManage();
                }
            }
        },
        getUserData() {
            //正常普通用户
            api.getUserData({
                page: 1,
                nums: 10000,
                status: 0
            }).then(res => {
                if (res.status_code == 1) {
                    this._userData = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg);
                    this.$router.push({ path: '/login-admin' });
                }
            }).catch(() => {
            })
        },
        getBadUserData() {
            //违规用户
            api.getUserData({
                page: 1,
                nums: 10000,
                status: 1
            }).then(res => {
                if (res.status_code == 1) {
                    this._badUserData = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                }
            }).catch(() => {
            });
        },
        getUserManage() {
            //管理员
            api.getUserManage({
                page: 1,
                nums: 10000,
            }).then(res => {
                if (res.status_code == 1) {
                    this._userManage = res.data.list || [];
                    this.renderCurrent();
                } else {
                    this.$message.error(res.msg)
                }
            }).catch(() => {
            })
        },
        sealUser(i) {
            api.updateUserStatus({
                id: this.userData[i].id,
                status: 1
            }).then(res => {
                if (res.status_code == 1) {
                    this.getUserData();
                } else {
                    this.$message.error(res.msg)
                }
            }).catch(() => {
            })
        },
        unsealUser(i) {
            api.updateUserStatus({
                id: this.badUserData[i].id,
                status: 0
            }).then(res => {
                if (res.status_code == 1) {
                    this.getBadUserData();
                } else {
                    this.$message.error(res.msg)
                }

            }).catch(() => {
            })
        },
        deleteUsers(row) {
            // 管理员删除用户：走 /admin/user/delete/{id}（原 /user/delete/{id} 无鉴权，已删除）
            api.adminDeleteUser(row.id).then(res => {
                if (res.status_code == 1) {
                    this.getBadUserData();
                    this.$message.success("删除成功");
                } else {
                    this.$message.error(res.msg);
                }

            }).catch(() => {
            })
        },
        regAdmin() {
            if (this.adminPassword) {
                if (this.adminPassword == this.adminRePassword) {
                    api.regAdministrator({
                        adminName: this.adminName,
                        accountNumber: this.adminAccount,
                        adminPassword: this.adminPassword,
                    }).then(res => {
                        if (res.status_code == 1) {
                            this.total = this.total + 1;
                            this.getUserManage();
                        } else {
                            this.$message.error(res.msg)
                        }
                    }).catch(() => {
                        this.$message.error("添加失败，账号重复或网络异常")
                    });
                    this.adminRegVisible = false
                }
                else {
                    this.$message.error("两次输入的密码不一致");
                }
            }
            else {
                this.$message.error("请填写完整信息");
            }
        },
    },
    data() {
        return {
            mode: 1,
            nowPage: 1,
            total: 0,
            pageSize: 10,
            adminRegVisible: false,
            adminAccount: '',
            adminPassword: '',
            adminRePassword: '',
            adminName: '',
            searchText: '', // 搜索关键词
            // 全量原始列表（表格展示的是过滤后的结果，避免二次搜索叠加过滤）
            _userData: [],
            _badUserData: [],
            _userManage: [],
            userData: [],
            badUserData: [],
            userManage: [],
            resetPasswordDialogVisible: false,
            newPassword: '',
            confirmPassword: '',
            currentUser: null,
            dialogVisible: false, // 弹窗可见性
            editForm: { // 编辑表单数据
                id: '',
                accountNumber: '',
                adminName: '',
                adminPassword: ''
            }
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

.addAdminButton {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    height: 60px;
    outline: none;
}
.search-bar {
  width: 700px;
  height: 50px;
}
.button1,
.button2,
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

.button2 {
    background-color: #67c23a;
}

.button3 {
    background-color: #f56c6c;
}

.button1:hover,
.button2:hover,
.button3:hover {
    opacity: 0.85;
}

.dialog-footer {
    display: flex;
    justify-content: space-between; /* 水平间距平均分布 */
    margin-top: 20px; /* 为了和上面的表单区域有一定的间距 */
}
</style>
