<template>
  <div class="page-header">
    <div class="page-header__text">
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__desc">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.page-header__title {
  color: $yszx-text;
  font-size: 22px;
  font-weight: 700;
}

.page-header__desc {
  margin: 8px 0 0;
  color: $yszx-text-secondary;
  font-size: 14px;
  line-height: 1.6;
}

.page-header__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}
</style>
