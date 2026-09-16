<template>
  <div class="main-border">
    <div>
      <el-table :data="tableData" border stripe style="width: 100%;margin: 2px auto">
        <el-table-column label="图片">
          <template #default="scope">
            <!-- 解析图片列表字符串为数组 -->
            <div class="image-container">
              <img :src="rowImg(scope.row)" alt="Image" class="full-image" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="商品">
          <template #default="scope">
            {{ scope.row.goodName }}
          </template>
        </el-table-column>
        <el-table-column prop="showOrder" label="轮播顺序" width="110"></el-table-column>

        <el-table-column fixed="right" label="操作" width="300">
          <template #default="scope">
            <div style="display: flex; align-items: center;justify-content: center;">
              <div class="button1" type="primary" @click="edit(scope.row)">编辑</div>
              <el-popconfirm @confirm="del(scope.row.id)" title="确定删除？">
                <template #reference>
                  <div class="button3" type="danger" style="margin-left: 10px">删除</div>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 新增按钮 -->
    <div style="text-align: center; display: flex;
    justify-content: center; /* 水平居中 */">
      <div class="button1" @click="add" type="primary" style="margin: 30px;">
        新增
      </div>
    </div>
    <!-- 编辑弹窗 -->
    <el-dialog title="编辑信息" v-model="dialogFormVisible1" width="30%" :close-on-click-modal="false">
      <el-form :model="entity">
        <el-form-item label="商品" label-width="150px">
          <el-select v-model="entity.goodId" filterable placeholder="请选择要展示的（在售）商品"
            style="width: 80%">
            <el-option v-for="g in goodsOptions" :key="g.id" :value="g.id"
              :label="g.idleName + '（ID: ' + g.id + '）'">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="轮播顺序" label-width="150px">
          <el-input-number v-model="entity.showOrder" :min="1" :max="99" controls-position="right"
            style="width: 200px"></el-input-number>
          <div class="form-hint">数字越小越靠前，保存后按该值排序</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <div class="button2" type="primary" @click="save2">确 定</div>
          <div class="button1" @click="dialogFormVisible1 = false">取 消</div>
        </div>
      </template>
    </el-dialog>

    <!-- 新增弹窗 -->
    <el-dialog title="添加信息" v-model="dialogFormVisible" width="30%" :close-on-click-modal="false">
      <el-form :model="entity">
        <el-form-item label="商品" label-width="150px">
          <el-select v-model="entity.goodId" filterable placeholder="请选择要展示的（在售）商品"
            style="width: 80%">
            <el-option v-for="g in goodsOptions" :key="g.id" :value="g.id"
              :label="g.idleName + '（ID: ' + g.id + '）'">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="轮播顺序" label-width="150px">
          <el-input-number v-model="entity.showOrder" :min="1" :max="99" controls-position="right"
            style="width: 200px"></el-input-number>
          <div class="form-hint">数字越小越靠前，保存后按该值排序</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <div class="button2" type="primary" @click="save1">确 定</div>
          <div class="button1" @click="dialogFormVisible = false">取 消</div>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * 1. 原版用裸 axios 直连 http://localhost:9321/admin/carouselList、/carousel/add、
 *    /carousel/delete/{id}（无任何鉴权头，后端现在要求 admin JWT）→ 全部改走
 *    api.getCarouselList / addCarousel / updateCarousel / deleteCarousel（adminRequest）
 * 2. 图片地址 $store.state.baseApi + img → imageUrl()（兼容旧数据 "/image?imageName=xxx"）
 * 3. slot-scope/slot="reference"/:visible.sync/slot="footer" → #default/#reference/v-model/#footer
 * 4. 删除原版 add() 里 this.tableData.length++ 的脏写法（向表格数据塞 undefined），
 *    轮播顺序选项改为 computed orderOptionCount
 */
import api from '@/api'
import { imageUrl } from '@/utils/request'

// 兼容旧数据：历史 img 里存的是 "/image?imageName=xxx"，统一剥成纯文件名
const toImageName = (entry) => {
    if (!entry) return ''
    return entry.includes('imageName=') ? entry.split('imageName=')[1] : entry
}

