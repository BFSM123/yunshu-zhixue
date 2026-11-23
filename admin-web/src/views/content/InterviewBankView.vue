<template>
  <PageShell
    title="面试题库"
    description="Excel 批量导入、手工录入、主题维护与检索测试。"
    pagination
  >
    <template #actions>
      <el-button
        v-permission="'interviewbank:edit'"
        @click="importVisible = true"
      >
        Excel 导入
      </el-button>
      <el-button
        v-permission="'interviewbank:edit'"
        type="primary"
        @click="openCreate"
      >
        录入题目
      </el-button>
    </template>

    <template #filters>
      <el-form inline class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="题干关键词"
            clearable
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="方向">
          <el-select
            v-model="query.subjectId"
            placeholder="全部方向"
            clearable
            @change="search"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="主题">
          <el-select
            v-model="query.topic"
            placeholder="全部主题"
            clearable
            filterable
            :disabled="query.unclassified"
            @change="handleTopicChange"
          >
            <el-option
              v-for="option in topicOptions"
              :key="option.name"
              :label="`${option.name}（${option.count}）`"
              :value="option.name"
            >
              <span class="topic-option">
                <span>{{ option.name }}（{{ option.count }}）</span>
                <el-tag v-if="option.outside" type="warning" size="small" effect="plain">
                  词表外
                </el-tag>
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="难度">
          <el-select
            v-model="query.difficulty"
            placeholder="全部难度"
            clearable
            @change="search"
          >
            <el-option
              v-for="item in INTERVIEW_DIFFICULTY_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.unclassified" @change="handleUnclassifiedChange">
            只看未分类
          </el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </template>

    <template #toolbar>
      <div class="table-toolbar__group">
        <el-button
          v-permission="'interviewbank:edit'"
          :disabled="selectedIds.length === 0"
          @click="handleBatchStatus('on')"
        >
          批量上架
        </el-button>
        <el-button
          v-permission="'interviewbank:edit'"
          :disabled="selectedIds.length === 0"
          @click="handleBatchStatus('off')"
        >
          批量下架
        </el-button>
        <el-button
          v-permission="'interviewbank:delete'"
          type="danger"
          plain
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
      <div class="table-toolbar__right">
        <el-button v-permission="'interviewbank:view'" @click="topicVisible = true">
          主题管理
        </el-button>
        <el-button
          v-permission="'interviewbank:search_test'"
          @click="searchTestVisible = true"
        >
          检索测试
        </el-button>
        <span class="table-toolbar__hint">已选 {{ selectedIds.length }} 项</span>
      </div>
    </template>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="44" />
      <el-table-column label="题目" min-width="210">
        <template #default="{ row }">
          <div class="question-content">
            {{ row.content }}
            <el-tag
              v-if="row.status === 2"
              type="info"
              size="small"
              effect="plain"
              class="source-badge"
            >
              已下架
            </el-tag>
            <el-tag
              v-if="showSourceBadge(row.sourceType)"
              :type="interviewSourceTagType(row.sourceType)"
              size="small"
              effect="plain"
              class="source-badge"
            >
              {{ interviewSourceLabel(row.sourceType) }}
            </el-tag>
          </div>
          <div v-if="row.tags" class="question-meta">标签：{{ row.tags }}</div>
        </template>
      </el-table-column>
      <el-table-column label="来源" min-width="150">
        <template #default="{ row }">
          <div v-if="row.sourceType === 2">手工录入</div>
          <template v-else>
            <div class="source-doc" :title="row.sourceDocName || interviewSourceLabel(row.sourceType)">
              {{ row.sourceDocName || interviewSourceLabel(row.sourceType) }}
            </div>
            <div class="source-meta" :title="`${row.subjectName || ''} · 第 ${row.sourceSeq || 1} 题`">
              <span v-if="row.sourceSeq">第 {{ row.sourceSeq }} 题</span>
              <span v-else>{{ row.subjectName || '—' }}</span>
            </div>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="70">
        <template #default="{ row }">
          <el-tag :type="interviewDifficultyTagType(row.difficulty)" size="small" effect="plain">
            {{ interviewDifficultyLabel(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="主题" min-width="90" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.topic">{{ row.topic }}</span>
          <span v-else class="question-meta">未分类</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="165" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" @click="openDetail(row as InterviewQuestionListItem)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 1"
              v-permission="'interviewbank:edit'"
              link
              type="warning"
              @click="handleStatus(row as InterviewQuestionListItem, 'off')"
            >
              下架
            </el-button>
            <el-button
              v-if="row.status === 2"
              v-permission="'interviewbank:edit'"
              link
              type="success"
              @click="handleStatus(row as InterviewQuestionListItem, 'on')"
            >
              上架
            </el-button>
            <el-button
              v-permission="'interviewbank:edit'"
              link
              type="primary"
              @click="openEdit(row as InterviewQuestionListItem)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'interviewbank:delete'"
              link
              type="danger"
              @click="handleDelete(row as InterviewQuestionListItem)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty
          :description="query.unclassified
            ? '没有未分类的题目 —— 题库里的题都标好主题了'
            : '没有符合条件的题目'"
          :image-size="80"
        />
      </template>
    </el-table>

    <template #pagination>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="handleSizeChange"
      />
    </template>
  </PageShell>

  <InterviewQuestionFormDialog
    v-model="formVisible"
    :mode="formMode"
    :detail="editingDetail"
    :subjects="subjects"
    :topics="topicNames"
    @success="handleSaved"
  />
  <InterviewQuestionDetailDialog
    v-model="detailVisible"
    :question="detail"
    :loading="detailLoading"
    @edit="handleDetailEdit"
  />
  <InterviewQuestionImportDialog
    v-model="importVisible"
    @success="handleSaved"
  />
  <InterviewTopicDialog
    v-model="topicVisible"
    :subjects="subjects"
    @changed="loadTopics"
  />
  <InterviewSearchTestDialog
    v-model="searchTestVisible"
    :subjects="subjects"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import PageShell from '@/components/layout/PageShell.vue'
import InterviewQuestionFormDialog from '@/components/content/InterviewQuestionFormDialog.vue'
import InterviewQuestionDetailDialog from '@/components/content/InterviewQuestionDetailDialog.vue'
import InterviewQuestionImportDialog from '@/components/content/InterviewQuestionImportDialog.vue'
import InterviewTopicDialog from '@/components/content/InterviewTopicDialog.vue'
import InterviewSearchTestDialog from '@/components/content/InterviewSearchTestDialog.vue'
import type {
  InterviewQuestionDetail,
  InterviewQuestionListItem,
  InterviewTopic
} from '@/types/interview'
import type { SubjectCard } from '@/types/content'
import type { InterviewQuestionQuery } from '@/api/interview'
import {
  batchDeleteInterviewQuestions,
  batchUpdateInterviewQuestionStatus,
  deleteInterviewQuestion,
  getInterviewQuestionDetail,
  getInterviewQuestions,
  getInterviewTopics,
  updateInterviewQuestionStatus
} from '@/api/interview'
import { getSubjects } from '@/api/subject'
import {
  INTERVIEW_DIFFICULTY_OPTIONS,
  interviewDifficultyLabel,
  interviewDifficultyTagType,
  interviewSourceLabel,
  interviewSourceTagType,
  showSourceBadge
} from '@/utils/interview'

interface TopicOption {
  name: string
  count: number
  outside: boolean
}

const loading = ref(false)
const rows = ref<InterviewQuestionListItem[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const tableRef = ref<TableInstance>()
const subjects = ref<SubjectCard[]>([])
const topics = ref<InterviewTopic[]>([])

const query = reactive<{
  keyword: string
  subjectId: number | undefined
  topic: string | undefined
  difficulty: number | undefined
  unclassified: boolean
  page: number
  pageSize: number
}>({
  keyword: '',
  subjectId: undefined,
  topic: undefined,
  difficulty: undefined,
  unclassified: false,
  page: 1,
  pageSize: 10
})

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingDetail = ref<InterviewQuestionDetail | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<InterviewQuestionDetail | null>(null)
const importVisible = ref(false)
const topicVisible = ref(false)
const searchTestVisible = ref(false)

const topicOptions = computed<TopicOption[]>(() => {
  const map = new Map<string, TopicOption>()
  for (const topic of topics.value) {
    const existing = map.get(topic.name)
    if (existing) {
      existing.count += topic.questionCount
      existing.outside = existing.outside && !topic.inVocabulary
    } else {
      map.set(topic.name, {
        name: topic.name,
        count: topic.questionCount,
        outside: !topic.inVocabulary
      })
    }
  }
  // 保持后端顺序：词表主题按排序号在前，词表外主题在后。
  return Array.from(map.values())
})

const topicNames = computed(() => topicOptions.value.map((item) => item.name))

let searchTimer: ReturnType<typeof setTimeout> | undefined

watch(
  () => query.keyword,
  () => {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(search, 400)
  }
)

onMounted(async () => {
  await Promise.all([loadSubjects(), loadTopics()])
  await load()
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})

async function loadSubjects() {
  try {
    subjects.value = await getSubjects()
  } catch {
    subjects.value = []
  }
}

async function loadTopics() {
  try {
    topics.value = await getInterviewTopics()
  } catch {
    topics.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const params: InterviewQuestionQuery = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.subjectId === 'number') params.subjectId = query.subjectId
    if (typeof query.difficulty === 'number') params.difficulty = query.difficulty
    if (query.unclassified) {
      params.unclassified = true
    } else if (query.topic) {
      params.topic = query.topic
    }

    const result = await getInterviewQuestions(params)
    rows.value = result.list
    total.value = result.total
    selectedIds.value = []
    await nextTick()
    tableRef.value?.clearSelection()
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function handleTopicChange() {
  if (query.topic) {
    query.unclassified = false
  }
  search()
}

function handleUnclassifiedChange(checked: string | number | boolean) {
  if (checked) {
    query.topic = undefined
  }
  search()
}

function reset() {
  query.keyword = ''
  query.subjectId = undefined
  query.topic = undefined
  query.difficulty = undefined
  query.unclassified = false
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function handleSelectionChange(selection: InterviewQuestionListItem[]) {
  selectedIds.value = selection.map((item) => item.id)
}

async function handleSaved() {
  await Promise.all([loadSubjects(), loadTopics()])
  await load()
}

// ==================== 单题操作 ====================

function openCreate() {
  editingDetail.value = null
  formMode.value = 'create'
  formVisible.value = true
}

async function openEdit(row: InterviewQuestionListItem) {
  try {
    editingDetail.value = await getInterviewQuestionDetail(row.id)
    formMode.value = 'edit'
    formVisible.value = true
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function openDetail(row: InterviewQuestionListItem) {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getInterviewQuestionDetail(row.id)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    detailLoading.value = false
  }
}

function handleDetailEdit(question: InterviewQuestionDetail) {
  detailVisible.value = false
  editingDetail.value = question
  formMode.value = 'edit'
  formVisible.value = true
}

async function handleDelete(row: InterviewQuestionListItem) {
  let questionDetail: InterviewQuestionDetail | null = null
  try {
    questionDetail = await getInterviewQuestionDetail(row.id)
  } catch {
    return
  }

  const risks: string[] = []
  if (questionDetail.sourceType === 2) {
    risks.push('该题为手工录入，没有源文档，删除后不可恢复')
  } else if (questionDetail.sourceType === 3) {
    risks.push(`该题来自导入文件「${questionDetail.sourceDocName || '未知文件'}」，需要时可重新导入`)
  } else {
    risks.push('该题来自文档解析，需要时可重新解析源文档')
  }
  if (questionDetail.referencedCount > 0) {
    risks.push(`已被 ${questionDetail.referencedCount} 场面试引用，删除后历史面试记录中的题干快照仍会保留`)
  }

  try {
    await ElMessageBox.confirm(`${risks.join('；')}。确定删除吗？`, '删除面试题', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await deleteInterviewQuestion(row.id)
    ElMessage.success('面试题已删除')
    await handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleStatus(target: { id: number }, action: 'on' | 'off') {
  const message =
    action === 'off'
      ? '下架后 AI 面试将不再抽取该题目，确定下架吗？'
      : '上架后该题目将进入 AI 面试出题范围，确定上架吗？'
  try {
    await ElMessageBox.confirm(message, action === 'off' ? '下架面试题' : '上架面试题', {
      type: 'warning',
      confirmButtonText: '确定'
    })
  } catch {
    return
  }
  try {
    await updateInterviewQuestionStatus(target.id, action)
    ElMessage.success(action === 'off' ? '面试题已下架' : '面试题已上架')
    await handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

// ==================== 批量操作 ====================

async function handleBatchStatus(action: 'on' | 'off') {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将对选中的 ${selectedIds.value.length} 道面试题执行${action === 'on' ? '批量上架' : '批量下架'}，确定继续吗？`,
      action === 'on' ? '批量上架' : '批量下架',
      { type: 'warning', confirmButtonText: '确定' }
    )
  } catch {
    return
  }
  try {
    const count = await batchUpdateInterviewQuestionStatus(selectedIds.value, action)
    ElMessage.success(`已处理 ${count} 道面试题`)
    await handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将删除选中的 ${selectedIds.value.length} 道面试题，删除后不可恢复，确定继续吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    const count = await batchDeleteInterviewQuestions(selectedIds.value)
    ElMessage.success(`已删除 ${count} 道面试题`)
    await handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

/* 筛选条件自适应换行，窄屏时不隐藏控件 */
.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 12px;
  padding-bottom: 4px;

  :deep(.el-form-item) {
    flex: 1 1 130px;
    min-width: 130px;
    margin: 0 !important;
  }

  :deep(.el-form-item__label) {
    flex: 0 0 auto;
    padding-right: 6px;
  }

  :deep(.el-form-item__content) {
    flex: 1 1 auto;
    min-width: 0;
  }

  :deep(.el-input),
  :deep(.el-select) {
    width: 100%;
  }

  :deep(.el-form-item:first-child) {
    flex: 1.4 1 140px;
  }

  :deep(.el-form-item:nth-child(3)) {
    flex: 1.3 1 140px;
  }

  :deep(.el-form-item:last-child) {
    flex: 0 0 auto;
    min-width: auto;
  }
}

.table-toolbar__right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.topic-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.question-content {
  line-height: 1.6;
  word-break: break-word;
}

.source-badge {
  margin-left: 6px;
  vertical-align: middle;
}

.question-meta {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.source-doc {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-meta {
  margin-top: 2px;
  overflow: hidden;
  color: $yszx-text-secondary;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 4px;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
  padding: 2px 0;
  font-size: 12px;
}
</style>