<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新建学习路线' : '编辑学习路线'"
    width="640px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="路线名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入路线名称" maxlength="100" clearable />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="关联学科" prop="subjectId">
          <el-select
            v-model="form.subjectId"
            placeholder="请选择学科"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="路线图标">
          <el-input v-model="form.icon" placeholder="如 🚀" maxlength="50" clearable />
        </el-form-item>
        <el-form-item label="适合学历">
          <el-input
            v-model="form.targetEducation"
            placeholder="如 本科及以上"
            maxlength="100"
            clearable
          />
        </el-form-item>
      </div>

      <el-form-item label="路线描述">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          placeholder="介绍路线的学习目标、适合人群与学习建议"
          maxlength="2000"
          show-word-limit
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
import { nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createRoute, updateRoute } from '@/api/route'
import type { RouteDetail, RouteItem, RoutePayload } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  mode: 'create' | 'edit'
  route?: RouteItem | RouteDetail | null
  subjects: Array<{ id: number; name: string }>
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  name: '',
  subjectId: undefined as number | undefined,
  status: 1,
  icon: '',
  targetEducation: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入路线名称', trigger: 'blur' }],
  subjectId: [{ required: true, message: '请选择关联学科', trigger: 'change' }],
  status: [{ required: true, message: '请选择路线状态', trigger: 'change' }]
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
  if (props.mode === 'edit' && props.route) {
    form.name = props.route.name
    form.subjectId = props.route.subjectId
    form.status = props.route.status
    form.icon = props.route.icon ?? ''
    form.targetEducation = props.route.targetEducation ?? ''
    form.description = props.route.description ?? ''
    return
  }
  form.name = ''
  form.subjectId = undefined
  form.status = 1
  form.icon = ''
  form.targetEducation = ''
  form.description = ''
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.subjectId === undefined) return

  const payload: RoutePayload = {
    name: form.name.trim(),
    subjectId: form.subjectId,
    status: form.status,
    icon: form.icon.trim() || undefined,
    targetEducation: form.targetEducation.trim() || undefined,
    description: form.description.trim() || undefined
  }

  saving.value = true
  try {
    if (props.mode === 'create') {
      await createRoute(payload)
      ElMessage.success('学习路线已新建')
    } else if (props.route) {
      await updateRoute(props.route.id, payload)
      ElMessage.success('学习路线已更新')
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
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
</style>
