<template>
  <PageShell
    title="操作日志"
    description="登录、新增、修改、删除、导出等操作的审计记录。"
  >
    <template #actions>
      <el-button :loading="exporting" @click="handleExport">导出 CSV</el-button>
      <el-button type="danger" plain @click="handleClear">清空日志</el-button>
    </template>

    <template #filters>
      <el-form inline class="filter-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="用户 / 操作 / 目标"
            clearable
            style="width: 200px"
            @keyup.enter="search"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="query.operationType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="item in operationTypes"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </template>

    <el-table
      v-loading="loading"
      :data="rows"
      class="log-table"
      @row-click="openDetail"
    >
      <el-table-column prop="username" label="用户" min-width="120" />
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-tag :type="actionType(row.action)" size="small" effect="plain">
            {{ row.action }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" min-width="210">
        <template #default="{ row }">
          <span>{{ row.targetType }}</span>
          <span class="muted"> · {{ row.targetName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP 地址" min-width="150" />
      <el-table-column prop="createdAt" label="时间" min-width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="openDetail(row)">详情</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无匹配的日志记录" :image-size="80" />
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

  <LogDetailDrawer v-model="detailVisible" :log-id="detailId" />
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/layout/PageShell.vue'
import LogDetailDrawer from '@/components/system/LogDetailDrawer.vue'
import type { OperationLogQuery, OperationLogVO } from '@/types/system'
import { clearLogs, exportLogs, getLogs } from '@/api/log'

const operationTypes = ['登录', '登出', '新增', '修改', '删除', '禁用启用', '导出', '其他']

const loading = ref(false)
const exporting = ref(false)
const rows = ref<OperationLogVO[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const detailId = ref<number | null>(null)

const query = reactive({
  keyword: '',
  operationType: undefined as string | undefined,
  page: 1,
  pageSize: 10
})

onMounted(load)

async function load() {
  loading.value = true
  try {
    const result = await getLogs(buildParams(true) as unknown as OperationLogQuery)
    rows.value = result.list
    total.value = result.total
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function buildParams(withPage: boolean) {
  const params: Record<string, unknown> = {}
  if (query.keyword.trim()) params.keyword = query.keyword.trim()
  if (query.operationType) params.operationType = query.operationType
  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  if (withPage) {
    params.page = query.page
    params.pageSize = query.pageSize
  }
  return params
}

function search() {
  query.page = 1
  load()
}

function reset() {
  query.keyword = ''
  query.operationType = undefined
  dateRange.value = null
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function openDetail(row: unknown) {
  detailId.value = (row as OperationLogVO).id
  detailVisible.value = true
}

async function handleExport() {
  exporting.value = true
  try {
    await exportLogs(
      buildParams(false) as unknown as Omit<OperationLogQuery, 'page' | 'pageSize'>
    )
    ElMessage.success('日志已导出')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    exporting.value = false
  }
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('清空后所有日志将被物理删除且不可恢复，确定继续吗？', '清空日志', {
      type: 'warning',
      confirmButtonText: '清空',
      confirmButtonClass: 'el-button--danger'
    })
    await clearLogs()
    ElMessage.success('日志已清空')
    query.page = 1
    load()
  } catch {
    // 取消或接口异常
  }
}

function actionType(action: string): 'success' | 'warning' | 'danger' | 'info' {
  if (['新增'].includes(action)) return 'success'
  if (['修改', '禁用启用'].includes(action)) return 'warning'
  if (['删除'].includes(action)) return 'danger'
  return 'info'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.log-table {
  :deep(.el-table__row) {
    cursor: pointer;
  }
}

.muted {
  color: $yszx-text-secondary;
}
</style>
