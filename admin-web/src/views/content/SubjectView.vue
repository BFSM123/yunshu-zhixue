<template>
  <div class="page-container">
    <PageHeader title="学科管理" description="学科与阶段的增删改、排序、启停，删除前校验下级关联。">
      <template #actions>
        <el-button type="primary" v-permission="'subject:add'" @click="openCreateSubject">
          新增学科
        </el-button>
      </template>
    </PageHeader>

    <div class="stat-grid">
      <StatCard label="总学科数" :value="String(stats.totalSubjects)" />
      <StatCard label="有阶段学科" :value="String(stats.subjectsWithStages)" />
      <StatCard label="阶段总数" :value="String(stats.totalStages)" />
      <StatCard label="题目总数" :value="String(stats.totalQuestions)" />
    </div>

    <div v-loading="loading" class="subject-grid">
      <el-card v-for="(subject, subjectIndex) in subjects" :key="subject.id" shadow="never"
        class="subject-card">
        <div class="subject-head">
          <span class="subject-icon" :style="{ backgroundColor: subject.themeColor }">
            {{ subject.icon }}
          </span>
          <div class="subject-title">
            <div class="subject-name-row">
              <h3 class="subject-name">{{ subject.name }}</h3>
              <el-tag size="small" effect="plain" round>排序 {{ subject.sortOrder }}</el-tag>
            </div>
            <p class="subject-desc">{{ subject.description }}</p>
          </div>
        </div>

        <div class="subject-counts">
          <div class="count-item">
            <span class="count-value">{{ subject.courseCount }}</span>
            <span class="count-label">课程</span>
          </div>
          <div class="count-item">
            <span class="count-value">{{ subject.routeCount }}</span>
            <span class="count-label">路线</span>
          </div>
          <div class="count-item">
            <span class="count-value">{{ subject.questionCount }}</span>
            <span class="count-label">题目</span>
          </div>
        </div>

        <div class="stage-section">
          <div class="stage-head">
            <span class="stage-head__title">阶段（{{ subject.stages.length }}）</span>
            <el-button
              v-permission="'subject:add'"
              link
              type="primary"
              :icon="Plus"
              :disabled="actionPending"
              @click="openCreateStage(subject)"
            >
              添加阶段
            </el-button>
          </div>

          <div v-if="subject.stages.length" class="stage-list">
            <div
              v-for="(stage, stageIndex) in subject.stages"
              :key="stage.id"
              class="stage-row"
              :class="{ 'is-disabled': stage.status === 0 }"
            >
              <span class="stage-icon">{{ stage.icon || '📚' }}</span>
              <span class="stage-name">{{ stage.name }}</span>
              <el-tag size="small" :type="stage.status === 1 ? 'success' : 'info'">
                {{ stage.status === 1 ? '启用' : '禁用' }}
              </el-tag>
              <span class="stage-count">{{ stage.questionCount }} 题</span>

              <div class="stage-actions">
                <el-tooltip content="上移" placement="top">
                  <el-button
                    link
                    :icon="ArrowUp"
                    :disabled="actionPending || stageIndex === 0"
                    @click="moveStage(subject, stage, 'up')"
                  />
                </el-tooltip>
                <el-tooltip content="下移" placement="top">
                  <el-button
                    link
                    :icon="ArrowDown"
                    :disabled="actionPending || stageIndex === subject.stages.length - 1"
                    @click="moveStage(subject, stage, 'down')"
                  />
                </el-tooltip>
                <el-button
                  v-permission="'subject:edit'"
                  link
                  :type="stage.status === 1 ? 'warning' : 'success'"
                  :disabled="actionPending"
                  @click="toggleStageStatus(subject, stage)"
                >
                  {{ stage.status === 1 ? '禁用' : '启用' }}
                </el-button>
                <el-button
                  v-permission="'subject:edit'"
                  link
                  type="primary"
                  :disabled="actionPending"
                  @click="openEditStage(subject, stage)"
                >
                  编辑
                </el-button>
                <el-button
                  v-permission="'subject:delete'"
                  link
                  type="danger"
                  :disabled="actionPending"
                  @click="removeStage(subject, stage)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </div>
          <div v-else class="stage-empty">该学科暂无阶段</div>
        </div>

        <div class="subject-actions">
          <el-button
            v-permission="'subject:edit'"
            link
            :disabled="actionPending || subjectIndex === 0"
            @click="moveSubject(subject, 'up')"
          >
            上移
          </el-button>
          <el-button
            v-permission="'subject:edit'"
            link
            :disabled="actionPending || subjectIndex === subjects.length - 1"
            @click="moveSubject(subject, 'down')"
          >
            下移
          </el-button>
          <el-button
            v-permission="'subject:edit'"
            link
            type="primary"
            :disabled="actionPending"
            @click="openEditSubject(subject)"
          >
            编辑学科
          </el-button>
          <el-button
            v-permission="'subject:delete'"
            link
            type="danger"
            :disabled="actionPending"
            @click="removeSubject(subject)"
          >
            删除学科
          </el-button>
        </div>
      </el-card>

      <el-empty v-if="!loading && !subjects.length" description="暂无学科，点击右上角新增学科" />
    </div>

    <SubjectFormDialog
      v-model="subjectDialogVisible"
      :mode="subjectDialogMode"
      :subject="editingSubject"
      @success="load"
    />
    <StageFormDialog
      v-model="stageDialogVisible"
      :mode="stageDialogMode"
      :subject-id="stageSubjectId"
      :stage="editingStage"
      @success="load"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, ArrowUp, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import StatCard from '@/components/layout/StatCard.vue'
