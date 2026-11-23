<template>
  <el-drawer
    :model-value="modelValue"
    title="题目详情"
    size="640px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="detail-body">
      <template v-if="question">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="学科">
            {{ question.subjectName ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="阶段">
            {{ question.stageName ?? '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="题型">
            <el-tag :type="questionTypeTagType(question.type)" size="small" effect="plain">
              {{ questionTypeLabel(question.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="难度">
            <el-tag
              :type="questionDifficultyTagType(question.difficulty)"
              size="small"
              effect="plain"
            >
              {{ questionDifficultyLabel(question.difficulty) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag
              :type="questionStatusTagType(question.status)"
              size="small"
              effect="plain"
            >
              {{ questionStatusLabel(question.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标签">
            {{ question.tags || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ question.createdAt }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ question.updatedAt }}
          </el-descriptions-item>
        </el-descriptions>

        <el-alert
          v-if="question.status === 5 && question.rejectReason"
          type="error"
          :closable="false"
          class="section-gap"
          :title="`驳回原因：${question.rejectReason}`"
        />

        <section class="detail-section">
          <h3 class="detail-section__title">题干</h3>
          <p class="detail-text">{{ question.content }}</p>
        </section>

        <section v-if="question.options.length" class="detail-section">
          <h3 class="detail-section__title">选项</h3>
          <div
            v-for="option in question.options"
            :key="option.id"
            class="detail-option"
            :class="{ 'detail-option--correct': option.isCorrect === 1 }"
          >
            <span class="detail-option__key">{{ option.optionKey }}</span>
            <span class="detail-option__text">{{ option.optionText }}</span>
            <el-tag v-if="option.isCorrect === 1" type="success" size="small" effect="plain">
              正确答案
            </el-tag>
          </div>
        </section>

        <section v-if="!isChoice" class="detail-section">
          <h3 class="detail-section__title">
            {{ question.type === 3 ? '正确答案' : '参考答案' }}
          </h3>
          <p class="detail-text">{{ question.answer || '未填写' }}</p>
        </section>

        <section class="detail-section">
          <h3 class="detail-section__title">解析</h3>
          <p class="detail-text detail-text--muted">{{ question.analysis || '暂无解析' }}</p>
        </section>

        <section class="detail-section">
          <h3 class="detail-section__title">答题统计</h3>
          <div class="answer-stats">
            <div class="answer-stat">
              <span class="answer-stat__label">答题次数</span>
              <span class="answer-stat__value">{{ question.doneCount }}</span>
            </div>
            <div class="answer-stat">
              <span class="answer-stat__label">正确次数</span>
              <span class="answer-stat__value">{{ question.correctCount }}</span>
            </div>
            <div class="answer-stat">
              <span class="answer-stat__label">正确率</span>
              <span class="answer-stat__value">{{ question.accuracyRate }}%</span>
            </div>
          </div>
        </section>
      </template>
      <el-empty v-else-if="!loading" description="题目不存在" />
    </div>

    <template #footer>
      <div class="detail-footer">
        <el-button @click="emit('update:modelValue', false)">关闭</el-button>
        <div v-if="question" class="detail-footer__actions">
          <el-button
            v-if="question.status === 3"
            v-permission="'question:audit'"
            type="primary"
            @click="emit('audit', question)"
          >
            审核
          </el-button>
          <el-button
            v-if="question.status === 5"
            v-permission="'question:edit'"
            type="warning"
            @click="emit('resubmit', question)"
          >
            重新提交
          </el-button>
          <el-button
            v-if="question.status === 1"
            v-permission="'question:edit'"
            type="warning"
            plain
            @click="emit('status', question, 'off')"
          >
            下架
          </el-button>
          <el-button
            v-if="question.status === 2"
            v-permission="'question:edit'"
            type="success"
            plain
            @click="emit('status', question, 'on')"
          >
            上架
          </el-button>
          <el-button v-permission="'question:edit'" @click="emit('edit', question)">
            编辑
          </el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { QuestionDetail } from '@/types/question'
import {
  questionDifficultyLabel,
  questionDifficultyTagType,
  questionStatusLabel,
  questionStatusTagType,
  questionTypeLabel,
  questionTypeTagType
} from '@/utils/question'

const props = defineProps<{
  modelValue: boolean
  question: QuestionDetail | null
  loading: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  edit: [QuestionDetail]
  audit: [QuestionDetail]
  status: [QuestionDetail, 'on' | 'off']
  resubmit: [QuestionDetail]
}>()

const isChoice = computed(
  () => props.question?.type === 1 || props.question?.type === 2
)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.detail-body {
  min-height: 200px;
}

.detail-section {
  margin-top: 22px;
}

.detail-section__title {
  margin-bottom: 10px;
  font-size: 15px;
  font-weight: 600;
}

.detail-text {
  margin: 0;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-text--muted {
  color: $yszx-text-secondary;
}

.detail-option {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 8px;
}

.detail-option--correct {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.detail-option__key {
  display: inline-flex;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  height: 24px;
  color: $yszx-text-secondary;
  font-size: 12px;
  font-weight: 600;
  background: #e2e8f0;
  border-radius: 50%;
}

.detail-option__text {
  flex: 1;
  line-height: 1.7;
  word-break: break-word;
}

.answer-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.answer-stat {
  padding: 14px 16px;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 10px;
}

.answer-stat__label {
  display: block;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.answer-stat__value {
  display: block;
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
}

.detail-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.detail-footer__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
