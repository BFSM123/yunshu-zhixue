<template>
  <div class="page-container">
    <h1 class="page-title">面试记录</h1>
    <p class="page-subtitle">历史面试列表与统计，报告仅在线查看。</p>
    <PlaceholderPage
      title="历史记录"
      description="面试次数、平均分、最高分、累计时长与记录列表的页面骨架。"
      :todos="[
        '统计卡 4 项',
        '记录列表：方向、难度、题量、综合评分、时长、完成时间',
        '点击进入报告详情'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