import SubjectFormDialog from '@/components/content/SubjectFormDialog.vue'
import StageFormDialog from '@/components/content/StageFormDialog.vue'
import type { StageItem, SubjectCard, SubjectStats } from '@/types/content'
import {
  deleteStage,
  deleteSubject,
  getSubjects,
  getSubjectStats,
  sortStage,
  sortSubject,
  updateStageStatus
} from '@/api/subject'

const loading = ref(false)
const actionPending = ref(false)
const subjects = ref<SubjectCard[]>([])
const stats = ref<SubjectStats>({
  totalSubjects: 0,
  subjectsWithStages: 0,
  totalStages: 0,
  totalQuestions: 0
})

const subjectDialogVisible = ref(false)
const subjectDialogMode = ref<'create' | 'edit'>('create')
const editingSubject = ref<SubjectCard | null>(null)

const stageDialogVisible = ref(false)
const stageDialogMode = ref<'create' | 'edit'>('create')
const stageSubjectId = ref<number | null>(null)
const editingStage = ref<StageItem | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    const [statsData, list] = await Promise.all([getSubjectStats(), getSubjects()])
    stats.value = statsData
    subjects.value = list
  } finally {
    loading.value = false
  }
}

function openCreateSubject() {
  subjectDialogMode.value = 'create'
  editingSubject.value = null
  subjectDialogVisible.value = true
}

function openEditSubject(subject: SubjectCard) {
  subjectDialogMode.value = 'edit'
  editingSubject.value = subject
  subjectDialogVisible.value = true
}

function openCreateStage(subject: SubjectCard) {
  stageDialogMode.value = 'create'
  stageSubjectId.value = subject.id
  editingStage.value = null
  stageDialogVisible.value = true
}

function openEditStage(subject: SubjectCard, stage: StageItem) {
  stageDialogMode.value = 'edit'
  stageSubjectId.value = subject.id
  editingStage.value = stage
  stageDialogVisible.value = true
}

async function moveSubject(subject: SubjectCard, move: 'up' | 'down') {
  await runAction(async () => {
    await sortSubject(subject.id, move)
  })
}

async function moveStage(subject: SubjectCard, stage: StageItem, move: 'up' | 'down') {
  await runAction(async () => {
    await sortStage(subject.id, stage.id, move)
  })
}

