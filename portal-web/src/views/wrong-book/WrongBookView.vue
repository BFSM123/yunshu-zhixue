<template>
  <div class="page-container">
    <h1 class="page-title">错题本</h1>
    <p class="page-subtitle">按错误次数分级，连续答对 3 次自动标记已掌握。</p>
    <PlaceholderPage
      title="错题复习"
      description="错题统计、筛选、重做与掌握标记的页面骨架。"
      :todos="[
        '统计卡：总数 / 待复习 / 已掌握 / 掌握率 + 高频错题 Top3',
        '按状态、题型、时间范围筛选与排序',
        '错题重做（复用组卷流程）',
        '标记已掌握 / 移除错题'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
