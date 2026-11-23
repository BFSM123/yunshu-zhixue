<template>
  <el-dialog
    :model-value="modelValue"
    title="面试题主题管理"
    width="760px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="topic-toolbar">
      <div class="topic-toolbar__hint">
        主题词表用于面试题的主题归类；题目中已使用但词表未维护的主题会标注为「词表外」。
      </div>
      <el-button
        v-permission="'interviewbank:edit'"
        type="primary"
        @click="openCreate"
      >
        新增主题
      </el-button>
    </div>

    <el-table v-loading="loading" :data="topics" max-height="420" size="small">
      <el-table-column label="主题名称" min-width="180">
        <template #default="{ row }">
          <div class="topic-name">
            <span>{{ row.name }}</span>
            <el-tag v-if="!row.inVocabulary" type="warning" size="small" effect="plain">
              词表外
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="关联学科" min-width="160">
        <template #default="{ row }">
          {{ row.subjectId ? row.subjectName || '—' : '全局主题' }}
        </template>
      </el-table-column>
      <el-table-column prop="questionCount" label="题目数" width="90" />
      <el-table-column label="排序" width="70">
        <template #default="{ row }">
          {{ row.inVocabulary ? row.sortOrder : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.inVocabulary">
            <el-button
              v-permission="'interviewbank:edit'"
              link
              type="primary"
              @click="openEdit(row as InterviewTopic)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'interviewbank:delete'"
              link
              type="danger"
              @click="handleDelete(row as InterviewTopic)"
            >
              删除
            </el-button>
          </template>
          <span v-else class="topic-outside-hint">请新增同名词表主题</span>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="主题词表为空，可新增主题或直接导入题目" :image-size="70" />
      </template>
    </el-table>

    <el-dialog
      v-model="formVisible"
      :title="editingId ? '编辑主题' : '新增主题'"
      width="460px"
      append-to-body
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="主题名称" prop="name">
          <el-input v-model="form.name" maxlength="50" placeholder="请输入主题名称" />
        </el-form-item>
        <el-form-item label="关联学科">
          <el-select
            v-model="form.subjectId"
            placeholder="全局主题（不限定学科）"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
          <p class="field-hint">不选择学科表示全局主题</p>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
          <p class="field-hint">数值越小越靠前，不填自动排在末尾</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createInterviewTopic,
  deleteInterviewTopic,
  getInterviewTopics,
  updateInterviewTopic
} from '@/api/interview'
import type { InterviewTopic } from '@/types/interview'
import type { SubjectCard } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  subjects: SubjectCard[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  changed: []
}>()

const loading = ref(false)
const topics = ref<InterviewTopic[]>([])

const formVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<{
  name: string
  subjectId: number | null
  sortOrder: number | undefined
}>({
  name: '',
  subjectId: null,
  sortOrder: undefined
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入主题名称', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) load()
  }
)

async function load() {
  loading.value = true
  try {
    topics.value = await getInterviewTopics()
  } catch {
    topics.value = []
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.name = ''
  form.subjectId = null
  form.sortOrder = undefined
  formVisible.value = true
}

function openEdit(topic: InterviewTopic) {
  if (!topic.id) return
  editingId.value = topic.id
  form.name = topic.name
  form.subjectId = topic.subjectId ?? null
  form.sortOrder = topic.sortOrder ?? undefined
  formVisible.value = true
}

async function submit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = {
      name: form.name.trim(),
      subjectId: form.subjectId,
      sortOrder: form.sortOrder
    }
    if (editingId.value) {
      await updateInterviewTopic(editingId.value, payload)
      ElMessage.success('主题已更新')
    } else {
      await createInterviewTopic(payload)
      ElMessage.success('主题已新增')
    }
    formVisible.value = false
    await load()
    emit('changed')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function handleDelete(topic: InterviewTopic) {
  if (!topic.id) return
  try {
    await ElMessageBox.confirm(
      topic.questionCount > 0
        ? `该主题下有 ${topic.questionCount} 道面试题，需先调整题目主题才能删除。确定删除吗？`
        : `确定删除主题「${topic.name}」吗？`,
      '删除主题',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    await deleteInterviewTopic(topic.id)
    ElMessage.success('主题已删除')
    await load()
    emit('changed')
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.topic-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.topic-toolbar__hint {
  color: $yszx-text-secondary;
  font-size: 13px;
  line-height: 1.6;
}

.topic-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.topic-outside-hint {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.field-hint {
  margin: 4px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>