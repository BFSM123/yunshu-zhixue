<template>
  <div class="page-container">
    <PageHeader title="数据看板" description="平台核心指标、用户增长、题目分布与最近活动。" />

    <div class="stat-grid">
      <StatCard v-for="item in stats" :key="item.label" :label="item.label" trend="环比 —" />
    </div>

    <el-row :gutter="16" class="section-gap">
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>用户增长趋势</template>
          <el-empty description="布局已就绪，待接入用户增长数据" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="8" class="chart-col">
        <el-card shadow="never">
          <template #header>题目学科分布</template>
          <el-empty description="布局已就绪，待接入题目分布数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section-gap">
      <template #header>最近活动</template>
      <el-empty description="布局已就绪，待接入最近活动数据" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from '@/components/layout/PageHeader.vue'
import StatCard from '@/components/layout/StatCard.vue'

const stats = [
  { label: '学科数量' },
  { label: '题目数量' },
  { label: '注册用户' },
  { label: 'AI 面试次数' }
]
</script>

<style scoped lang="scss">
@media (max-width: 1200px) {
  .chart-col {
    margin-top: 16px;
  }
}
</style>
