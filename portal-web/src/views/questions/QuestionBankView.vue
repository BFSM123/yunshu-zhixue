<template>
  <div class="page-container">
    <h1 class="page-title">题库中心</h1>
    <p class="page-subtitle">学科 → 阶段 → 题目三级浏览，游客可查看题干，解析与答案需登录。</p>
    <PlaceholderPage
      title="题库浏览"
      description="页面骨架已就绪，等待接入题库接口。"
      :todos="[
        '左侧学科列表与阶段列表',
        '题目列表：题型/难度筛选、关键词搜索、分页',
        '题目详情：登录后展示答案与解析',
        '卡片上的答题次数与正确率'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
