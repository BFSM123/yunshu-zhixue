<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增学科' : '编辑学科'"
    width="540px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="学科名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入学科名称" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="学科图标 Emoji" prop="icon">
          <el-input v-model="form.icon" placeholder="如 🤖" maxlength="10" clearable>
            <template #prepend>
              <span class="emoji-preview">{{ form.icon || '❓' }}</span>
            </template>
          </el-input>
        </el-form-item>
      </div>

      <el-form-item label="学科描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="请输入学科描述"
        />
      </el-form-item>

      <el-form-item label="排序号" prop="sortOrder">
        <el-input-number
          v-model="form.sortOrder"
          :min="0"
          :max="9999"
          :controls="false"
          placeholder="留空自动排在最后"
          style="width: 100%"
        />
        <p class="field-hint">数字越小越靠前，留空自动排在最后。</p>
      </el-form-item>

      <el-form-item label="主题颜色" prop="themeColor">
        <div class="color-palette">
          <button
            v-for="color in palette"
            :key="color"
            type="button"
            class="color-swatch"
            :class="{ 'is-active': form.themeColor === color }"
            :style="{ backgroundColor: color }"
            :title="color"
            @click="form.themeColor = color"
          >
            <span v-if="form.themeColor === color" class="color-check">✓</span>
          </button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createSubject, updateSubject } from '@/api/subject'
import type { SubjectCard } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  subject?: SubjectCard | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const palette = [
  '#2563EB',
  '#7C3AED',
  '#059669',
  '#D97706',
  '#DC2626',
  '#0891B2',
  '#DB2777',
  '#EA580C',
  '#0F172A',
  '#475569'
]

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<{
  name: string
  icon: string
  description: string
  sortOrder: number | undefined
  themeColor: string
}>({
  name: '',
  icon: '',
  description: '',
  sortOrder: undefined,
  themeColor: palette[0]
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入学科名称', trigger: 'blur' }],
  icon: [{ required: true, message: '请输入学科图标 Emoji', trigger: 'blur' }],
  description: [{ required: true, message: '请输入学科描述', trigger: 'blur' }],
  themeColor: [{ required: true, message: '请选择主题颜色', trigger: 'change' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    fillForm()
    nextTick(() => formRef.value?.clearValidate())
  }
)

function fillForm() {
  if (props.mode === 'edit' && props.subject) {
    form.name = props.subject.name
    form.icon = props.subject.icon
    form.description = props.subject.description
    form.sortOrder = props.subject.sortOrder
    form.themeColor = props.subject.themeColor || palette[0]
    return
  }
  form.name = ''
  form.icon = ''
  form.description = ''
  form.sortOrder = undefined
  form.themeColor = palette[0]
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      icon: form.icon.trim(),
      description: form.description.trim(),
      sortOrder: form.sortOrder ?? undefined,
      themeColor: form.themeColor
    }
    if (props.mode === 'create') {
      await createSubject(payload)
      ElMessage.success('学科已新增')
    } else if (props.subject) {
      await updateSubject(props.subject.id, payload)
      ElMessage.success('学科已更新')
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

.emoji-preview {
  display: inline-block;
  width: 20px;
  font-size: 16px;
  line-height: 1;
  text-align: center;
}

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.color-swatch {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  color: #fff;
  border: 2px solid transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.color-swatch:hover {
  transform: scale(1.08);
}

.color-swatch.is-active {
  border-color: $yszx-text;
  box-shadow: 0 0 0 2px #fff inset;
}

.color-check {
  font-size: 14px;
  font-weight: 700;
}
</style>
