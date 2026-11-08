<template>
  <div class="error-page">
    <el-result icon="info" title="页面不存在" sub-title="你访问的后台页面可能已被移动或删除">
      <template #extra>
        <el-button type="primary" @click="$router.replace('/dashboard')">返回看板</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
</style>
