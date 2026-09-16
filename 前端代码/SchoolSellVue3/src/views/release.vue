<template>
    <div>
        <app-head></app-head>
        <app-body>
            <div class="release-idle-container">
                <div class="release-idle-container-title">上传您的商品</div>
                <hr>
                <div class="release-idle-container-form">
                    <!-- 统一为「label 在上、控件在下」的 el-form 布局，所有字段的标签与控件风格一致 -->
                    <el-form label-position="top" class="release-form">
                        <el-form-item label="物品名称" required>
                            <el-input placeholder="请输入物品名称" v-model="idleItemInfo.idleName" maxlength="30"
                                show-word-limit clearable></el-input>
                        </el-form-item>

                        <el-form-item label="物品标签（可选）">
                            <el-input placeholder="多个标签用逗号分隔，如：可爱,萌宠,包邮" v-model="idleItemInfo.idleTags"
                                maxlength="80" show-word-limit clearable></el-input>
                            <div class="release-tip">标签会显示在商品详情页名称下方，点击标签可找到同标签的商品</div>
                        </el-form-item>

                        <el-form-item label="所在地区" required>
                            <el-cascader :options="options" v-model="selectedOptions" @change="handleChange"
                                :separator="' '" placeholder="请选择学校 / 校区 / 楼号" clearable
                                style="width: 100%;">
                            </el-cascader>
                        </el-form-item>

                        <div class="release-form-row">
                            <el-form-item label="商品类别" required class="release-form-col">
                                <el-select v-model="idleItemInfo.idleLabel" placeholder="请选择类别" clearable
                                    style="width: 100%;">
                                    <el-option v-for="item in options2" :key="item.value" :label="item.label"
                                        :value="item.value">
                                    </el-option>
                                </el-select>
                            </el-form-item>
                            <el-form-item label="价格（元）" required class="release-form-col">
                                <el-input-number v-model="idleItemInfo.idlePrice" :precision="2" :step="5"
                                    :min="0" :max="10000000" controls-position="right"
                                    style="width: 100%;"></el-input-number>
                            </el-form-item>
                        </div>

                        <el-form-item label="库存数量（件）" required>
                            <el-input-number v-model="idleItemInfo.stock" :min="1" :max="9999"
                                :precision="0" controls-position="right" style="width: 100%;"></el-input-number>
                            <div class="release-tip">多件可售：每成交 1 件库存 -1、销量 +1，售罄后自动下架</div>
                        </el-form-item>

                        <el-form-item label="物品描述" required>
                            <el-input type="textarea" :autosize="{ minRows: 4, maxRows: 12 }"
                                maxlength="2000" show-word-limit placeholder="请输入物品的详细介绍，或填好标题、传好照片后用「AI 智能预填」自动生成..."
                                v-model="idleItemInfo.idleDetails"></el-input>
                        </el-form-item>

                        <el-form-item label="物品照片（最多上传 3 张）">
                            <div class="upload-panel">
                                <el-upload :http-request="uploadRequest" :on-preview="fileHandlePreview"
                                    :on-remove="fileHandleRemove" :show-file-list="showFileList"
                                    :limit="3" :on-exceed="handleExceed" accept="image/*" drag multiple>
                                    <div class="upload-placeholder">
                                        <img :src="uploadPng" class="upload-icon" alt="上传" />
                                        <div class="el-upload__text"><em>点击上传</em> 或拖拽图片到此处</div>
                                    </div>
                                </el-upload>
                                <div class="pic_upload">最多上传 3 张照片，点击列表可查看大图</div>
                                <el-dialog v-model="imgDialogVisible" width="60%">
                                    <img width="100%" :src="imageUrl(dialogImageUrl)" alt="">
                                </el-dialog>
                            </div>
                        </el-form-item>

                        <!-- AI 能力入口：智能预填 + 发布前合规自检 -->
                        <div class="ai-prefill-bar">
                            <div class="ai-prefill-bar-text">
                                <strong>✨ AI 智能预填</strong>：已填标题并上传照片？让 AI 帮你补全
                                「标题规范 / 分类 / 商品描述 / 参考价」，参考价会结合平台成交行情并给出依据，生成后逐项确认再发布
                            </div>
                            <div class="ai-prefill-actions">
                                <el-button plain :loading="aiComplianceLoading"
                                    @click="aiComplianceClick">🛡 发布前合规自检</el-button>
                                <el-button type="primary" plain :loading="aiLoading" :icon="MagicStick"
                                    @click="aiPrefillClick">
                                    {{ aiLoading ? 'AI 生成中…' : 'AI 一键预填' }}
                                </el-button>
                            </div>
                        </div>

                        <!-- 合规自检结果（LOW/MEDIUM/HIGH + 原因），只提示不阻塞手动发布 -->
                        <div v-if="aiComplianceResult.riskLevel" class="ai-compliance-result"
                            :class="'risk-' + aiComplianceResult.riskLevel.toLowerCase()">
                            <strong>{{ complianceTitle }}</strong>
                            <span v-if="aiComplianceResult.reasons && aiComplianceResult.reasons.length">：
                                {{ aiComplianceResult.reasons.join('；') }}
                            </span>
                            <span v-else>：未发现明显风险</span>
                        </div>

                        <div class="release-submit">
                            <div class="button1" plain @click="releaseButton"> 确认发布</div>
                        </div>
                    </el-form>

                    <!-- AI 草稿确认对话框：展示四项草稿，用户逐项修改后点「应用到表单」才回填 -->
                    <el-dialog v-model="aiDialogVisible" title="🤖 AI 已生成发布草稿，请逐项确认" width="660px"
                        :close-on-click-modal="false">
                        <el-form label-position="top" class="ai-draft-form">
                            <el-form-item label="标题">
                                <el-input v-model="aiDraft.name" maxlength="60" show-word-limit
                                    placeholder="AI 建议的标题"></el-input>
                            </el-form-item>
                            <div class="ai-draft-row">
                                <el-form-item label="商品分类" class="ai-draft-col">
                                    <el-select v-model="aiDraft.labelId" placeholder="保持我原来的选择" clearable
                                        style="width: 100%;">
                                        <el-option v-for="item in options2" :key="item.value" :label="item.label"
                                            :value="item.value"></el-option>
                                    </el-select>
                                    <div class="ai-draft-tip">不选择则沿用你表单里已选的分类</div>
                                </el-form-item>
                                <el-form-item label="参考价（元，仅建议）" class="ai-draft-col">
                                    <el-input-number v-model="aiDraft.suggestPrice" :precision="2" :step="10"
                                        :min="0" :max="100000" controls-position="right" style="width: 100%;"></el-input-number>
                                    <div class="ai-draft-tip">留 0 表示不修改你填的价格</div>
                                </el-form-item>
                            </div>
                            <div v-if="aiDraft.priceReason" class="ai-draft-reason">
                                📈 定价依据：{{ aiDraft.priceReason }}
                            </div>
                            <el-form-item label="商品描述">
                                <el-input type="textarea" :autosize="{ minRows: 6, maxRows: 14 }"
                                    v-model="aiDraft.details" maxlength="2000" show-word-limit></el-input>
                            </el-form-item>
                        </el-form>

                        <!-- 精修：对上一版草稿不满意，输入修改意见重新生成 -->
                        <div class="ai-refine-bar">
                            <el-input v-model="aiRefineText" type="textarea" :rows="2" resize="none"
                                maxlength="200" show-word-limit
                                placeholder="不满意？告诉我怎么改，例如：描述改成口语化、加上“可小刀”、标题保留我的原名、突出电池续航…"></el-input>
                            <el-button type="warning" plain :loading="aiRefining"
                                @click="aiRefineDraft">按我的要求重新生成</el-button>
                        </div>

                        <template #footer>
                            <el-button @click="aiDialogVisible = false">算了，我自己写</el-button>
                            <el-button type="primary" @click="applyAiDraft">应用到表单</el-button>
                        </template>
                    </el-dialog>
                </div>
            </div>
        </app-body>
        <app-foot></app-foot>
    </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 上传改 api.uploadFile 自定义 http-request（原 el-upload action 硬编码
 *    http://localhost:9321/file/，且不带 JWT 会 401）；后端返回 "/image?imageName=xxx"，
 *    统一剥成纯文件名存入 pictureList
 * 2. require/相对路径静态资源 → 顶部 import（Vite 不支持 require）
 * 3. 组件路径改 '@/components/xxx'；:visible.sync → v-model；slot → 具名插槽
 * 4. 修复提示文案 bug：limit=3 但超限提示写"限制7张" → 改为 3
 * 5. 表单排布统一：名称/地区/类别/价格/描述/照片统一改为 label 上置的 el-form 布局，
 *    类别与价格同排等宽；发布前按字段分别给出缺失提示
 * 6. 新增“库存数量”字段：多件可售，后端付款成功时库存 -1、销量 +1，售罄自动下架
 */
