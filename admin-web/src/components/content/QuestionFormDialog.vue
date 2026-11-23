<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增题目' : '编辑题目'"
    width="760px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="题目内容" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="3"
          maxlength="5000"
          show-word-limit
          placeholder="请输入题干"
        />
      </el-form-item>

      <div class="form-grid form-grid--three">
        <el-form-item label="学科" prop="subjectId">
          <el-select
            v-model="form.subjectId"
            placeholder="请选择学科"
            style="width: 100%"
            @change="handleSubjectChange"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段" prop="stageId">
          <el-select
            v-model="form.stageId"
            placeholder="请先选择学科"
            :disabled="!form.subjectId"
            style="width: 100%"
          >
            <el-option
              v-for="stage in stageOptions"
              :key="stage.id"
              :label="stage.name"
              :value="stage.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="难度" prop="difficulty">
          <el-select v-model="form.difficulty" style="width: 100%">
            <el-option
              v-for="item in QUESTION_DIFFICULTY_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item label="题型" prop="type">
        <el-radio-group v-model="form.type" @change="handleTypeChange">
          <el-radio-button
            v-for="item in QUESTION_TYPE_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <template v-if="isChoice">
        <el-form-item label="选项与正确答案">
          <div class="option-list">
            <div
              v-for="(option, index) in form.options"
              :key="index"
              class="option-row"
            >
              <span class="option-key">{{ optionKey(index) }}</span>
              <el-input
                v-model="option.optionText"
                :placeholder="`请输入选项 ${optionKey(index)}`"
                maxlength="500"
                clearable
              />
              <el-checkbox
                v-if="form.type === 2"
                v-model="option.correct"
                class="option-correct"
              >
                正确
              </el-checkbox>
              <el-checkbox
                v-else
                :model-value="option.correct"
                class="option-correct"
                @change="setSingleCorrect(index)"
              >
                正确
              </el-checkbox>
              <el-button
                link
                type="danger"
                :disabled="form.options.length <= 2"
                @click="removeOption(index)"
              >
                删除
              </el-button>
            </div>
            <el-button
              link
              type="primary"
              :disabled="form.options.length >= 6"
              @click="addOption"
            >
              + 添加选项
            </el-button>
            <p class="field-hint">
              至少 2 个选项；单选只能有 1 个正确答案，多选至少 1 个。
            </p>
          </div>
        </el-form-item>
      </template>

      <el-form-item v-else-if="form.type === 3" label="正确答案" prop="answer">
        <el-radio-group v-model="form.answer">
          <el-radio value="正确">正确</el-radio>
          <el-radio value="错误">错误</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-else label="参考答案（可选）">
        <el-input
          v-model="form.answer"
          type="textarea"
          :rows="3"
          maxlength="5000"
          placeholder="简答题参考答案，可留空"
        />
      </el-form-item>

      <el-form-item label="解析（可选）">
        <el-input
          v-model="form.analysis"
          type="textarea"
          :rows="2"
          maxlength="5000"
          placeholder="答案解析"
        />
      </el-form-item>

      <el-form-item label="标签（可选）">
        <el-input
          v-model="form.tags"
          maxlength="255"
          placeholder="多个标签用逗号分隔，如：JavaSE基础,集合"
        />
      </el-form-item>

      <el-form-item v-if="mode === 'edit' && detail" label="当前状态">
        <el-tag :type="questionStatusTagType(detail.status)" effect="plain">
          {{ questionStatusLabel(detail.status) }}
        </el-tag>
        <span class="field-hint field-hint--inline">
          编辑后状态按规则自动保持或重新进入审核队列
        </span>
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
import { createQuestion, updateQuestion } from '@/api/question'
import type { QuestionDetail, QuestionSavePayload } from '@/types/question'
import type { SubjectCard } from '@/types/content'
import {
  QUESTION_DIFFICULTY_OPTIONS,
  QUESTION_TYPE_OPTIONS,
  questionStatusLabel,
  questionStatusTagType
} from '@/utils/question'

