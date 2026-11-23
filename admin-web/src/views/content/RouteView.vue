<template>
  <div class="page-container">
    <PageHeader title="学习路线" description="按学科维护学习路线、课程顺序与发布状态。">
      <template #actions>
        <el-button v-permission="'route:add'" type="primary" @click="openCreate">
          新建路线
        </el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="route-grid-wrap">
      <div v-if="routes.length" class="route-grid">
        <article v-for="item in routes" :key="item.id" class="route-card">
          <header class="route-card__header">
            <div class="route-card__icon">{{ item.icon || '📍' }}</div>
            <div class="route-card__heading">
              <h3 class="route-card__name" :title="item.name">{{ item.name }}</h3>
              <div class="route-card__tags">
                <el-tag size="small" effect="plain">{{ item.subjectName }}</el-tag>
                <el-tag
                  size="small"
                  :type="item.status === 1 ? 'success' : 'info'"
                  effect="plain"
                >
                  {{ item.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </div>
            </div>
          </header>

          <p class="route-card__desc">{{ item.description || '暂无路线描述' }}</p>

          <div class="route-card__stats">
            <span><strong>{{ item.courseCount }}</strong> 门课程</span>
            <span><strong>{{ formatDuration(item.totalDurationMinutes) }}</strong></span>
            <span v-if="item.targetEducation">{{ item.targetEducation }}</span>
          </div>

          <div class="route-card__courses">
            <div class="route-card__courses-title">课程预览</div>
            <ol v-if="item.previewCourses.length" class="route-course-list">
              <li v-for="course in item.previewCourses" :key="course.routeCourseId">
                <span class="route-course-list__name" :title="course.name">
                  {{ course.name }}
                </span>
                <span class="route-course-list__meta">
                  {{ formatDuration(course.durationMinutes) }}
                </span>
              </li>
            </ol>
            <div v-else class="route-card__empty">暂无课程</div>
            <div v-if="item.courseCount > item.previewCourses.length" class="route-card__more">
              等 {{ item.courseCount }} 门课程
            </div>
          </div>

          <footer class="route-card__footer">
            <el-button link type="primary" @click="openDetail(item)">课程管理</el-button>
            <el-button link @click="openPreview(item)">预览</el-button>
            <el-button v-permission="'route:edit'" link @click="openEdit(item)">编辑</el-button>
            <el-button v-permission="'route:edit'" link @click="toggleStatus(item)">
              {{ item.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button
              v-permission="'route:delete'"
              link
              type="danger"
              @click="remove(item)"
            >
              删除
            </el-button>
          </footer>
        </article>
      </div>

      <el-card v-else-if="!loading" shadow="never">
        <el-empty description="暂无学习路线，点击上方新建路线创建一条" :image-size="90" />
      </el-card>
    </div>

    <RouteFormDialog
      v-model="formVisible"
      :mode="formMode"
      :route="editingRoute"
      :subjects="subjectOptions"
      @success="load"
    />
    <RoutePreviewDialog v-model="previewVisible" :route-id="previewRouteId" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/layout/PageHeader.vue'
import RouteFormDialog from '@/components/content/RouteFormDialog.vue'
import RoutePreviewDialog from '@/components/content/RoutePreviewDialog.vue'
import type { RouteItem } from '@/types/content'
import { deleteRoute, getRoutes, updateRouteStatus } from '@/api/route'
import { getSubjects } from '@/api/subject'

const router = useRouter()

const loading = ref(false)
const routes = ref<RouteItem[]>([])
const subjectOptions = ref<Array<{ id: number; name: string }>>([])

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingRoute = ref<RouteItem | null>(null)

const previewVisible = ref(false)
const previewRouteId = ref<number | null>(null)

onMounted(async () => {
  await Promise.all([load(), loadSubjects()])
})

async function load() {
  loading.value = true
  try {
    routes.value = await getRoutes()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function loadSubjects() {
  try {
    const subjects = await getSubjects()
    subjectOptions.value = subjects.map((subject) => ({ id: subject.id, name: subject.name }))
  } catch {
    subjectOptions.value = []
  }
}

function openCreate() {
  formMode.value = 'create'
  editingRoute.value = null
  formVisible.value = true
}

function openEdit(item: RouteItem) {
  formMode.value = 'edit'
  editingRoute.value = item
  formVisible.value = true
}

function openDetail(item: RouteItem) {
  router.push(`/content/routes/${item.id}`)
}

function openPreview(item: RouteItem) {
  previewRouteId.value = item.id
  previewVisible.value = true
}

async function toggleStatus(item: RouteItem) {
  const nextStatus = item.status === 1 ? 0 : 1
  try {
    await updateRouteStatus(item.id, nextStatus)
    ElMessage.success(nextStatus === 1 ? '路线已启用' : '路线已禁用')
    load()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

async function remove(item: RouteItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除路线「${item.name}」吗？路线下的课程关联与学员完成记录将一并删除。`,
      '删除学习路线',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteRoute(item.id)
    ElMessage.success('学习路线已删除')
    load()
  } catch {
    // 错误提示由请求拦截器统一处理
  }
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  if (hours === 0) return `${rest} 分钟`
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.route-grid-wrap {
  min-height: 200px;
}

.route-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.route-card {
  display: flex;
  flex-direction: column;
  padding: 18px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 12px;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
    transform: translateY(-2px);
  }
}

.route-card__header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.route-card__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  font-size: 22px;
  background: #eff6ff;
  border-radius: 11px;
}

.route-card__heading {
  min-width: 0;
}

.route-card__name {
  margin: 2px 0 6px;
  overflow: hidden;
  color: $yszx-text;
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.route-card__desc {
  display: -webkit-box;
  margin: 12px 0;
  overflow: hidden;
  min-height: 40px;
  color: $yszx-text-secondary;
  font-size: 13px;
  line-height: 1.6;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.route-card__stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 14px;
  padding: 10px 0;
  color: $yszx-text-secondary;
  font-size: 12px;
  border-top: 1px dashed $yszx-border;
  border-bottom: 1px dashed $yszx-border;

  strong {
    color: $yszx-text;
    font-size: 14px;
  }
}

.route-card__courses {
  flex: 1;
  margin-top: 10px;
}

.route-card__courses-title {
  margin-bottom: 6px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.route-course-list {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    padding: 4px 0;
    font-size: 13px;
  }
}

.route-course-list__name {
  overflow: hidden;
  color: $yszx-text;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-course-list__meta {
  flex-shrink: 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.route-card__empty {
  padding: 8px 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.route-card__more {
  margin-top: 4px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.route-card__footer {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid $yszx-border;

  :deep(.el-button) {
    margin-left: 0;
  }
}

@media (max-width: 1280px) {
  .route-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .route-grid {
    grid-template-columns: 1fr;
  }
}
</style>
