<template>
  <el-drawer
    :model-value="modelValue"
    title="学员详情"
    size="620px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="student-detail">
      <template v-if="detail">
        <div class="profile">
          <el-avatar :size="64" :src="detail.avatar || undefined" class="profile__avatar">
            {{ detail.nickname.slice(0, 1) }}
          </el-avatar>
          <div class="profile__info">
            <div class="profile__name">
              {{ detail.nickname }}
              <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small">
                {{ detail.status === 1 ? '已启用' : '已禁用' }}
              </el-tag>
            </div>
            <div class="profile__meta">手机号 {{ detail.mobile }}</div>
          </div>
        </div>

        <el-descriptions :column="2" border class="profile-desc">
          <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">
            {{ detail.createdAt || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录">
            {{ detail.lastLoginAt || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="累计面试">
            {{ detail.interviewCount }} 次
          </el-descriptions-item>
        </el-descriptions>

        <h4 class="section-title">学习统计</h4>
        <div class="stat-grid">
          <div v-for="item in statItems" :key="item.label" class="stat-item">
            <span class="stat-item__value">{{ item.value }}</span>
            <span class="stat-item__label">{{ item.label }}</span>
          </div>
        </div>

        <h4 class="section-title">面试记录</h4>
        <el-table v-loading="interviewLoading" :data="interviews" size="small">
          <el-table-column label="面试方向" min-width="120">
            <template #default="{ row }">{{ row.subjectName || '—' }}</template>
          </el-table-column>
          <el-table-column label="难度" width="70">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ difficultyLabel(row.difficulty) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="questionCount" label="题数" width="60" />
          <el-table-column label="综合评分" width="90">
            <template #default="{ row }">
              <span :class="{ 'score-high': row.overallScore >= 8 }">
                {{ row.overallScore ?? '—' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="面试时间" min-width="150">
            <template #default="{ row }">
              <span class="muted">{{ row.completedAt || '—' }}</span>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无面试记录" :image-size="60" />
          </template>
        </el-table>

        <div v-if="interviewTotal > interviewPageSize" class="interview-pagination">
          <el-pagination
            v-model:current-page="interviewPage"
            :page-size="interviewPageSize"
            :total="interviewTotal"
            layout="prev, pager, next"
            small
            @current-change="loadInterviews"
          />
        </div>
      </template>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { StudentDetail, StudentInterview } from '@/types/student'
import { getStudentDetail, getStudentInterviews } from '@/api/student'

const props = defineProps<{
  modelValue: boolean
  studentId: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const difficultyLabels: Record<number, string> = { 1: '简单', 2: '中等', 3: '困难' }

const loading = ref(false)
const detail = ref<StudentDetail | null>(null)

const interviewLoading = ref(false)
const interviews = ref<StudentInterview[]>([])
const interviewTotal = ref(0)
const interviewPage = ref(1)
const interviewPageSize = 5

const statItems = computed(() => {
  if (!detail.value) return []
  const stats = detail.value.stats
  return [
    { label: '学习天数', value: `${stats.studyDays} 天` },
    { label: '已学课程', value: `${stats.completedCourseCount} 门` },
    { label: '刷题总数', value: `${stats.practiceCount} 题` },
    { label: '正确率', value: `${stats.accuracyRate.toFixed(1)}%` }
  ]
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.studentId) return
    detail.value = null
    interviews.value = []
    interviewTotal.value = 0
    interviewPage.value = 1
    loading.value = true
    try {
      detail.value = await getStudentDetail(props.studentId)
      await loadInterviews()
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  }
)

async function loadInterviews() {
  if (!props.studentId) return
  interviewLoading.value = true
  try {
    const result = await getStudentInterviews(
      props.studentId,
      interviewPage.value,
      interviewPageSize
    )
    interviews.value = result.list
    interviewTotal.value = result.total
  } catch {
    interviews.value = []
    interviewTotal.value = 0
  } finally {
    interviewLoading.value = false
  }
}

function difficultyLabel(value: number) {
  return difficultyLabels[value] || '—'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.student-detail {
  min-height: 260px;
}

.profile {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.profile__avatar {
  color: #fff;
  font-size: 24px;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.profile__name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 17px;
  font-weight: 600;
}

.profile__meta {
  margin-top: 6px;
  color: $yszx-text-secondary;
  font-size: 13px;
}

.section-title {
  margin: 24px 0 14px;
  font-size: 14px;
  font-weight: 600;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 12px;
  background: $yszx-bg;
  border: 1px solid $yszx-border;
  border-radius: 8px;
}

.stat-item__value {
  font-size: 18px;
  font-weight: 600;
}

.stat-item__label {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.score-high {
  color: #16a34a;
  font-weight: 600;
}

.muted {
  color: $yszx-text-secondary;
}

.interview-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

@media (max-width: 640px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>