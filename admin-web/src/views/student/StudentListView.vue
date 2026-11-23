<template>
  <PageShell
    title="学员管理"
    description="学员筛选、启禁用、详情学习数据与面试记录。"
  >
    <template #actions>
      <el-button
        v-permission="'student:view'"
        :loading="exporting"
        @click="handleExport"
      >
        导出学员列表
      </el-button>
    </template>

    <template #filters>
      <el-form inline class="filter-form" @submit.prevent>
        <el-form-item label="昵称/手机号">
          <el-input
            v-model="query.keyword"
            placeholder="昵称或手机号"
            clearable
            style="width: 190px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="已启用" :value="1" />
            <el-option label="已禁用" :value="0" />
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
          v-permission="'student:edit'"
          :disabled="!selection.length"
          @click="handleBatchStatus(1)"
        >
          批量启用
        </el-button>
        <el-button
          v-permission="'student:edit'"
          :disabled="!selection.length"
          @click="handleBatchStatus(0)"
        >
          批量禁用
        </el-button>
        <el-button
          v-permission="'student:delete'"
          type="danger"
          plain
          :disabled="!selection.length"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>
      <span class="table-toolbar__hint">已选 {{ selection.length }} 项</span>
    </template>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      :fit="false"
      scrollbar-always-on
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" reserve-selection />
      <el-table-column label="学员" min-width="140">
        <template #default="{ row }">
          <div class="student-cell">
            <el-avatar :size="30" :src="row.avatar || undefined" class="student-cell__avatar">
              {{ row.nickname.slice(0, 1) }}
            </el-avatar>
            <span class="student-cell__name">{{ row.nickname }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="mobile" label="手机号" width="115" />
      <el-table-column label="注册时间" width="165">
        <template #default="{ row }">
          <span class="muted nowrap">{{ row.createdAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="学习天数" width="88">
        <template #default="{ row }">{{ row.studyDays }} 天</template>
      </el-table-column>
      <el-table-column label="刷题 · 面试" min-width="125">
        <template #default="{ row }">
          刷题 {{ row.practiceCount }} · 面试 {{ row.interviewCount }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="86">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(asStudent(row))">详情</el-button>
          <el-button
            v-permission="'student:edit'"
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(asStudent(row))"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button
            v-permission="'student:delete'"
            link
            type="danger"
            @click="handleDelete(asStudent(row))"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有找到匹配的学员" :image-size="80" />
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

  <StudentDetailDrawer v-model="detailVisible" :student-id="detailStudentId" />
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import PageShell from '@/components/layout/PageShell.vue'
import StudentDetailDrawer from '@/components/student/StudentDetailDrawer.vue'
import type { StudentQuery, StudentVO } from '@/types/student'
import {
  batchDeleteStudents,
  deleteStudent,
  exportStudents,
  getStudents,
  updateStudentsStatusBatch,
  updateStudentStatus
} from '@/api/student'

const tableRef = ref<TableInstance>()
const loading = ref(false)
const exporting = ref(false)
const rows = ref<StudentVO[]>([])
const total = ref(0)
const selection = ref<StudentVO[]>([])
const detailVisible = ref(false)
const detailStudentId = ref<number | null>(null)

const query = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  pageSize: 10
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const params: StudentQuery = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.status === 'number') params.status = query.status
    const result = await getStudents(params)
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
  query.status = undefined
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function handleSelectionChange(value: StudentVO[]) {
  selection.value = value
}

function asStudent(row: unknown): StudentVO {
  return row as StudentVO
}

function openDetail(row: StudentVO) {
  detailStudentId.value = row.id
  detailVisible.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    const params: { keyword?: string; status?: number } = {}
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.status === 'number') params.status = query.status
    await exportStudents(params)
    ElMessage.success('导出成功')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    exporting.value = false
  }
}

async function toggleStatus(row: StudentVO) {
  const target = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${target}学员「${row.nickname}」吗？`, '状态变更', {
      type: 'warning'
    })
    await updateStudentStatus(row.id, row.status === 1 ? 0 : 1)
    ElMessage.success(`已${target}`)
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleBatchStatus(status: number) {
  if (!selection.value.length) return
  const target = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${target}选中的 ${selection.value.length} 名学员吗？`,
      '批量操作',
      { type: 'warning' }
    )
    await updateStudentsStatusBatch(selection.value.map((item) => item.id), status)
    ElMessage.success(`已批量${target}`)
    tableRef.value?.clearSelection()
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleDelete(row: StudentVO) {
  try {
    await ElMessageBox.confirm(`确定要删除学员「${row.nickname}」吗？`, '删除学员', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteStudent(row.id)
    ElMessage.success('学员已删除')
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleBatchDelete() {
  if (!selection.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selection.value.length} 名学员吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
    await batchDeleteStudents(selection.value.map((item) => item.id))
    ElMessage.success('已批量删除')
    tableRef.value?.clearSelection()
    load()
  } catch {
    // 取消或接口异常
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;

  :deep(.el-form-item) {
    margin-right: 12px;
    margin-bottom: 0;
  }
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-cell__avatar {
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.student-cell__name {
  font-weight: 500;
}

.muted {
  color: $yszx-text-secondary;
}

.nowrap {
  white-space: nowrap;
}
</style>