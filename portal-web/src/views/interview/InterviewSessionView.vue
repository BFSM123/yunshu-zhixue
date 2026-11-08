<template>
  <div class="page-container">
    <h1 class="page-title">面试进行中</h1>
    <p class="page-subtitle">逐题作答，SSE 流式评分，未完成会话仅保存在本地。</p>
    <PlaceholderPage
      title="面试会话"
      description="对话区、题目侧栏、进度面板与语音能力的页面骨架。"
      :todos="[
        'AI / 学员消息气泡与打字机效果（SSE）',
        '分析中 → 评分中 → 思考中阶段提示',
        '浏览器语音识别与语音播报（不支持时降级提示）',
        '退出二次确认、结束面试并生成报告'
      ]"
    >
      <div class="links">
        <el-button @click="$router.push('/interview/reports/demo')">查看报告占位页</el-button>
      </div>
    </PlaceholderPage>
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>

<style scoped>
.links {
  margin-top: 20px;
}
</style>
