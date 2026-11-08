<template>
  <div class="not-found">
    <el-result icon="warning" title="页面不存在" sub-title="你访问的页面可能已被移动或删除">
      <template #extra>
        <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
  padding: 80px 20px;
}
</style>
