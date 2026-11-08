<template>
  <el-dialog
    :model-value="modelValue"
    title="新增角色"
    width="480px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="角色名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入角色名称" maxlength="50" />
      </el-form-item>
      <el-form-item label="角色编码" prop="code">
        <el-input v-model="form.code" placeholder="如 content_editor" maxlength="50" />
        <p class="field-hint">仅支持小写字母、数字和下划线，创建后不可修改。</p>
      </el-form-item>
      <el-form-item label="角色描述" prop="description">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="选填"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createRole } from '@/api/role'

const props = defineProps<{ modelValue: boolean }>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({ name: '', code: '', description: '' })

const rules: FormRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { max: 50, message: '角色名称不能超过 50 位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]*$/,
      message: '编码仅支持小写字母、数字和下划线，且以字母开头',
      trigger: 'blur'
    }
  ]
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    Object.assign(form, { name: '', code: '', description: '' })
    formRef.value?.clearValidate()
  }
)

async function submit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await createRole({
      name: form.name.trim(),
      code: form.code.trim(),
      description: form.description.trim() || undefined
    })
    ElMessage.success('角色已创建')
    emit('success')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>
