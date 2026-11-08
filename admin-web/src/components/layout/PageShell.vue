<template>
  <div class="page-container">
    <PageHeader :title="title" :description="description">
      <template v-if="$slots.actions" #actions>
        <slot name="actions" />
      </template>
    </PageHeader>

    <div v-if="$slots.summary" class="page-summary">
      <slot name="summary" />
    </div>

    <el-card v-if="$slots.filters" shadow="never" class="filter-card">
      <slot name="filters" />
    </el-card>

    <el-card shadow="never" class="table-card">
      <div v-if="$slots.toolbar" class="table-toolbar">
        <slot name="toolbar" />
      </div>
      <slot />
      <div v-if="$slots.pagination" class="table-pagination">
        <slot name="pagination" />
      </div>
      <div v-else-if="pagination" class="table-pagination">
        <el-pagination layout="total, prev, pager, next" :total="0" disabled />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import PageHeader from './PageHeader.vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    pagination?: boolean
  }>(),
  {
    description: '',
    pagination: false
  }
)
</script>

<style scoped lang="scss">
.page-summary {
  margin-bottom: 16px;
}
</style>
