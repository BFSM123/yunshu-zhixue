<template>
  <div class="page-container" v-loading="loading">
    <el-button link class="back-link" @click="router.push('/content/routes')">
      <el-icon><ArrowLeft /></el-icon>
      返回列表
    </el-button>

    <PageHeader
      :title="detail ? `${detail.name} - 课程管理` : '路线课程管理'"
      description="课程顺序即学习顺序，学员按列表依次学习。"
    >
      <template #actions>
        <el-button
          v-if="detail"
          v-permission="'route:edit'"
          type="primary"
          @click="pickerVisible = true"
        >
          从课程库添加
        </el-button>
      </template>
    </PageHeader>

    <template v-if="detail">
      <el-card shadow="never" class="info-card">
        <el-descriptions :column="4" border>
          <el-descriptions-item label="关联学科">
            {{ detail.subjectName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detail.status === 1 ? 'success' : 'info'" size="small" effect="plain">
              {{ detail.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="适合学历">
            {{ detail.targetEducation || '不限' }}
          </el-descriptions-item>
          <el-descriptions-item label="课程 / 时长">
            {{ detail.courseCount }} 门 · {{ formatDuration(detail.totalDurationMinutes) }}
          </el-descriptions-item>
          <el-descriptions-item label="路线描述" :span="4">
            <span class="desc-text">{{ detail.description || '暂无描述' }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="never" class="table-card">
        <el-table :data="detail.courses">
          <el-table-column label="序号" width="70">
            <template #default="{ row }">{{ row.sortOrder }}</template>
          </el-table-column>
          <el-table-column label="BV号" width="140">
            <template #default="{ row }">
              <span class="course-meta">{{ row.bvid || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="课程名称" min-width="280">
            <template #default="{ row }">
              <div class="course-cell">
                <el-image
                  v-if="row.coverUrl"
                  :src="row.coverUrl"
                  fit="cover"
                  class="course-cover"
                >
                  <template #error>
                    <div class="course-cover course-cover--empty">失效</div>
                  </template>
                </el-image>
                <div v-else class="course-cover course-cover--empty">
                  <el-icon><VideoCamera /></el-icon>
                </div>
                <span class="course-name">{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="时长" width="120">
            <template #default="{ row }">{{ formatDuration(row.durationMinutes) }}</template>
          </el-table-column>
          <el-table-column label="来源" width="90">
            <template #default="{ row }">
              <el-tag :type="row.source === 1 ? 'danger' : 'info'" size="small" effect="plain">
                {{ row.source === 1 ? 'B站' : '其他' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="学科" min-width="150">
            <template #default="{ row }">
              <span>{{ row.subjectName }}</span>
              <el-tag v-if="row.otherSubject" size="small" type="warning" effect="plain">
                跨学科
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="110">
            <template #default="{ $index }">
              <el-button
                v-permission="'route:edit'"
                link
                :disabled="$index === 0 || sorting"
                @click="move($index, -1)"
              >
                ↑
              </el-button>
              <el-button
                v-permission="'route:edit'"
                link
                :disabled="$index === detail.courses.length - 1 || sorting"
                @click="move($index, 1)"
              >
                ↓
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button
                v-permission="'route:edit'"
                link
                type="danger"
                @click="remove(row as RouteCourseItem)"
              >
                移除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无课程，点击右上角从课程库添加" :image-size="80" />
          </template>
        </el-table>
      </el-card>

      <RouteCoursePickerDialog
        v-model="pickerVisible"
        :route-id="detail.id"
        :route-subject-id="detail.subjectId"
        :added-course-ids="detail.courses.map((course) => course.courseId)"
        @success="load"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, VideoCamera } from '@element-plus/icons-vue'
import PageHeader from '@/components/layout/PageHeader.vue'
import RouteCoursePickerDialog from '@/components/content/RouteCoursePickerDialog.vue'
import type { RouteCourseItem, RouteDetail } from '@/types/content'
import {
  getRouteDetail,
  removeRouteCourse,
  sortRouteCourses
} from '@/api/route'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const sorting = ref(false)
const detail = ref<RouteDetail | null>(null)
const pickerVisible = ref(false)

const routeId = Number(route.params.id)

onMounted(load)

async function load() {
  loading.value = true
  try {
    detail.value = await getRouteDetail(routeId)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function move(index: number, offset: number) {
  if (!detail.value) return
  const courses = detail.value.courses
  const target = index + offset
  if (target < 0 || target >= courses.length) return

  const ordered = [...courses]
  const [moved] = ordered.splice(index, 1)
  ordered.splice(target, 0, moved)

  sorting.value = true
  try {
    await sortRouteCourses(routeId, ordered.map((course) => course.routeCourseId))
    await load()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    sorting.value = false
  }
}

async function remove(course: RouteCourseItem) {
  try {
    await ElMessageBox.confirm(
      `确定从路线中移除课程「${course.name}」吗？该课程的学员完成记录将一并删除。`,
      '移除课程',
      { type: 'warning', confirmButtonText: '移除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await removeRouteCourse(routeId, course.courseId)
    ElMessage.success('课程已移除')
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

.back-link {
  margin-bottom: 8px;
  color: $yszx-text-secondary;
}

.info-card {
  margin-bottom: 16px;
}

.desc-text {
  color: $yszx-text-secondary;
  line-height: 1.7;
}

.course-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.course-cover {
  flex-shrink: 0;
  width: 72px;
  height: 41px;
  border-radius: 6px;
}

.course-cover--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: $yszx-text-secondary;
  font-size: 14px;
  background: #f1f5f9;
}

.course-name {
  font-weight: 600;
}

.course-meta {
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>
