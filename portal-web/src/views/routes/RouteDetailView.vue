<template>
  <div class="page-container">
    <h1 class="page-title">路线详情</h1>
    <p class="page-subtitle">当前路线 ID：{{ route.params.id }}</p>
    <PlaceholderPage
      title="路线课程"
      description="路线基本信息、课程列表与完成勾选的页面骨架。"
      :todos="[
        '路线信息卡：学科、适合学历、描述、总进度',
        '课程列表：顺序、名称、时长、来源、完成状态',
        '标记课程完成（幂等）并刷新进度'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import PlaceholderPage from '@/components/PlaceholderPage.vue'

const route = useRoute()
</script>
