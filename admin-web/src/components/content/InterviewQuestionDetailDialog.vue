<template>
  <el-dialog
    :model-value="modelValue"
    title="面试题详情"
    width="760px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="detail-body">
      <template v-if="question">
        <section class="detail-section">
          <h4 class="detail-section__title">题干</h4>
          <p class="detail-content">{{ question.content }}</p>
        </section>

        <section class="detail-section">
          <h4 class="detail-section__title">参考答案</h4>
          <p v-if="question.referenceAnswer" class="detail-content detail-content--answer">
            {{ question.referenceAnswer }}
          </p>
          <p v-else class="detail-empty">未填写参考答案</p>
        </section>

        <section class="detail-section">
          <h4 class="detail-section__title">题目信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-item__label">所属学科</span>
              <span class="detail-item__value">{{ question.subjectName || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">主题</span>
              <span class="detail-item__value">{{ question.topic || '未分类' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">难度</span>
              <el-tag :type="interviewDifficultyTagType(question.difficulty)" size="small" effect="plain">
                {{ interviewDifficultyLabel(question.difficulty) }}
              </el-tag>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">状态</span>
              <el-tag :type="interviewStatusTagType(question.status)" size="small" effect="plain">
                {{ interviewStatusLabel(question.status) }}
              </el-tag>
            </div>
            <div class="detail-item detail-item--wide">
              <span class="detail-item__label">来源</span>
              <span class="detail-item__value">{{ interviewSourceText(question) }}</span>
            </div>
            <div class="detail-item detail-item--wide">
              <span class="detail-item__label">标签</span>
              <span class="detail-item__value">{{ question.tags || '—' }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">被面试引用</span>
              <span class="detail-item__value">{{ question.referencedCount }} 场</span>
            </div>
            <div class="detail-item">
              <span class="detail-item__label">创建时间</span>
              <span class="detail-item__value">{{ formatTime(question.createdAt) }}</span>
            </div>
          </div>
        </section>
      </template>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
      <el-button
        v-if="question"
        v-permission="'interviewbank:edit'"
        type="primary"
        @click="emit('edit', question)"
      >
        编辑主题/难度
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { InterviewQuestionDetail } from '@/types/interview'
import {
  interviewDifficultyLabel,
  interviewDifficultyTagType,
  interviewSourceText,
  interviewStatusLabel,
  interviewStatusTagType
} from '@/utils/interview'

defineProps<{
  modelValue: boolean
  question: InterviewQuestionDetail | null
  loading: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  edit: [InterviewQuestionDetail]
}>()

function formatTime(value: string | null | undefined): string {
  return value ? value.replace('T', ' ').slice(0, 19) : '—'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.detail-body {
  min-height: 120px;
}

.detail-section + .detail-section {
  margin-top: 20px;
}

.detail-section__title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 600;
}

.detail-content {
  margin: 0;
  padding: 12px 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 8px;
}

.detail-content--answer {
  max-height: 240px;
  overflow-y: auto;
  color: $yszx-text-secondary;
}

.detail-empty {
  margin: 0;
  color: $yszx-text-secondary;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 24px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.detail-item--wide {
  grid-column: span 2;
}

.detail-item__label {
  flex: 0 0 84px;
  color: $yszx-text-secondary;
  font-size: 13px;
}

.detail-item__value {
  min-width: 0;
  word-break: break-word;
}
</style>