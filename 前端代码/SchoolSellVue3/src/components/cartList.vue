<template>
  <div class="main-border">
    <!-- 弹出编辑框 -->
    <el-dialog title="编辑分类" v-model="editDialogVisible" width="30%" :close-on-click-modal="false">
      <el-form :model="form1" label-width="80px">
        <el-form-item label="分类名称">
          <el-input v-model="form1.name"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <div class="button2" @click="saveEdit">保存</div>
          <div class="button1" @click="cancelEdit">取消</div>
        </div>
      </template>
    </el-dialog>

    <!-- 统一搜索栏 -->
    <admin-search-bar v-model="searchText" placeholder="输入分类名称搜索" @search="search" />

    <!-- 分类表格 -->
    <el-table :data="categoryList" style="width: 100%">
      <el-table-column prop="name" label="分类名称" show-overflow-tooltip></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <div style="display: flex; align-items: center;justify-content: center;">
            <div class="button1" @click="deleteCategory(scope.row.id)">删除分类</div>
            <div class="button2" @click="showEditDialog(scope.row)">编辑分类</div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加分类表单 -->
    <el-form ref="form" :model="form" label-width="80px">
      <el-form-item label="分类名称">
        <el-input v-model="form.name"></el-input>
      </el-form-item>
      <el-form-item>
        <div class="button-container-center">
          <div class="button1" @click="addCategory" style="margin-top: 15px;float: right;">添加分类</div>
        </div>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
/**
 * 迁移要点（Vue2 → Vue3）：
 * - 该组件实为“商品分类管理”，修正原文件误写的 name: "orderList" → "cartList"
 * - this.$api → import api from '@/api'；分类写操作（addType/editType/deleteType）
 *   已改为 adminRequest（管理端接口），token 取 admin_token
 * - :visible.sync → v-model、slot="footer" → #footer、slot-scope → #default
 * - 删除无规则的 $refs.form.validate 空转（表单无 rules，校验恒通过，改为直接判空）
 * - 删除 console.log
 */
import api from '@/api'

export default {
  name: "cartList",
  data() {
    return {
      categoryList: [], // 用于存储分类列表
      _categoryList: [], // 全量分类（表格展示的是它过滤后的结果）
      searchText: '', // 搜索关键词
      form: {
        name: '' // 新分类的名称
      },
      form1: {
        name: '' // 编辑分类的名称
      },
      editDialogVisible: false, // 控制编辑框的显示与隐藏
      editingCategory: null // 当前正在编辑的分类对象
    };
  },
  created() {
    this.listType();
  },
  methods: {
    // 搜索方法（从全量原始列表过滤，而非在已过滤结果上继续过滤）
    search() {
      const kw = this.searchText.trim().toLowerCase();
      this.categoryList = this._categoryList.filter(item =>
        !kw || String(item.name || '').toLowerCase().includes(kw)
      );
    },
    listType() {
      api.listType({ begin: 0, size: 999 }).then(res => {
        if (res.status_code == 1) {
          this._categoryList = res.data;
          this.search();
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
      });
    },
    addCategory() {
      if (!this.form.name.trim()) {
        this.$message.error('分类名称不能为空');
        return;
      }
      api.addType(this.form).then(res => {
        if (res.status_code == 1) {
          this.$message.success('分类添加成功');
          this.form.name = '';
          this.listType();
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
      });
    },
    showEditDialog(category) {
      // 设置当前正在编辑的分类对象
      this.editingCategory = category;
      // 将分类名称填充到编辑表单中
      this.form1.name = category.name;
      // 显示编辑框
      this.editDialogVisible = true;
    },
    saveEdit() {
      // 执行保存逻辑
      api.editType({ id: this.editingCategory.id, name: this.form1.name }).then(res => {
        if (res.status_code == 1) {
          this.$message.success('分类编辑成功');
          this.listType(); // 重新加载分类列表
          this.cancelEdit(); // 取消编辑状态
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
      });
    },
    cancelEdit() {
      // 取消编辑，清空表单数据并隐藏编辑框
      this.form1.name = ''; // 清空表单数据
      this.editDialogVisible = false; // 隐藏编辑框
    },
    deleteCategory(id) {
      api.deleteType(id).then(res => {
        if (res.status_code == 1) {
          this.$message.success('分类删除成功');
          this.listType();
        } else {
          this.$message.error(res.msg);
        }
      }).catch(() => {
      });
    }
  }
};
</script>

<style scoped>
.search-bar {
  width: 700px;
  height: 50px;
}
.button1,
.button2 {
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

.button1:hover,
.button2:hover {
  opacity: 0.85;
}

.dialog-footer {
    display: flex;
    justify-content: space-between; /* 水平间距平均分布 */
    margin-top: 20px; /* 为了和上面的表单区域有一定的间距 */
}
.button-container-center {
    /* display: flex;
    justify-content: center; */
    margin-top: 15px;
}
.main-border {
    background-color: #FFF;
    padding: 10px 30px;
    box-shadow: 0 1px 15px -6px rgba(0, 0, 0, .5);
    border-radius: 5px;
}
</style>
