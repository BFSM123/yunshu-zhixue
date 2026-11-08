<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增阶段' : '编辑阶段'"
    width="480px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="阶段图标" prop="icon">
        <el-input v-model="form.icon" placeholder="留空默认 📚" maxlength="10" clearable>
          <template #prepend>
            <span class="emoji-preview">{{ form.icon || '📚' }}</span>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item label="阶段名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入阶段名称" maxlength="50" clearable />
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
import { createStage, updateStage } from '@/api/subject'
import type { StageItem } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  subjectId: number | null
  stage?: StageItem | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive<{
  name: string
  icon: string
  sortOrder: number | undefined
}>({
  name: '',
  icon: '',
  sortOrder: undefined
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入阶段名称', trigger: 'blur' }]
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
  if (props.mode === 'edit' && props.stage) {
    form.name = props.stage.name
    form.icon = props.stage.icon || ''
    form.sortOrder = props.stage.sortOrder
    return
  }
  form.name = ''
  form.icon = ''
  form.sortOrder = undefined
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || props.subjectId == null) return

  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      icon: form.icon.trim() || undefined,
      sortOrder: form.sortOrder ?? undefined
    }
    if (props.mode === 'create') {
      await createStage(props.subjectId, payload)
      ElMessage.success('阶段已新增')
    } else if (props.stage) {
      await updateStage(props.subjectId, props.stage.id, payload)
      ElMessage.success('阶段已更新')
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
</style>