import AppHead from '@/components/AppHeader.vue';
import AppBody from '@/components/AppPageBody.vue'
import AppFoot from '@/components/AppFoot.vue'
import options from '@/components/country-data.js'
import uploadPng from '@/assets/upload.png'
import { MagicStick } from '@element-plus/icons-vue'
import api from '@/api'
import { imageUrl } from '@/utils/request'
import { compressImageFile } from '@/utils/image-compress'

// 兼容旧数据：后端上传接口返回 "/image?imageName=xxx"，剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
    name: "release",
    components: {
        AppHead,
        AppBody,
        AppFoot
    },
    data() {
        return {
            uploadPng,
            imgDialogVisible: false,
            dialogImageUrl: '',
            showFileList: true,
            options: options,
            selectedOptions: [],
            options2: [],
            imgList: [],
            idleItemInfo: {
                idleName: '',
                idleDetails: '',
                pictureList: '',
                idlePrice: 0,
                idlePlace: '',
                idleLabel: '',
                idleTags: '',
                stock: 1
            },
            // AI 智能预填 / 合规自检相关状态（2026-09 新增）
            aiLoading: false,
            aiDialogVisible: false,
            aiComplianceLoading: false,
            aiComplianceResult: {
                riskLevel: null,
                reasons: []
            },
            // 精修：对上一版草稿输入修改意见，重新生成
            aiRefineText: '',
            aiRefining: false,
            aiDraft: {
                name: '',
                details: '',
                labelId: null,
                suggestPrice: 0,
                priceReason: ''
            }
        };
    },
    methods: {
        imageUrl,
        // 获取类型列表
        listType() {
            api.listType({ begin: 0, size: 999 }).then((res) => {
                for (var i = 0; i < res.data.length; ++i) {
                    // 将类型信息添加到选项列表中
                    this.options2.push({
                        value: res.data[i].id,
                        label: res.data[i].name
                    })
                }
            })
        },
        // 处理选择框数值变化事件
        handleChange(value) {
            // 更新闲置物品信息中的闲置地点（取“校区”一档，与原有行为一致）
            this.idleItemInfo.idlePlace = value[1];
        },
        // 处理文件移除事件
        fileHandleRemove(file) {
            // 从图片列表中移除被移除的文件（自定义上传里已把文件名记到 file.response.data）
            const name = file.response && file.response.data;
            const i = this.imgList.indexOf(name);
            if (i > -1) {
                this.imgList.splice(i, 1);
            }
        },
        // 处理文件预览事件
        fileHandlePreview(file) {
            // 显示图片预览对话框
            this.dialogImageUrl = file.response.data;
            this.imgDialogVisible = true;
        },
        // 自定义上传：先压缩（≤1280px/≤1.5MB，P0-2）再走 api.uploadFile（携带 JWT），
        // 后端返回 "/image?imageName=xxx"
        uploadRequest(option) {
            compressImageFile(option.file)
                .then((compressed) => api.uploadFile(compressed))
                .then(res => {
                    if (res.status_code === 1) {
                        const name = toImageName(res.data);
                        this.imgList.push(name);
                        // 让 el-upload 记录 response，供预览/移除使用
                        option.onSuccess({ data: name });
                    } else {
                        this.$message.error(res.msg || '图片上传失败');
                        option.onError(new Error(res.msg));
                    }
                })
                .catch(() => {
                    this.$message.error('图片上传失败');
                    option.onError(new Error('upload failed'));
                });
        },
        // 发布按钮点击事件
        releaseButton() {
            const info = this.idleItemInfo;
            // 逐项校验，缺失时给出对应字段的明确提示（不再笼统弹“请填写完整信息”）
            if (!info.idleName || !info.idleName.trim()) {
                this.$message.error('请输入物品名称');
                return;
            }
            if (!this.selectedOptions || this.selectedOptions.length === 0) {
                this.$message.error('请选择所在地区');
                return;
            }
            if (!info.idleLabel) {
                this.$message.error('请选择商品类别');
                return;
            }
            if (!info.idlePrice || info.idlePrice <= 0) {
                this.$message.error('请输入正确的价格');
                return;
            }
            if (!info.stock || info.stock < 1) {
                this.$message.error('库存数量至少为 1 件');
                return;
            }
            if (!info.idleDetails || !info.idleDetails.trim()) {
                this.$message.error('请输入物品描述');
                return;
            }
            if (this.imgList.length === 0) {
                this.$message.error('请至少上传一张物品照片');
                return;
            }

            // 将图片列表转换为JSON字符串，并更新闲置物品信息
            this.idleItemInfo.pictureList = JSON.stringify(this.imgList);

            api.addIdleItem(this.idleItemInfo).then(res => {
                if (res.status_code === 1) {
                    // 提示发布成功并跳转到详情页
                    this.$message({
                        message: '发布成功！',
                        type: 'success'
                    });
                    this.$router.replace({ path: '/details', query: { id: res.data.id } });
                } else {
                    // 提示发布失败原因
                    this.$message.error('发布失败！' + res.msg);
                }
            }).catch(() => {
                this.$message.error('发布失败，请稍后重试');
            })
        },
        // 把图片发给第三方模型前做隐私二次确认（P1-4）；返回 Promise<boolean>
        confirmSendImages() {
            return this.$confirm(
                '图片将被发送至第三方 AI 模型（云服务）用于识别/审查。若图中含人脸、证件、他人手机号等隐私信息，请勿发送。是否继续？',
                'AI 图片上传确认',
                { type: 'warning', confirmButtonText: '继续发送', cancelButtonText: '取消' }
            ).then(() => true).catch(() => false);
        },
        // AI 一键预填入口：无图可纯文本；有图先做隐私确认
        async aiPrefillClick() {
            const title = (this.idleItemInfo.idleName || '').trim();
            const hasImg = this.imgList.length > 0;
            if (!title && !hasImg) {
                this.$message.warning('请先填写物品名称或上传至少一张照片，AI 才能生成草稿');
                return;
            }
            if (hasImg && !(await this.confirmSendImages())) {
                return; // 用户取消：不发图、不调用
            }
            if (!hasImg) {
                this.$message.info('未检测到照片，将按纯文本模式为你的标题生成描述');
            }
            this.doAiPrefill({
                title: title,
                labelId: this.idleItemInfo.idleLabel || null,
                imageNames: this.imgList.slice(0, 3)
            });
        },
        // 实际调 AI 预填：把标题/分类/已传图片交给后端 AI 网关，返回草稿弹窗确认
        doAiPrefill(payload) {
            this.aiLoading = true;
            api.aiPrefill(payload).then((res) => {
                if (res.status_code === 1 && res.data) {
                    this.fillAiDraft(res.data);
                    this.aiDialogVisible = true;
                } else {
                    // 业务失败（未配置 key / 限流 / 模型报错等）：给出后端文案，不阻塞手动发布
                    this.$message.error(res.msg || 'AI 生成失败，可手动填写后发布');
                }
            }).catch(() => {
                // HTTP/网络错误已由 request.js 统一提示
            }).finally(() => {
                this.aiLoading = false;
            });
        },
        // 把后端草稿映射进 aiDraft（预填 / 精修共用）
        fillAiDraft(d) {
            const price = parseFloat(d.suggestPrice);
            this.aiDraft = {
                name: d.name || '',
                details: d.details || '',
                labelId: (d.labelId != null && d.labelId !== '') ? Number(d.labelId) : null,
                suggestPrice: price > 0 ? price : 0,
                priceReason: d.priceReason || ''
            };
        },
        // 精修：把“上一版草稿 + 用户修改意见”发给 AI 重新生成，仍在弹窗内更新
        aiRefineDraft() {
            const ins = (this.aiRefineText || '').trim();
            if (!ins) {
                this.$message.warning('请先输入你的修改要求，例如“改成口语化、加上可小刀”');
                return;
            }
            const d = this.aiDraft;
            const draftText = `标题：${d.name || '（空）'}\n分类id：${d.labelId == null ? '未定' : d.labelId}`
                + `\n参考价：${d.suggestPrice > 0 ? d.suggestPrice : '未定'}`
                + `\n描述：${d.details || '（空）'}`;
            this.aiRefining = true;
            api.aiPrefill({
                title: (this.idleItemInfo.idleName || '').trim(),
                labelId: d.labelId != null ? d.labelId : (this.idleItemInfo.idleLabel || null),
                imageNames: this.imgList.slice(0, 3),
                instructions: ins,
                draft: draftText
            }).then((res) => {
                if (res.status_code === 1 && res.data) {
                    this.fillAiDraft(res.data);
                    this.aiRefineText = '';
                    this.$message.success('已按你的要求重新生成，请继续核对');
                } else {
                    this.$message.error(res.msg || '重新生成失败，请稍后再试');
                }
            }).catch(() => {
                // HTTP/网络错误已由 request.js 统一提示
            }).finally(() => {
                this.aiRefining = false;
            });
        },
        // 发布前合规自检：标题/描述/图片交给 AI 做内容安全初审，结果只提示不阻塞
        async aiComplianceClick() {
            const title = (this.idleItemInfo.idleName || '').trim();
            const details = (this.idleItemInfo.idleDetails || '').trim();
            const hasImg = this.imgList.length > 0;
            if (!title && !details && !hasImg) {
                this.$message.warning('请先填写标题/描述或上传照片，再进行合规自检');
                return;
            }
            if (hasImg && !(await this.confirmSendImages())) {
                return;
            }
            this.aiComplianceLoading = true;
            api.aiCompliance({
                title: title,
                details: details,
                imageNames: this.imgList.slice(0, 3)
            }).then((res) => {
                if (res.status_code === 1 && res.data) {
                    this.aiComplianceResult = {
                        riskLevel: (res.data.riskLevel || 'LOW').toUpperCase(),
                        reasons: Array.isArray(res.data.reasons) ? res.data.reasons : []
                    };
                } else {
                    this.$message.error(res.msg || '合规自检暂不可用，可继续手动发布');
                }
            }).catch(() => {
                // HTTP/网络错误已由 request.js 统一提示
            }).finally(() => {
                this.aiComplianceLoading = false;
            });
        },
        complianceTitle() {
            const lv = this.aiComplianceResult && this.aiComplianceResult.riskLevel;
            if (lv === 'HIGH') return '⚠ 高风险：建议修改后再发布';
            if (lv === 'MEDIUM') return '⚡ 中风险：请重点核对后再发布';
            return '✅ 低风险';
        },
        // 用户确认后把 AI 草稿回填发布表单（逐项判断，不覆盖用户已手填内容）
        applyAiDraft() {
            const d = this.aiDraft;
            if (d.name && d.name.trim()) {
                this.idleItemInfo.idleName = d.name.trim();
            }
            if (d.details && d.details.trim()) {
                this.idleItemInfo.idleDetails = d.details.trim();
            }
            if (d.labelId != null && d.labelId !== '') {
                this.idleItemInfo.idleLabel = Number(d.labelId);
            }
            if (d.suggestPrice > 0) {
                this.idleItemInfo.idlePrice = Number(d.suggestPrice);
            }
            this.aiDialogVisible = false;
            this.$message.success('AI 草稿已应用，请核对信息后再发布');
        },
        // 处理文件数量超出限制事件
        handleExceed(files, fileList) {
            // 提示文件数量超出限制信息
            this.$message.warning(`限制3张图片，本次选择了 ${files.length} 张图，共选择了 ${files.length + fileList.length} 张图`);
        },
    },
    mounted() {
        this.listType()
    }
}
</script>

