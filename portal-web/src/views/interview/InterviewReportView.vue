<template>
  <div class="page-container">
    <h1 class="page-title">面试报告</h1>
    <p class="page-subtitle">当前报告 ID：{{ route.params.id }}</p>
    <PlaceholderPage
      title="能力评估"
      description="六维雷达图、环形评分、优缺点与提升卡的页面骨架。"
      :todos="[
        '综合评分环与相比上次的趋势',
        '六维能力雷达图（ECharts）',
        '优势 / 不足 / 改进建议',
        '逐题回放：题干、作答、得分与评语'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import PlaceholderPage from '@/components/PlaceholderPage.vue'

const route = useRoute()
</script>
