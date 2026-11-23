<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑面试题' : '录入面试题'"
    width="720px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <template v-if="!isEdit">
        <el-form-item label="所属学科" prop="subjectId">
          <el-select
            v-model="form.subjectId"
            placeholder="请选择所属学科（方向）"
            style="width: 100%"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="题干" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="3"
            maxlength="5000"
            show-word-limit
            placeholder="请输入面试题题干"
          />
        </el-form-item>

        <el-form-item label="参考答案（可选）">
          <el-input
            v-model="form.referenceAnswer"
            type="textarea"
            :rows="4"
            maxlength="10000"
            placeholder="参考答案可留空"
          />
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="题干">
          <div class="readonly-content">{{ detail?.content }}</div>
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="edit-tip"
          title="题干与参考答案只读，仅可维护主题与难度"
        />
        <el-form-item label="参考答案">
          <div class="readonly-content readonly-content--answer">
            {{ detail?.referenceAnswer || '未填写参考答案' }}
          </div>
        </el-form-item>
      </template>

      <div class="form-grid form-grid--two">
        <el-form-item label="主题">
          <el-select
            v-model="form.topic"
            placeholder="未分类"
            style="width: 100%"
            filterable
            allow-create
            clearable
            default-first-option
          >
            <el-option
              v-for="topic in topics"
              :key="topic"
              :label="topic"
              :value="topic"
            />
          </el-select>
          <p class="field-hint">主题为空表示「未分类」；可直接输入词表外主题</p>
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="form.difficulty" style="width: 100%">
            <el-option
              v-for="item in INTERVIEW_DIFFICULTY_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item v-if="!isEdit" label="标签（可选）">
        <el-input
          v-model="form.tags"
          maxlength="255"
          placeholder="多个标签用逗号分隔，如：Ollama,本地部署"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  createInterviewQuestion,
  updateInterviewQuestion
} from '@/api/interview'
import type {
  InterviewQuestionDetail,
  InterviewQuestionSavePayload,
  InterviewQuestionUpdatePayload
} from '@/types/interview'
import type { SubjectCard } from '@/types/content'
import { INTERVIEW_DIFFICULTY_OPTIONS } from '@/utils/interview'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  detail?: InterviewQuestionDetail | null
  subjects: SubjectCard[]
  topics: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const isEdit = computed(() => props.mode === 'edit')

const form = reactive<{
  subjectId: number | undefined
  content: string
  referenceAnswer: string
  topic: string
  difficulty: number
  tags: string
}>({
  subjectId: undefined,
  content: '',
  referenceAnswer: '',
  topic: '',
  difficulty: 2,
  tags: ''
})

const rules: FormRules = {
  subjectId: [{ required: true, message: '请选择所属学科', trigger: 'change' }],
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

function fillForm() {
  if (isEdit.value && props.detail) {
    form.subjectId = props.detail.subjectId
    form.content = props.detail.content
    form.referenceAnswer = props.detail.referenceAnswer ?? ''
    form.topic = props.detail.topic ?? ''
    form.difficulty = props.detail.difficulty
    form.tags = props.detail.tags ?? ''
  } else {
    form.subjectId = undefined
    form.content = ''
    form.referenceAnswer = ''
    form.topic = ''
    form.difficulty = 2
    form.tags = ''
  }
  nextTick(() => formRef.value?.clearValidate())
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) fillForm()
  }
)

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const topic = form.topic.trim()
  const topicOutsideVocabulary = topic !== '' && !props.topics.includes(topic)

  saving.value = true
  try {
    if (isEdit.value && props.detail) {
      const payload: InterviewQuestionUpdatePayload = {
        topic: topic || undefined,
        difficulty: form.difficulty
      }
      await updateInterviewQuestion(props.detail.id, payload)
      ElMessage.success('面试题已更新')
    } else {
      if (form.subjectId === undefined) return
      const payload: InterviewQuestionSavePayload = {
        subjectId: form.subjectId,
        content: form.content.trim(),
        referenceAnswer: form.referenceAnswer.trim() || undefined,
        topic: topic || undefined,
        difficulty: form.difficulty,
        tags: form.tags.trim() || undefined
      }
      await createInterviewQuestion(payload)
      ElMessage.success('面试题已录入并上架')
    }
    if (topicOutsideVocabulary) {
      ElMessage.warning('该主题不在主题词表中，可在「主题管理」中维护')
    }
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.edit-tip {
  margin-bottom: 16px;
}

.readonly-content {
  width: 100%;
  padding: 10px 12px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 8px;
}

.readonly-content--answer {
  max-height: 180px;
  overflow-y: auto;
  color: $yszx-text-secondary;
}

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.form-grid--two {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}
</style>