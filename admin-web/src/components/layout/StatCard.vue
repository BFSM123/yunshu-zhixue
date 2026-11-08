<template>
  <div class="stat-card">
    <div class="stat-card__label">{{ label }}</div>
    <div class="stat-card__value">{{ value }}</div>
    <div v-if="trend" class="stat-card__trend">{{ trend }}</div>
  </div>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value?: string
    trend?: string
  }>(),
  {
    value: '—',
    trend: ''
  }
)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.stat-card {
  padding: 20px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 12px;
}

.stat-card__label {
  color: $yszx-text-secondary;
  font-size: 13px;
}

.stat-card__value {
  margin: 10px 0 6px;
  font-size: 28px;
  font-weight: 700;
}

.stat-card__trend {
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>
