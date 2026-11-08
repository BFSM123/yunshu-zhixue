<template>
  <el-drawer
    :model-value="modelValue"
    title="用户详情"
    size="520px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="user-detail">
      <template v-if="detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">
            {{ detail.user.username }}
            <el-tag v-if="detail.user.isBuiltin" size="small" type="warning" effect="plain">
              内置
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            {{ detail.user.roleName || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.user.status === 1 ? 'success' : 'info'" size="small">
              {{ detail.user.status === 1 ? '已启用' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ detail.user.mobile || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱" :span="2">
            {{ detail.user.email || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ detail.user.createdAt || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            {{ detail.user.lastLoginAt || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">最近操作</h4>
        <el-timeline v-if="detail.recentOperations.length" class="operation-timeline">
          <el-timeline-item
            v-for="item in detail.recentOperations"
            :key="item.id"
            :timestamp="item.createdAt"
            placement="top"
          >
            <div class="operation-line">
              <el-tag size="small" effect="plain">{{ item.action }}</el-tag>
              <span>{{ item.targetName }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无操作记录" :image-size="62" />
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { UserDetail } from '@/types/system'
import { getUserDetail } from '@/api/user'

const props = defineProps<{
  modelValue: boolean
  userId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const loading = ref(false)
const detail = ref<UserDetail | null>(null)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.userId) return
    loading.value = true
    detail.value = null
    try {
      detail.value = await getUserDetail(props.userId)
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  }
)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.user-detail {
  min-height: 240px;
}

.section-title {
  margin: 26px 0 16px;
  font-size: 14px;
  font-weight: 600;
}

.operation-timeline {
  padding-left: 4px;
}

.operation-line {
  display: flex;
  align-items: center;
  gap: 8px;
  color: $yszx-text-secondary;
  font-size: 13px;
}
</style>
