<template>
  <el-dialog
    :model-value="modelValue"
    title="审核题目"
    width="560px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert type="info" :closable="false" class="audit-content">
      <template #title>题目内容</template>
      <p class="audit-content__text">{{ target?.content }}</p>
    </el-alert>

    <el-form label-position="top">
      <el-form-item label="审核结果">
        <el-radio-group v-model="pass">
          <el-radio :value="true">审核通过，上架</el-radio>
          <el-radio :value="false">驳回</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="!pass" label="驳回原因" required>
        <el-input
          v-model="rejectReason"
          type="textarea"
          :rows="3"
          maxlength="255"
          show-word-limit
          placeholder="请填写驳回原因，学员端不会展示"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { auditQuestion } from '@/api/question'

const props = defineProps<{
  modelValue: boolean
  target?: { id: number; content: string } | null
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const pass = ref(true)
const rejectReason = ref('')
const submitting = ref(false)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      pass.value = true
      rejectReason.value = ''
    }
  }
)

async function submit() {
  if (!props.target) return
  const reason = rejectReason.value.trim()
  if (!pass.value && !reason) {
    ElMessage.warning('驳回时必须填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await auditQuestion(props.target.id, {
      pass: pass.value,
      rejectReason: pass.value ? undefined : reason
    })
    ElMessage.success(pass.value ? '审核通过，题目已上架' : '题目已驳回')
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.audit-content {
  margin-bottom: 18px;
}

.audit-content__text {
  margin: 6px 0 0;
  color: #0f172a;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
