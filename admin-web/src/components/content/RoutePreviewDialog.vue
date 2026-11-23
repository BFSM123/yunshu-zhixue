<template>
  <el-dialog
    :model-value="modelValue"
    title="学员视角预览"
    width="720px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="route-preview">
      <template v-if="detail">
        <div class="preview-header">
          <div class="preview-icon">{{ detail.icon || '📍' }}</div>
          <div class="preview-title-wrap">
            <h2 class="preview-title">{{ detail.name }}</h2>
            <p v-if="detail.description" class="preview-desc">{{ detail.description }}</p>
          </div>
        </div>

        <div class="preview-meta">
          <span>{{ detail.subjectName }}</span>
          <span v-if="detail.targetEducation">适合 {{ detail.targetEducation }}</span>
          <span>共 {{ detail.courseCount }} 门课程</span>
          <span>总计时长 {{ formatDuration(detail.totalDurationMinutes) }}</span>
          <span>按列表顺序学习</span>
        </div>

        <div v-if="detail.courses.length" class="preview-courses">
          <div
            v-for="(course, index) in detail.courses"
            :key="course.routeCourseId"
            class="preview-course"
          >
            <span class="preview-course__index">{{ index + 1 }}</span>
            <el-image
              v-if="course.coverUrl"
              :src="course.coverUrl"
              fit="cover"
              class="preview-course__cover"
            >
              <template #error>
                <div class="preview-course__cover preview-course__cover--empty">失效</div>
              </template>
            </el-image>
            <div v-else class="preview-course__cover preview-course__cover--empty">
              <el-icon><VideoCamera /></el-icon>
            </div>
            <div class="preview-course__body">
              <div class="preview-course__name">{{ course.name }}</div>
              <div class="preview-course__meta">
                <template v-if="course.bvid">{{ course.bvid }} · </template>
                {{ formatDuration(course.durationMinutes) }}
                <template v-if="course.otherSubject"> · {{ course.subjectName }}</template>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="课程筹备中，敬请期待" :image-size="80" />
      </template>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { VideoCamera } from '@element-plus/icons-vue'
import { getRouteDetail } from '@/api/route'
import type { RouteDetail } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  routeId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
}>()

const loading = ref(false)
const detail = ref<RouteDetail | null>(null)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.routeId) return
    detail.value = null
    loading.value = true
    try {
      detail.value = await getRouteDetail(props.routeId)
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  }
)

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} 分钟`
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.route-preview {
  min-height: 180px;
}

.preview-header {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.preview-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  font-size: 24px;
  background: #eff6ff;
  border-radius: 12px;
}

.preview-title-wrap {
  min-width: 0;
}

.preview-title {
  margin: 0;
  color: $yszx-text;
  font-size: 20px;
  font-weight: 700;
}

.preview-desc {
  margin: 6px 0 0;
  color: $yszx-text-secondary;
  font-size: 13px;
  line-height: 1.7;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;

  span {
    padding: 3px 10px;
    color: $yszx-text-secondary;
    font-size: 12px;
    background: #f1f5f9;
    border-radius: 999px;
  }
}

.preview-course {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-top: 1px solid $yszx-border;
}

.preview-course__index {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  color: $yszx-primary;
  font-size: 12px;
  font-weight: 600;
  background: #eff6ff;
  border-radius: 50%;
}

.preview-course__cover {
  flex-shrink: 0;
  width: 88px;
  height: 50px;
  border-radius: 6px;
}

.preview-course__cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $yszx-text-secondary;
  font-size: 16px;
  background: #f1f5f9;
}

.preview-course__body {
  min-width: 0;
}

.preview-course__name {
  overflow: hidden;
  color: $yszx-text;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-course__meta {
  margin-top: 3px;
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>
