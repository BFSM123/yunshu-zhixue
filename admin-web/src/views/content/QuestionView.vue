<template>
  <PageShell
    title="题库管理"
    description="题目审核、状态流转、Excel 导入导出与批量操作。"
    pagination
  >
    <template #actions>
      <el-button>Excel 导入</el-button>
      <el-button>导出</el-button>
      <el-button type="primary">新增题目</el-button>
    </template>

    <template #summary>
      <div class="stat-grid">
        <StatCard v-for="item in stats" :key="item" :label="item" />
      </div>
    </template>

    <template #filters>
      <el-form inline class="filter-form">
        <el-form-item label="关键词">
          <el-input placeholder="题目标题" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="学科">
          <el-select placeholder="全部学科" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="阶段">
          <el-select placeholder="全部阶段" clearable style="width: 150px" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select placeholder="全部题型" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select placeholder="全部难度" clearable style="width: 130px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select placeholder="全部状态" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary">查询</el-button>
          <el-button>重置</el-button>
        </el-form-item>
      </el-form>
    </template>

    <template #toolbar>
      <div class="table-toolbar__group">
        <el-button disabled>批量上架</el-button>
        <el-button disabled>批量下架</el-button>
        <el-button disabled>提交审核</el-button>
        <el-button disabled>批量删除</el-button>
      </div>
      <span class="table-toolbar__hint">已选 0 项</span>
    </template>

    <el-empty description="布局已就绪，待接入题库数据" />
  </PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/layout/PageShell.vue'
import StatCard from '@/components/layout/StatCard.vue'

const stats = ['总题数', '已上架', '待审核', '单选题', '多选题', '判断题', '简答题']
</script>
