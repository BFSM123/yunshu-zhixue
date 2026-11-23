<template>
  <PageShell
    title="题库管理"
    description="题目审核、状态流转、Excel 导入导出与批量操作。"
    pagination
  >
    <template #actions>
      <el-button v-permission="'question:import'" @click="importVisible = true">Excel 导入</el-button>
      <el-button v-permission="'question:export'" :loading="exporting" @click="handleExport">
        导出
      </el-button>
      <el-button v-permission="'question:add'" type="primary" @click="openCreate">
        新增题目
      </el-button>
    </template>

    <template #summary>
      <div class="stat-grid stat-grid--seven">
        <StatCard label="题目总数" :value="String(stats.total)" />
        <StatCard label="已上架" :value="String(stats.onShelf)" />
        <StatCard label="待审核" :value="String(stats.pending)" />
        <StatCard label="单选题" :value="String(stats.single)" />
        <StatCard label="多选题" :value="String(stats.multiple)" />
        <StatCard label="判断题" :value="String(stats.judgment)" />
        <StatCard label="简答题" :value="String(stats.shortAnswer)" />
      </div>
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
        <el-form-item label="学科">
          <el-select
            v-model="query.subjectId"
            placeholder="全部学科"
            clearable
            @change="handleSubjectFilterChange"
          >
            <el-option
              v-for="subject in subjects"
              :key="subject.id"
              :label="subject.name"
              :value="subject.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="阶段">
          <el-select
            v-model="query.stageId"
            placeholder="全部阶段"
            clearable
            filterable
            @change="search"
          >
            <el-option
              v-for="option in stageFilterOptions"
              :key="option.id"
              :label="option.label"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="题型">
          <el-select
            v-model="query.type"
            placeholder="全部题型"
            clearable
            @change="search"
          >
            <el-option
              v-for="item in QUESTION_TYPE_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
              v-for="item in QUESTION_DIFFICULTY_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部状态"
            clearable
            @change="search"
          >
            <el-option
              v-for="item in QUESTION_STATUS_OPTIONS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
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
          v-permission="'question:edit'"
          :disabled="selectedIds.length === 0"
          @click="handleBatchStatus('on')"
        >
          批量上架
        </el-button>
        <el-button
          v-permission="'question:edit'"
          :disabled="selectedIds.length === 0"
          @click="handleBatchStatus('off')"
        >
          批量下架
        </el-button>
        <el-button
          v-permission="'question:audit'"
          :disabled="selectedIds.length === 0"
          @click="handleBatchAudit"
        >
          批量审核通过
        </el-button>
        <el-button
          v-permission="'question:delete'"
          type="danger"
          plain
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
      <span class="table-toolbar__hint">已选 {{ selectedIds.length }} 项</span>
    </template>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="48" />
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="题目" min-width="320">
        <template #default="{ row }">
          <div class="question-content">{{ row.content }}</div>
          <div v-if="row.tags" class="question-meta">标签：{{ row.tags }}</div>
        </template>
      </el-table-column>
      <el-table-column label="阶段" min-width="180">
        <template #default="{ row }">
          <div>{{ row.stageName || '—' }}</div>
          <div class="question-meta">{{ row.subjectName || '—' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="86">
        <template #default="{ row }">
          <el-tag :type="questionTypeTagType(row.type)" size="small" effect="plain">
            {{ questionTypeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="难度" width="86">
        <template #default="{ row }">
          <el-tag :type="questionDifficultyTagType(row.difficulty)" size="small" effect="plain">
            {{ questionDifficultyLabel(row.difficulty) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag :type="questionStatusTagType(row.status)" size="small" effect="plain">
            {{ questionStatusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <div class="row-actions">
            <el-button link type="primary" @click="openDetail(row as QuestionListItem)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 3"
              v-permission="'question:audit'"
              link
              type="primary"
              @click="openAudit(row as QuestionListItem)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.status === 5"
              v-permission="'question:edit'"
              link
              type="warning"
              @click="handleResubmit(row as QuestionListItem)"
            >
              重新提交
            </el-button>
            <el-button
              v-if="row.status === 1"
              v-permission="'question:edit'"
              link
              type="warning"
              @click="handleStatus(row as QuestionListItem, 'off')"
            >
              下架
            </el-button>
            <el-button
              v-if="row.status === 2"
              v-permission="'question:edit'"
              link
              type="success"
              @click="handleStatus(row as QuestionListItem, 'on')"
            >
              上架
            </el-button>
            <el-button
              v-permission="'question:edit'"
              link
              type="primary"
              @click="openEdit(row as QuestionListItem)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'question:delete'"
              link
              type="danger"
              @click="handleDelete(row as QuestionListItem)"
            >
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无匹配的题目，试试调整筛选条件" :image-size="80" />
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

  <QuestionFormDialog
    v-model="formVisible"
    :mode="formMode"
    :detail="editingDetail"
    :subjects="subjects"
    @success="handleSaved"
  />
  <QuestionAuditDialog
    v-model="auditVisible"
    :target="auditTarget"
    @success="handleSaved"
  />
  <QuestionDetailDrawer
    v-model="detailVisible"
    :question="detail"
    :loading="detailLoading"
    @edit="handleDetailEdit"
    @audit="handleDetailAudit"
    @status="handleDetailStatus"
    @resubmit="handleDetailResubmit"
  />
  <QuestionImportDialog
    v-model="importVisible"
    @success="handleSaved"
  />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import PageShell from '@/components/layout/PageShell.vue'
import StatCard from '@/components/layout/StatCard.vue'
import QuestionFormDialog from '@/components/content/QuestionFormDialog.vue'
import QuestionAuditDialog from '@/components/content/QuestionAuditDialog.vue'
import QuestionDetailDrawer from '@/components/content/QuestionDetailDrawer.vue'
import QuestionImportDialog from '@/components/content/QuestionImportDialog.vue'
import type { QuestionDetail, QuestionListItem, QuestionStats } from '@/types/question'
import type { SubjectCard } from '@/types/content'
import type { QuestionFilter, QuestionQuery } from '@/api/question'
import {
  batchAuditQuestions,
  batchDeleteQuestions,
  batchUpdateQuestionStatus,
  deleteQuestion,
  exportQuestions,
  getQuestionDetail,
  getQuestions,
  getQuestionStats,
  resubmitQuestion,
  updateQuestionStatus
} from '@/api/question'
import { getSubjects } from '@/api/subject'
import {
  QUESTION_DIFFICULTY_OPTIONS,
  QUESTION_STATUS_OPTIONS,
  QUESTION_TYPE_OPTIONS,
  questionDifficultyLabel,
  questionDifficultyTagType,
  questionStatusLabel,
  questionStatusTagType,
  questionTypeLabel,
  questionTypeTagType
} from '@/utils/question'

const loading = ref(false)
const exporting = ref(false)
const rows = ref<QuestionListItem[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const tableRef = ref<TableInstance>()
const subjects = ref<SubjectCard[]>([])
const stats = ref<QuestionStats>({
  total: 0,
  onShelf: 0,
  pending: 0,
  single: 0,
  multiple: 0,
  judgment: 0,
  shortAnswer: 0
})

const query = reactive<{
  keyword: string
  subjectId: number | undefined
  stageId: number | undefined
  type: number | undefined
  difficulty: number | undefined
  status: number | undefined
  page: number
  pageSize: number
}>({
  keyword: '',
  subjectId: undefined,
  stageId: undefined,
  type: undefined,
  difficulty: undefined,
  status: undefined,
  page: 1,
  pageSize: 10
})

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingDetail = ref<QuestionDetail | null>(null)
const auditVisible = ref(false)
const auditTarget = ref<{ id: number; content: string } | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<QuestionDetail | null>(null)
const importVisible = ref(false)

const stageFilterOptions = computed(() => {
  if (query.subjectId) {
    const subject = subjects.value.find((item) => item.id === query.subjectId)
    return (subject?.stages ?? []).map((stage) => ({
      id: stage.id,
      label: stage.name
    }))
  }
  return subjects.value.flatMap((subject) =>
    subject.stages.map((stage) => ({
      id: stage.id,
      label: `${subject.name} / ${stage.name}`
    }))
  )
})

let searchTimer: ReturnType<typeof setTimeout> | undefined

watch(
  () => query.keyword,
  () => {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(search, 400)
  }
)

onMounted(async () => {
  await loadSubjects()
  await Promise.all([loadStats(), load()])
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

async function loadStats() {
  try {
    stats.value = await getQuestionStats()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function load() {
  loading.value = true
  try {
    const params: QuestionQuery = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.subjectId === 'number') params.subjectId = query.subjectId
    if (typeof query.stageId === 'number') params.stageId = query.stageId
    if (typeof query.type === 'number') params.type = query.type
    if (typeof query.difficulty === 'number') params.difficulty = query.difficulty
    if (typeof query.status === 'number') params.status = query.status

    const result = await getQuestions(params)
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

function handleSubjectFilterChange() {
  query.stageId = undefined
  search()
}

function reset() {
  query.keyword = ''
  query.subjectId = undefined
  query.stageId = undefined
  query.type = undefined
  query.difficulty = undefined
  query.status = undefined
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function handleSelectionChange(selection: QuestionListItem[]) {
  selectedIds.value = selection.map((item) => item.id)
}

function handleSaved() {
  load()
  loadStats()
}

// ==================== 单题操作 ====================

function openCreate() {
  editingDetail.value = null
  formMode.value = 'create'
  formVisible.value = true
}

async function openEdit(row: QuestionListItem) {
  try {
    editingDetail.value = await getQuestionDetail(row.id)
    formMode.value = 'edit'
    formVisible.value = true
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

function openAudit(target: { id: number; content: string }) {
  auditTarget.value = target
  auditVisible.value = true
}

async function openDetail(row: QuestionListItem) {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await getQuestionDetail(row.id)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row: QuestionListItem) {
  try {
    await ElMessageBox.confirm('删除后题目不可恢复，确定删除吗？', '删除题目', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
  } catch {
    return
  }
  try {
    await deleteQuestion(row.id)
    ElMessage.success('题目已删除')
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleStatus(target: { id: number }, action: 'on' | 'off') {
  const message =
    action === 'off'
      ? '下架后学员端将不再展示该题目，确定下架吗？'
      : '上架后题目将重新进入待审核队列，确定提交吗？'
  try {
    await ElMessageBox.confirm(message, action === 'off' ? '下架题目' : '上架题目', {
      type: 'warning',
      confirmButtonText: '确定'
    })
  } catch {
    return
  }
  try {
    await updateQuestionStatus(target.id, action)
    ElMessage.success(action === 'off' ? '题目已下架' : '题目已提交审核')
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleResubmit(target: { id: number }) {
  try {
    await ElMessageBox.confirm('重新提交后题目将回到待审核状态，确定提交吗？', '重新提交', {
      type: 'warning',
      confirmButtonText: '提交'
    })
  } catch {
    return
  }
  try {
    await resubmitQuestion(target.id)
    ElMessage.success('题目已重新提交审核')
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

// ==================== 批量操作 ====================

async function handleBatchStatus(action: 'on' | 'off') {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将对选中的 ${selectedIds.value.length} 道题目执行${action === 'on' ? '批量上架（进入待审核）' : '批量下架'}，确定继续吗？`,
      action === 'on' ? '批量上架' : '批量下架',
      { type: 'warning', confirmButtonText: '确定' }
    )
  } catch {
    return
  }
  try {
    const count = await batchUpdateQuestionStatus(selectedIds.value, action)
    ElMessage.success(`已处理 ${count} 道题目`)
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleBatchAudit() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将批量审核通过选中的 ${selectedIds.value.length} 道题目（仅待审核题目生效），确定继续吗？`,
      '批量审核通过',
      { type: 'warning', confirmButtonText: '通过' }
    )
  } catch {
    return
  }
  try {
    const count = await batchAuditQuestions(selectedIds.value, true)
    ElMessage.success(`已审核通过 ${count} 道题目`)
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `将删除选中的 ${selectedIds.value.length} 道题目，删除后不可恢复，确定继续吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
  } catch {
    return
  }
  try {
    const count = await batchDeleteQuestions(selectedIds.value)
    ElMessage.success(`已删除 ${count} 道题目`)
    handleSaved()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const params: QuestionFilter = {}
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.subjectId === 'number') params.subjectId = query.subjectId
    if (typeof query.stageId === 'number') params.stageId = query.stageId
    if (typeof query.type === 'number') params.type = query.type
    if (typeof query.difficulty === 'number') params.difficulty = query.difficulty
    if (typeof query.status === 'number') params.status = query.status
    await exportQuestions(params)
    ElMessage.success('导出完成')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    exporting.value = false
  }
}

// ==================== 详情抽屉操作 ====================

function handleDetailEdit(question: QuestionDetail) {
  detailVisible.value = false
  editingDetail.value = question
  formMode.value = 'edit'
  formVisible.value = true
}

function handleDetailAudit(question: QuestionDetail) {
  detailVisible.value = false
  openAudit(question)
}

function handleDetailStatus(question: QuestionDetail, action: 'on' | 'off') {
  detailVisible.value = false
  handleStatus(question, action)
}

function handleDetailResubmit(question: QuestionDetail) {
  detailVisible.value = false
  handleResubmit(question)
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

/* 筛选条件强制单行：控件自适应收缩，窗口过窄时横向滚动而不是换行 */
.filter-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 4px;

  :deep(.el-form-item) {
    flex: 1 1 120px;
    min-width: 120px;
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

  /* 关键词、阶段内容较长，分配更多宽度 */
  :deep(.el-form-item:first-child) {
    flex: 1.4 1 140px;
  }

  :deep(.el-form-item:nth-child(3)) {
    flex: 1.3 1 140px;
  }

  /* 查询/重置按钮组保持自身宽度，不参与拉伸 */
  :deep(.el-form-item:last-child) {
    flex: 0 0 auto;
    min-width: auto;
  }
}

.stat-grid--seven {
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

@media (max-width: 1500px) {
  .stat-grid--seven {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .stat-grid--seven {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.question-content {
  display: -webkit-box;
  overflow: hidden;
  line-height: 1.6;
  word-break: break-word;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.question-meta {
  margin-top: 4px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}
</style>
