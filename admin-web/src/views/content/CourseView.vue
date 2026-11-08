<template>
  <PageShell
    title="B站课程库"
    description="课程筛选、新增编辑、批量导入与「其他学科」兜底。"
    pagination
  >
    <template #actions>
      <el-button v-permission="'course:add'" @click="importVisible = true">批量导入</el-button>
      <el-button v-permission="'course:add'" type="primary" @click="openCreate">新增课程</el-button>
    </template>

    <template #summary>
      <div class="stat-grid stat-grid--three">
        <StatCard label="课程总数" :value="String(stats.totalCourses)" />
        <StatCard label="总时长（小时）" :value="String(stats.totalDurationHours)" />
        <StatCard label="关联学科数" :value="String(stats.relatedSubjectCount)" />
      </div>
    </template>

    <template #filters>
      <el-form inline class="filter-form" @submit.prevent>
        <el-form-item label="课程标题">
          <el-input
            v-model="query.keyword"
            placeholder="课程名称 / BV 号"
            clearable
            style="width: 220px"
          />
        </el-form-item>
        <el-form-item label="学科">
          <el-select
            v-model="query.subjectId"
            placeholder="全部学科"
            clearable
            style="width: 180px"
            @change="search"
          >
            <el-option
              v-for="option in subjectOptions"
              :key="option.id"
              :label="option.name"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源">
          <el-select
            v-model="query.source"
            placeholder="全部来源"
            clearable
            style="width: 140px"
            @change="search"
          >
            <el-option label="B站" :value="1" />
            <el-option label="其他" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </template>

    <el-table v-loading="loading" :data="rows">
      <el-table-column label="序号" width="64">
        <template #default="{ $index }">
          {{ (query.page - 1) * query.pageSize + $index + 1 }}
        </template>
      </el-table-column>
      <el-table-column label="封面" width="116">
        <template #default="{ row }">
          <el-image
            v-if="row.coverUrl"
            :src="row.coverUrl"
            fit="cover"
            class="course-cover"
            :preview-src-list="[row.coverUrl]"
            preview-teleported
          >
            <template #error>
              <div class="course-cover course-cover--empty">失效</div>
            </template>
          </el-image>
          <div v-else class="course-cover course-cover--empty">
            <el-icon><VideoCamera /></el-icon>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="课程名称" min-width="240">
        <template #default="{ row }">
          <div class="course-name">{{ row.name }}</div>
          <div v-if="row.bvid" class="course-meta">{{ row.bvid }}</div>
        </template>
      </el-table-column>
      <el-table-column label="时长" width="140">
        <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
      </el-table-column>
      <el-table-column label="学科" min-width="160">
        <template #default="{ row }">
          <div>{{ row.subjectName }}</div>
          <div v-if="row.subjectNames.length" class="course-meta">
            关联：{{ row.subjectNames.join('、') }}
          </div>
        </template>
      </el-table-column>
      <el-table-column label="来源" width="90">
        <template #default="{ row }">
          <el-tag :type="row.source === 1 ? 'danger' : 'info'" size="small" effect="plain">
            {{ row.source === 1 ? 'B站' : '其他' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button
            v-permission="'course:edit'"
            link
            type="primary"
            @click="openEdit(row as CourseItem)"
          >
            编辑
          </el-button>
          <el-button
            v-permission="'course:delete'"
            link
            type="danger"
            @click="remove(row as CourseItem)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无匹配的课程，试试调整筛选条件" :image-size="80" />
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

  <CourseFormDialog
    v-model="formVisible"
    :mode="formMode"
    :course="editingCourse"
    :subjects="subjects"
    @success="load"
  />
  <CourseImportDialog v-model="importVisible" @success="load" />
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { VideoCamera } from '@element-plus/icons-vue'
import PageShell from '@/components/layout/PageShell.vue'
import StatCard from '@/components/layout/StatCard.vue'
import CourseFormDialog from '@/components/content/CourseFormDialog.vue'
import CourseImportDialog from '@/components/content/CourseImportDialog.vue'
import type { CourseItem, CourseStats, SubjectCard } from '@/types/content'
import type { CourseQuery } from '@/api/course'
import { deleteCourse, getCourses, getCourseStats } from '@/api/course'
import { getSubjects } from '@/api/subject'

const loading = ref(false)
const rows = ref<CourseItem[]>([])
const total = ref(0)
const stats = ref<CourseStats>({
  totalCourses: 0,
  totalDurationHours: 0,
  relatedSubjectCount: 0
})
const subjects = ref<SubjectCard[]>([])

const query = reactive<{
  keyword: string
  subjectId: number | undefined
  source: number | undefined
  page: number
  pageSize: number
}>({
  keyword: '',
  subjectId: undefined,
  source: undefined,
  page: 1,
  pageSize: 10
})

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingCourse = ref<CourseItem | null>(null)
const importVisible = ref(false)

const subjectOptions = computed(() => [
  ...subjects.value.map((subject) => ({ id: subject.id, name: subject.name })),
  { id: 0, name: '其他学科' }
])

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

async function load() {
  loading.value = true
  try {
    const params: CourseQuery = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.subjectId === 'number') params.subjectId = query.subjectId
    if (typeof query.source === 'number') params.source = query.source

    const [statsData, result] = await Promise.all([getCourseStats(), getCourses(params)])
    stats.value = statsData
    rows.value = result.list
    total.value = result.total
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.subjectId = undefined
  query.source = undefined
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function openCreate() {
  formMode.value = 'create'
  editingCourse.value = null
  formVisible.value = true
}

function openEdit(course: CourseItem) {
  formMode.value = 'edit'
  editingCourse.value = course
  formVisible.value = true
}

async function remove(course: CourseItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除课程「${course.name}」吗？删除后不可恢复。`,
      '删除课程',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await deleteCourse(course.id)
    ElMessage.success('课程已删除')
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

.stat-grid--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.course-cover {
  width: 88px;
  height: 50px;
  border-radius: 6px;
}

.course-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $yszx-text-secondary;
  font-size: 16px;
  background: #f1f5f9;
}

.course-name {
  font-weight: 600;
}

.course-meta {
  margin-top: 2px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

@media (max-width: 1100px) {
  .stat-grid--three {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .stat-grid--three {
    grid-template-columns: 1fr;
  }
}
</style>