async function toggleStageStatus(subject: SubjectCard, stage: StageItem) {
  const disabling = stage.status === 1
  if (disabling) {
    try {
      await ElMessageBox.confirm(
        `停用后「${stage.name}」下的题目将不在学员端展示，确定停用吗？`,
        '停用阶段',
        { type: 'warning', confirmButtonText: '停用', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }
  await runAction(async () => {
    await updateStageStatus(subject.id, stage.id, disabling ? 0 : 1)
    ElMessage.success(disabling ? '阶段已停用' : '阶段已启用')
  })
}

async function removeStage(subject: SubjectCard, stage: StageItem) {
  if (stage.questionCount > 0) {
    await ElMessageBox.alert(
      `「${stage.name}」下关联 ${stage.questionCount} 道题目，无法删除。请先处理题目后再删除阶段。`,
      '无法删除',
      { type: 'warning', confirmButtonText: '知道了' }
    )
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除阶段「${stage.name}」吗？删除后不可恢复。`,
      '删除阶段',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await runAction(async () => {
    await deleteStage(subject.id, stage.id)
    ElMessage.success('阶段已删除')
  })
}

async function removeSubject(subject: SubjectCard) {
  const related = subject.stages.length + subject.courseCount
    + subject.routeCount + subject.questionCount
  if (related > 0) {
    await ElMessageBox.alert(buildSubjectDeleteMessage(subject), '无法删除', {
      type: 'warning',
      confirmButtonText: '知道了'
    })
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除学科「${subject.name}」吗？删除后不可恢复。`,
      '删除学科',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await runAction(async () => {
    await deleteSubject(subject.id)
    ElMessage.success('学科已删除')
  })
}

function buildSubjectDeleteMessage(subject: SubjectCard): string {
  const stageCount = subject.stages.length
  const parts: string[] = []
  if (stageCount > 0 && subject.questionCount > 0) {
    parts.push(`该学科下有 ${stageCount} 个阶段、${subject.questionCount} 道题目`)
  } else if (stageCount > 0) {
    parts.push(`该学科下仅有 ${stageCount} 个阶段`)
  } else {
    parts.push('该学科暂无阶段')
  }
  if (subject.courseCount > 0) {
    parts.push(`关联 ${subject.courseCount} 门课程`)
  }
  if (subject.routeCount > 0) {
    parts.push(`关联 ${subject.routeCount} 条学习路线`)
  }
  return `${parts.join('，')}，请先处理关联内容后再删除。`
}

async function runAction(action: () => Promise<void>) {
  if (actionPending.value) return
  actionPending.value = true
  try {
    await action()
    await load()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    actionPending.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.subject-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(430px, 1fr));
  gap: 16px;
  margin-top: 16px;
  min-height: 120px;
}

.subject-card {
  border-radius: 14px;
}

.subject-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px;
}

.subject-head {
  display: flex;
  gap: 14px;
}

.subject-icon {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  font-size: 24px;
  line-height: 1;
  color: #fff;
  border-radius: 14px;
  box-shadow: 0 6px 14px rgb(15 23 42 / 12%);
}

.subject-title {
  min-width: 0;
}

.subject-name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.subject-name {
  font-size: 17px;
  font-weight: 700;
}

.subject-desc {
  margin: 6px 0 0;
  overflow: hidden;
  display: -webkit-box;
  color: $yszx-text-secondary;
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.subject-counts {
  display: flex;
  gap: 28px;
  padding: 14px 0;
  margin: 16px 0 4px;
  border-top: 1px solid $yszx-border;
  border-bottom: 1px solid $yszx-border;
}

.count-item {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.count-value {
  font-size: 20px;
  font-weight: 700;
}

.count-label {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.stage-section {
  flex: 1;
}

.stage-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.stage-head__title {
  color: $yszx-text-secondary;
  font-size: 13px;
  font-weight: 600;
}

.stage-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stage-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f8fafc;
  border-radius: 8px;
  transition: background 0.15s ease;
}

.stage-row:hover {
  background: #f1f5f9;
}

.stage-row.is-disabled .stage-name {
  color: $yszx-text-secondary;
}

.stage-icon {
  flex-shrink: 0;
  font-size: 15px;
  line-height: 1;
}

.stage-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-count {
  flex-shrink: 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.stage-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 2px;
}

.stage-actions :deep(.el-button) {
  margin-left: 0;
  font-size: 12px;
}

.stage-empty {
  padding: 14px;
  color: $yszx-text-secondary;
  font-size: 13px;
  text-align: center;
  background: #f8fafc;
  border-radius: 8px;
}

.subject-actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
  padding-top: 14px;
  margin-top: 14px;
  border-top: 1px solid $yszx-border;
}

@media (max-width: 720px) {
  .subject-grid {
    grid-template-columns: 1fr;
  }
}
</style>
