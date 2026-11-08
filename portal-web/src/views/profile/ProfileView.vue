<template>
  <div class="page-container">
    <h1 class="page-title">个人中心</h1>
    <p class="page-subtitle">资料维护、学习数据与账号安全。</p>
    <PlaceholderPage
      title="账号与学习数据"
      description="基本信息、学习统计与安全设置的页面骨架。"
      :todos="[
        '基本信息：昵称、邮箱、性别、技术方向、头像',
        '学习数据：刷题数、面试数、错题数、学习天数、正确率',
        '修改密码（成功后强制重新登录）',
        '完成新手引导标记'
      ]"
    />
  </div>
</template>

<script setup lang="ts">
import PlaceholderPage from '@/components/PlaceholderPage.vue'
</script>
