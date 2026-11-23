<template>
  <el-dialog
    :model-value="modelValue"
    title="从课程库添加"
    width="760px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-input
      v-model="keyword"
      placeholder="搜索课程名称 / BV 号"
      clearable
      class="search-input"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
    </el-input>

    <div v-loading="loading" class="course-picker">
      <template v-if="courses.length">
        <section v-for="group in groups" :key="group.title" class="course-group">
          <div class="course-group__title">
            {{ group.title }}
            <span class="course-group__count">{{ group.courses.length }}</span>
          </div>
          <el-checkbox-group v-model="selectedIds">
            <div v-for="course in group.courses" :key="course.id" class="course-row">
              <el-checkbox :value="course.id" :disabled="isAdded(course.id)">
                <div class="course-row__body">
                  <span class="course-row__name">{{ course.name }}</span>
                  <span class="course-row__meta">
                    <template v-if="course.bvid">{{ course.bvid }} · </template>
                    {{ formatDuration(course.durationMinutes) }}
                    <template v-if="course.subjectName"> · {{ course.subjectName }}</template>
                  </span>
                </div>
              </el-checkbox>
              <el-tag v-if="isAdded(course.id)" size="small" type="info" effect="plain">
                已添加
              </el-tag>
            </div>
          </el-checkbox-group>
        </section>
      </template>
      <el-empty
        v-else-if="!loading"
        :description="keyword ? '没有匹配的课程' : '课程库暂无课程'"
        :image-size="80"
      />
      <p v-if="resultTotal > courses.length" class="result-hint">
        共 {{ resultTotal }} 门课程，当前展示前 {{ courses.length }} 门，请输入关键词缩小范围。
      </p>
    </div>

    <template #footer>
      <span class="selected-count">已选 {{ selectedIds.length }} 门</span>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="selectedIds.length === 0"
        @click="submit"
      >
        添加所选
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getCourses } from '@/api/course'
import { addRouteCourses } from '@/api/route'
import type { CourseItem } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
  routeId: number
  routeSubjectId: number
  addedCourseIds: number[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const PAGE_SIZE = 100

const loading = ref(false)
const saving = ref(false)
const keyword = ref('')
const courses = ref<CourseItem[]>([])
const resultTotal = ref(0)
const selectedIds = ref<number[]>([])

let searchTimer: ReturnType<typeof setTimeout> | undefined

const addedIdSet = computed(() => new Set(props.addedCourseIds))

const currentSubjectCourses = computed(() =>
  courses.value.filter((course) => belongsToRouteSubject(course))
)

const otherSubjectCourses = computed(() =>
  courses.value.filter((course) => !belongsToRouteSubject(course))
)

const groups = computed(() =>
  [
    { title: '本学科课程', courses: currentSubjectCourses.value },
    { title: '其他学科课程（跨学科补充）', courses: otherSubjectCourses.value }
  ].filter((group) => group.courses.length > 0)
)

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    keyword.value = ''
    selectedIds.value = []
    loadCourses()
  }
)

watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(loadCourses, 300)
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})

function belongsToRouteSubject(course: CourseItem): boolean {
  return course.subjectId === props.routeSubjectId
    || course.subjectIds.includes(props.routeSubjectId)
}

function isAdded(courseId: number): boolean {
  return addedIdSet.value.has(courseId)
}

async function loadCourses() {
  loading.value = true
  try {
    const result = await getCourses({
      keyword: keyword.value.trim() || undefined,
      page: 1,
      pageSize: PAGE_SIZE
    })
    courses.value = result.list
    resultTotal.value = result.total
    // 移除已提交或已失效的选择
    const visibleIds = new Set(result.list.map((course) => course.id))
    selectedIds.value = selectedIds.value.filter(
      (id) => visibleIds.has(id) && !isAdded(id)
    )
  } catch {
    courses.value = []
    resultTotal.value = 0
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (selectedIds.value.length === 0) return
  saving.value = true
  try {
    await addRouteCourses(props.routeId, selectedIds.value)
    ElMessage.success(`已添加 ${selectedIds.value.length} 门课程`)
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
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

.search-input {
  margin-bottom: 12px;
}

.course-picker {
  max-height: 460px;
  min-height: 160px;
  overflow-y: auto;
  padding-right: 4px;
}

.course-group + .course-group {
  margin-top: 16px;
}

.course-group__title {
  margin-bottom: 6px;
  color: $yszx-text;
  font-size: 13px;
  font-weight: 600;
}

.course-group__count {
  margin-left: 6px;
  color: $yszx-text-secondary;
  font-weight: 400;
}

.course-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 10px;
  border-radius: 8px;

  &:hover {
    background: #f8fafc;
  }

  :deep(.el-checkbox) {
    flex: 1;
    min-width: 0;
    height: auto;
  }
}

.course-row__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.4;
}

.course-row__name {
  overflow: hidden;
  color: $yszx-text;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-row__meta {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.result-hint {
  margin: 12px 0 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.selected-count {
  margin-right: auto;
  color: $yszx-text-secondary;
  font-size: 13px;
}
</style>