export default {
  name: "Carousel",
  data() {
    return {
      tableData: [],
      entity: {},
      // 可选的（在售）商品列表，用于新增/编辑轮播时按名称选择，而不是手输 id
      goodsOptions: [],
      dialogFormVisible: false,
      dialogFormVisible1: false
    };
  },
  created() {
    this.load();
    this.loadGoods();
  },
  methods: {
    // 表格行首图地址（后端 /admin/carouselList 图片字段为 imgs=商品 picture_list，
    // 兼容旧数据的 img 字段）
    rowImg(row) {
      let raw = '';
      if (Array.isArray(row.imgs) && row.imgs.length > 0) {
        raw = row.imgs[0];
      } else {
        raw = row.imgs || row.img || '';
      }
      try {
        const list = JSON.parse(raw);
        return list.length > 0 ? imageUrl(toImageName(list[0])) : '';
      } catch (e) {
        return raw ? imageUrl(toImageName(raw)) : '';
      }
    },
    load() {
      api.getCarouselList().then(res => {
        if (res.status_code === 1) {
          this.tableData = res.data || [];
        } else {
          this.$message.error(res.msg || '轮播图加载失败');
        }
      }).catch(() => {
        this.$message.error('轮播图加载失败');
      });
    },
    // 加载可供轮播选择的在售商品（下拉框用），按名称可搜索
    loadGoods() {
      api.getGoods({ status: 1, page: 1, nums: 1000 }).then(res => {
        if (res.status_code === 1) {
          this.goodsOptions = res.data.list || [];
        }
      }).catch(() => {
        this.goodsOptions = [];
      });
    },
    // 新增时默认排到最后
    defaultOrder() {
      let max = 0;
      (this.tableData || []).forEach(row => {
        const n = Number(row.showOrder);
        if (!isNaN(n) && n > max) max = n;
      });
      return max + 1;
    },
    add() {
      this.entity = { showOrder: this.defaultOrder() };
      this.dialogFormVisible = true;
    },
    edit(row) {
      this.entity = JSON.parse(JSON.stringify(row));
      this.dialogFormVisible1 = true;
    },
    save1() {
      if (this.entity.goodId == undefined || this.entity.goodId === "") {
        this.$message.error("请选择商品");
        return;
      }
      if (this.entity.showOrder == undefined) {
        this.$message.error("轮播顺序不能为空");
        return;
      }

      api.addCarousel(this.entity)
        .then(res => {
          if (res.status_code == 1) {
            this.$message.success("保存成功");
            this.load();
            this.dialogFormVisible = false;
          } else {
            // 商品不存在或保存失败
            this.$message.error(res.msg || '不存在该商品');
          }
        })
        .catch(() => {
          this.$message.error('不存在该商品');
        });
    },
    save2() {
      if (this.entity.goodId == undefined || this.entity.goodId === "") {
        this.$message.error("请选择商品");
        return;
      }
      if (this.entity.showOrder == undefined) {
        this.$message.error("轮播顺序不能为空");
        return;
      }

      api.updateCarousel(this.entity)
        .then(res => {
          if (res.status_code == 1) {
            this.$message.success("更新成功");
            this.load(); // 重新加载数据
            this.dialogFormVisible1 = false; // 关闭对话框
          } else {
            // 商品不存在或更新失败
            this.$message.error(res.msg || '不存在该商品或更新失败');
          }
        })
        .catch(() => {
          this.$message.error('更新失败');
        });
    },
    del(id) {
      api.deleteCarousel(id)
        .then(res => {
          if (res.status_code === 1) {
            this.$message({
              type: "success",
              message: "删除成功",
            });
            this.load();
          } else {
            this.$message.error(res.msg || '删除失败');
          }
        })
        .catch(() => {
          this.$message.error('删除失败');
        });
    }
  },
};
</script>

<style scoped>
.main-border {
  background-color: #FFF;
  padding: 10px 30px;
  box-shadow: 0 1px 15px -6px rgba(0, 0, 0, .5);
  border-radius: 5px;
}

.form-hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.8;
  margin-top: 4px;
}

.image-container {
  width: 300px;
  height: 185px;
  overflow: hidden;
  background-size: cover;
}

.full-image {
  width: 100%;
  height: auto;
  object-fit: cover;
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
