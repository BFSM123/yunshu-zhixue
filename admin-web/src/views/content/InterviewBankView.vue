<template>
  <PageShell
    title="面试题库"
    description="文档解析、手工录入、md 导入、主题维护与检索测试。"
    pagination
  >
    <template #actions>
      <el-button>上传 Markdown</el-button>
      <el-button>手工录入</el-button>
      <el-button type="primary">导入题目</el-button>
    </template>

    <template #filters>
      <el-form inline class="filter-form">
        <el-form-item label="关键词">
          <el-input placeholder="题目内容" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item label="方向">
          <el-select placeholder="全部方向" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="主题">
          <el-select placeholder="全部主题" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="难度">
          <el-select placeholder="全部难度" clearable style="width: 130px" />
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
        <el-button disabled>批量删除</el-button>
      </div>
      <span class="table-toolbar__hint">已选 0 项</span>
    </template>

    <el-empty description="布局已就绪，待接入面试题库数据" />
  </PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/layout/PageShell.vue'
</script>
