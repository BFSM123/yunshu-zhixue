<template>
  <div class="page-container">
    <h1 class="page-title">AI 模拟面试</h1>
    <p class="page-subtitle">选择方向、难度与题量，每日 5 次配额，次日 0 点重置。</p>
    <PlaceholderPage
      title="面试配置"
      description="面试方向、难度、题量与剩余配额的页面骨架。"
      :todos="[
        '8 个方向卡片单选（默认 AI 应用与智能体开发）',
        '难度与题量选择，展示今日剩余次数',
        '发起面试并跳转面试进行页',
        '面试记录入口'
      ]"
    >
      <div class="links">
        <el-button @click="$router.push('/interview/records')">面试记录</el-button>
        <el-button type="primary" @click="$router.push('/interview/session')">进入面试页</el-button>
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
