<template>
  <div class="page-container">
    <PageHeader title="学习路线" description="路线卡片、课程排序、跨学科补充与发布状态。">
      <template #actions>
        <el-button type="primary">新建学习路线</el-button>
      </template>
    </PageHeader>

    <el-card shadow="never">
      <el-empty description="布局已就绪，待接入学习路线数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/layout/PageHeader.vue'
</script>
