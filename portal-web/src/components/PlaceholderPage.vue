<template>
  <div class="placeholder-page">
    <el-card shadow="never">
      <template #header>
        <div class="ph-header">
          <h2>{{ title }}</h2>
          <el-tag type="info" effect="plain">基础工程占位</el-tag>
        </div>
      </template>
      <p class="ph-desc">{{ description }}</p>
      <template v-if="todos.length">
        <el-divider />
        <h4>待实现内容</h4>
        <ul>
          <li v-for="item in todos" :key="item">{{ item }}</li>
        </ul>
      </template>
      <slot />
    </el-card>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
    todos?: string[]
  }>(),
  {
    description: '',
    todos: () => []
  }
)
</script>
