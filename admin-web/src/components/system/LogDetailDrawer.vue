<template>
  <el-drawer
    :model-value="modelValue"
    title="日志详情"
    size="460px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="log-detail">
      <el-descriptions v-if="log" :column="1" border>
        <el-descriptions-item label="日志 ID">{{ log.id }}</el-descriptions-item>
        <el-descriptions-item label="操作用户">{{ log.username }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="actionType(log.action)" size="small" effect="plain">
            {{ log.action }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作对象">
          {{ log.targetType }} · {{ log.targetName }}
        </el-descriptions-item>
        <el-descriptions-item label="IP 地址">{{ log.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ log.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { OperationLogVO } from '@/types/system'
import { getLogDetail } from '@/api/log'

const props = defineProps<{
  modelValue: boolean
  logId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const log = ref<OperationLogVO | null>(null)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.logId) return
    loading.value = true
    log.value = null
    try {
      log.value = await getLogDetail(props.logId)
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  }
)

function actionType(action: string): 'success' | 'warning' | 'danger' | 'info' {
  if (['新增'].includes(action)) return 'success'
  if (['修改', '禁用启用'].includes(action)) return 'warning'
  if (['删除'].includes(action)) return 'danger'
  return 'info'
}
</script>

<style scoped lang="scss">
.log-detail {
  min-height: 200px;
}
</style>