interface OptionForm {
  optionText: string
  correct: boolean
}

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  detail?: QuestionDetail | null
  subjects: SubjectCard[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const OPTION_KEYS = ['A', 'B', 'C', 'D', 'E', 'F']

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<{
  content: string
  subjectId: number | undefined
  stageId: number | undefined
  type: number
  difficulty: number
  tags: string
  analysis: string
  answer: string
  options: OptionForm[]
}>({
  content: '',
  subjectId: undefined,
  stageId: undefined,
  type: 1,
  difficulty: 1,
  tags: '',
  analysis: '',
  answer: '',
  options: []
})

const isChoice = computed(() => form.type === 1 || form.type === 2)

const stageOptions = computed(
  () => props.subjects.find((subject) => subject.id === form.subjectId)?.stages ?? []
)

const rules: FormRules = {
  content: [{ required: true, message: '请输入题干', trigger: 'blur' }],
  subjectId: [{ required: true, message: '请选择学科', trigger: 'change' }],
  stageId: [{ required: true, message: '请选择阶段', trigger: 'change' }],
  type: [{ required: true, message: '请选择题型', trigger: 'change' }],
  difficulty: [{ required: true, message: '请选择难度', trigger: 'change' }]
}

// 弹窗每次打开时按新增/编辑填充表单
function fillForm() {
  if (props.mode === 'edit' && props.detail) {
    const detail = props.detail
    form.content = detail.content
    form.subjectId = detail.subjectId ?? undefined
    form.stageId = detail.stageId
    form.type = detail.type
    form.difficulty = detail.difficulty
    form.tags = detail.tags ?? ''
    form.analysis = detail.analysis ?? ''
    form.answer = detail.answer ?? ''
    form.options = detail.options.length
      ? detail.options.map((option) => ({
          optionText: option.optionText,
          correct: option.isCorrect === 1
        }))
      : defaultOptions()
    ensureOptionCount()
  } else {
    form.content = ''
    form.subjectId = undefined
    form.stageId = undefined
    form.type = 1
    form.difficulty = 1
    form.tags = ''
    form.analysis = ''
    form.answer = ''
    form.options = defaultOptions()
  }
  nextTick(() => formRef.value?.clearValidate())
}

function defaultOptions(): OptionForm[] {
  return Array.from({ length: 4 }, () => ({ optionText: '', correct: false }))
}

function ensureOptionCount() {
  while (form.options.length < 2) {
    form.options.push({ optionText: '', correct: false })
  }
}

function optionKey(index: number): string {
  return OPTION_KEYS[index] ?? '?'
}

function handleSubjectChange() {
  form.stageId = undefined
}

function handleTypeChange() {
  if (form.type === 3) {
    form.answer = form.answer === '错误' ? '错误' : '正确'
    return
  }
  if (form.type === 4) {
    form.answer = form.answer === '正确' || form.answer === '错误' ? '' : form.answer
    return
  }
  form.answer = ''
  ensureOptionCount()
  if (form.type === 1) {
    const firstCorrect = form.options.findIndex((option) => option.correct)
    form.options.forEach((option, index) => {
      option.correct = index === (firstCorrect >= 0 ? firstCorrect : 0)
    })
  }
}

function addOption() {
  if (form.options.length >= 6) return
  form.options.push({ optionText: '', correct: false })
}

function removeOption(index: number) {
  if (form.options.length <= 2) return
  const removedCorrect = form.options[index].correct
  form.options.splice(index, 1)
  if (form.type === 1 && removedCorrect) {
    form.options[0].correct = true
  }
}

function setSingleCorrect(index: number) {
  form.options.forEach((option, current) => {
    option.correct = current === index
  })
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.subjectId === undefined || form.stageId === undefined) return

  const payload: QuestionSavePayload = {
    content: form.content.trim(),
    stageId: form.stageId,
    type: form.type,
    difficulty: form.difficulty,
    tags: form.tags.trim() || undefined,
    analysis: form.analysis.trim() || undefined
  }

  if (isChoice.value) {
    const options = form.options.filter((option) => option.optionText.trim())
    if (options.length < 2) {
      ElMessage.warning('选择题至少填写 2 个选项')
      return
    }
    const correctCount = options.filter((option) => option.correct).length
    if (form.type === 1 && correctCount !== 1) {
      ElMessage.warning('单选题只能设置 1 个正确答案')
      return
    }
    if (form.type === 2 && correctCount < 1) {
      ElMessage.warning('多选题至少设置 1 个正确答案')
      return
    }
    payload.options = options.map((option) => ({
      optionText: option.optionText.trim(),
      correct: option.correct
    }))
  } else if (form.type === 3) {
    payload.answer = form.answer
  } else {
    payload.answer = form.answer.trim() || undefined
  }

  saving.value = true
  try {
    if (props.mode === 'create') {
      await createQuestion(payload)
      ElMessage.success('题目已新增，当前为待审核状态')
    } else if (props.detail) {
      await updateQuestion(props.detail.id, payload)
      ElMessage.success('题目已更新')
    }
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

// 打开时填充；关闭时不处理，destroy-on-close 会销毁表单
watch(
  () => props.modelValue,
  (open) => {
    if (open) fillForm()
  }
)
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.form-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.option-list {
  width: 100%;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.option-key {
  display: inline-flex;
  flex: 0 0 24px;
  align-items: center;
  justify-content: center;
  height: 24px;
  color: $yszx-text-secondary;
  font-size: 12px;
  font-weight: 600;
  background: #f1f5f9;
  border-radius: 50%;
}

.option-correct {
  flex: 0 0 auto;
}

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.field-hint--inline {
  margin-left: 10px;
}

@media (max-width: 720px) {
  .form-grid,
  .form-grid--three {
    grid-template-columns: 1fr;
  }
}
</style>