<style scoped>
.release-idle-container {
    height: 100%;
    background: url("../../assets/xiaoyxi.png") center top / cover no-repeat;
    box-shadow: 1px 2px 4px rgba(0, 0, 0, 0.1); /* 水平偏移量 | 垂直偏移量 | 模糊半径 | 颜色 */
}

.release-idle-container-title {
    padding: 30px 0;
    font-weight: 600;
    width: 100%;
    text-align: center;
    background-clip: text;
    /* 裁剪背景图，使文字作为裁剪区域向外裁剪 */
    background-size: 200% 100%;
    font-size: 30px;
}

/* 表单内容居中、留白统一 */
.release-idle-container-form {
    max-width: 760px;
    margin: 0 auto 30px;
    padding: 0 40px;
}

.release-form {
    text-align: left;
}

/* 「类别 + 价格」并排、两列等宽 */
.release-form-row {
    display: flex;
    column-gap: 24px;
}

.release-form-col {
    flex: 1;
}

/* 上传区域：让拖拽框占满整行，图标与文案居中 */
.upload-panel {
    width: 100%;
}

.upload-panel :deep(.el-upload) {
    width: 100%;
}

.upload-panel :deep(.el-upload-dragger) {
    width: 100%;
    padding: 20px 0 6px;
}

