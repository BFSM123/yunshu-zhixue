<template>
  <div class="page-container">
    <h1 class="page-title">学习路线</h1>
    <p class="page-subtitle">按学科查看学习路线与课程顺序，勾选记录完成进度。</p>
    <PlaceholderPage
      title="路线列表"
      description="路线卡片与学科分组的页面骨架。"
      :todos="[
        '按学科分组展示路线卡片与进度',
        '路线详情：课程顺序、时长、外链跳转',
        '课程完成勾选与进度重算'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
