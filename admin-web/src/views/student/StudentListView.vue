<template>
  <PageShell
    title="学员管理"
    description="学员筛选、启禁用、详情学习数据与面试记录。"
    pagination
  >
    <template #actions>
      <el-button type="primary">导出学员列表</el-button>
    </template>

    <template #filters>
      <el-form inline class="filter-form">
        <el-form-item label="昵称">
          <el-input placeholder="学员昵称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input placeholder="手机号" clearable style="width: 180px" />
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
        <el-button disabled>批量启用</el-button>
        <el-button disabled>批量禁用</el-button>
        <el-button disabled>批量删除</el-button>
      </div>
      <span class="table-toolbar__hint">已选 0 项</span>
    </template>

    <el-empty description="布局已就绪，待接入学员数据" />
  </PageShell>
</template>

<script setup lang="ts">
import PageShell from '@/components/layout/PageShell.vue'
</script>
