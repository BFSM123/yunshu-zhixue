<template>
  <div class="error-page">
    <el-result icon="warning" title="无权限访问" sub-title="当前账号没有该功能权限，请联系超级管理员">
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
  min-height: 70vh;
}
</style>
