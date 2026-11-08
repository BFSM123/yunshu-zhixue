<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增课程' : '编辑课程'"
    width="640px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="课程名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入课程名称" maxlength="100" clearable />
      </el-form-item>

      <el-form-item label="封面图片 URL" prop="coverUrl">
        <el-input v-model="form.coverUrl" placeholder="https://..." maxlength="500" clearable />
        <div v-if="form.coverUrl" class="cover-preview">
          <el-image :src="form.coverUrl" fit="cover" class="cover-preview__image">
            <template #error>
              <div class="cover-preview__error">封面加载失败</div>
            </template>
          </el-image>
        </div>
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="时长（小时:分钟）" prop="duration" required>
          <el-input v-model="form.duration" placeholder="如 2:30" maxlength="7" clearable />
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-select v-model="form.source" style="width: 100%">
            <el-option label="B站" :value="1" />
            <el-option label="其他" :value="2" />
          </el-select>
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="所属学科" prop="subjectId">
          <el-select v-model="form.subjectId" placeholder="请选择学科" style="width: 100%">
            <el-option
              v-for="option in subjectOptions"
              :key="option.id"
              :label="option.name"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联学科（可选）">
          <el-select
            v-model="form.subjectIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            placeholder="可关联多个学科"
            style="width: 100%"
          >
            <el-option
              v-for="option in extraSubjectOptions"
              :key="option.id"
              :label="option.name"
              :value="option.id"
            />
          </el-select>
          <p class="field-hint">用于学习路线按学科筛选，主学科无需重复选择。</p>
        </el-form-item>
      </div>

      <el-form-item label="课程链接" prop="link">
        <el-input
          v-model="form.link"
          placeholder="B 站课程链接，可自动识别 BV 号"
          maxlength="500"
          clearable
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
import { createCourse, updateCourse } from '@/api/course'
import type { CourseItem, CoursePayload, SubjectCard } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  course?: CourseItem | null
  subjects: SubjectCard[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const OTHER_SUBJECT_ID = 0
const DURATION_PATTERN = /^\d{1,4}:\d{1,2}$/

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<{
  name: string
  coverUrl: string
  duration: string
  source: number
  subjectId: number | undefined
  subjectIds: number[]
  link: string
}>({
  name: '',
  coverUrl: '',
  duration: '',
  source: 1,
  subjectId: undefined,
  subjectIds: [],
  link: ''
})

const subjectOptions = computed(() => [
  ...props.subjects.map((subject) => ({ id: subject.id, name: subject.name })),
  { id: OTHER_SUBJECT_ID, name: '其他学科' }
])

const extraSubjectOptions = computed(() =>
  subjectOptions.value.filter((option) => option.id !== form.subjectId)
)

const rules: FormRules = {
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  duration: [{ validator: validateDuration, trigger: 'blur' }],
  source: [{ required: true, message: '请选择课程来源', trigger: 'change' }],
  subjectId: [{ required: true, message: '请选择所属学科', trigger: 'change' }],
  coverUrl: [{ validator: validateUrl, trigger: 'blur' }],
  link: [{ validator: validateUrl, trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    fillForm()
    nextTick(() => formRef.value?.clearValidate())
  }
)

watch(
  () => form.subjectId,
  (subjectId) => {
    if (subjectId === undefined) return
    form.subjectIds = form.subjectIds.filter((id) => id !== subjectId)
  }
)

function fillForm() {
  if (props.mode === 'edit' && props.course) {
    form.name = props.course.name
    form.coverUrl = props.course.coverUrl ?? ''
    form.duration = formatDurationInput(props.course.durationMinutes)
    form.source = props.course.source
    form.subjectId = props.course.subjectId ?? OTHER_SUBJECT_ID
    form.subjectIds = [...props.course.subjectIds]
    form.link = props.course.link ?? ''
    return
  }
  form.name = ''
  form.coverUrl = ''
  form.duration = ''
  form.source = 1
  form.subjectId = undefined
  form.subjectIds = []
  form.link = ''
}

function formatDurationInput(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return `${hours}:${String(rest).padStart(2, '0')}`
}

function parseDuration(value: string): number | null {
  if (!DURATION_PATTERN.test(value)) return null
  const [hours, minutes] = value.split(':').map(Number)
  if (minutes >= 60) return null
  const total = hours * 60 + minutes
  return total >= 1 && total <= 100_000 ? total : null
}

function validateDuration(_rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!value.trim()) {
    callback(new Error('请输入课程时长'))
    return
  }
  if (parseDuration(value.trim()) === null) {
    callback(new Error('时长格式应为 小时:分钟，如 2:30'))
    return
  }
  callback()
}

function validateUrl(_rule: unknown, value: string, callback: (error?: Error) => void) {
  const url = value.trim()
  if (url && !/^https?:\/\/.+/.test(url)) {
    callback(new Error('请输入以 http:// 或 https:// 开头的地址'))
    return
  }
  callback()
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.subjectId === undefined) return

  const durationMinutes = parseDuration(form.duration.trim())
  if (durationMinutes === null) return

  saving.value = true
  try {
    const payload: CoursePayload = {
      name: form.name.trim(),
      coverUrl: form.coverUrl.trim() || undefined,
      durationMinutes,
      source: form.source,
      link: form.link.trim() || undefined,
      subjectId: form.subjectId === OTHER_SUBJECT_ID ? null : form.subjectId,
      subjectIds: form.subjectIds.filter((id) => id !== form.subjectId)
    }
    if (props.mode === 'create') {
      await createCourse(payload)
      ElMessage.success('课程已新增')
    } else if (props.course) {
      await updateCourse(props.course.id, payload)
      ElMessage.success('课程已更新')
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

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.cover-preview {
  margin-top: 8px;
}

.cover-preview__image {
  width: 160px;
  height: 90px;
  border-radius: 8px;
}

.cover-preview__error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 160px;
  height: 90px;
  color: $yszx-text-secondary;
  font-size: 12px;
  background: #f1f5f9;
  border-radius: 8px;
}
</style>