.upload-placeholder {
    padding: 6px 0 10px;
}

.upload-icon {
    width: 60px;
    height: 60px;
    display: block;
    margin: 0 auto 8px;
}

.pic_upload {
    color: red;
    text-align: center;
    margin-top: 8px;
    font-size: 13px;
}

.release-tip {
    color: #999999;
    font-size: 12px;
    margin-top: 4px;
}

.release-submit {
    display: flex;
    justify-content: center;
    margin: 30px 0;
}

.button1 {
    width: 120px;
    height: 50px;
    font-size: 20px;
    color: white;
    text-align: center;
    background-color: #409EFF;
    border-radius: 10px;
    /* 使用Flexbox布局 */
    display: flex;
    justify-content: center;
    /* 水平居中 */
    align-items: center;
    /* 垂直居中 */
    cursor: pointer;
    transition: background-color 0.3s ease;
}

/* 鼠标悬浮时改变背景颜色和鼠标形状 */
.button1:hover {
    background-color: #66b1ff;
}

/* ===== AI 智能预填（2026-09 新增）===== */
.ai-prefill-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    background: linear-gradient(90deg, #ecf5ff, #f7fbff);
    border: 1px dashed #a0cfff;
    border-radius: 10px;
    padding: 12px 16px;
    margin: 2px 0 18px;
}

