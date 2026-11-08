<template>
  <div class="page-container">
    <h1 class="page-title">刷题练习</h1>
    <p class="page-subtitle">顺序或随机组卷，客观题本地判分，简答题由 AI 批改。</p>
    <PlaceholderPage
      title="练习会话"
      description="练习进度仅保存在浏览器本地（断点续练），已完成作答才落库。"
      :todos="[
        '开始练习：来源、学科、阶段、题型、难度、题量、模式',
        '答题卡与题目切换',
        '提交作答并展示判分结果与解析',
        '简答题 AI 批改（SSE 流式）'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