.ai-prefill-bar-text {
    flex: 1;
    font-size: 13px;
    color: #606266;
    line-height: 1.7;
}

.ai-prefill-bar-text strong {
    color: #409EFF;
    font-size: 14px;
}

/* 草稿对话框内「分类 + 价格」并排 */
.ai-draft-row {
    display: flex;
    column-gap: 24px;
}

.ai-draft-col {
    flex: 1;
}

.ai-draft-tip {
    color: #999999;
    font-size: 12px;
    margin-top: 2px;
}

/* AI 工具按钮组与合规结果 */
.ai-prefill-actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-shrink: 0;
}

.ai-compliance-result {
    border-radius: 8px;
    padding: 8px 12px;
    margin: 2px 0 14px;
    font-size: 13px;
    line-height: 1.6;
}

.ai-compliance-result.risk-high {
    background: #fef0f0;
    border: 1px solid #fde2e2;
    color: #b91c1c;
}

.ai-compliance-result.risk-medium {
    background: #fdf6ec;
    border: 1px solid #fae3c3;
    color: #b45309;
}

.ai-compliance-result.risk-low {
    background: #f0f9eb;
    border: 1px solid #e1f3d8;
    color: #277a3a;
}

.ai-draft-reason {
    background: #f7fbff;
    border-left: 3px solid #409eff;
    border-radius: 4px;
    color: #5470a6;
    font-size: 13px;
    line-height: 1.6;
    margin-bottom: 14px;
    padding: 8px 10px;
}

.ai-refine-bar {
    display: flex;
    align-items: flex-end;
    gap: 10px;
    margin-bottom: 8px;
}

.ai-refine-bar .el-button {
    height: 44px;
    flex-shrink: 0;
    margin-left: 0;
}
</style>
