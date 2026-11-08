<template>
  <PageShell
    title="用户管理"
    description="后台账号的新增、编辑、启禁用、重置密码与删除。"
  >
    <template #filters>
      <div class="filter-row">
        <el-form inline class="filter-form" @submit.prevent>
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              placeholder="用户名 / 手机号"
              clearable
              style="width: 160px"
              @keyup.enter="search"
            />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="query.roleId" placeholder="全部角色" clearable style="width: 130px">
              <el-option
                v-for="role in roleOptions"
                :key="role.id"
                :label="role.name"
                :value="role.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 110px">
              <el-option label="已启用" :value="1" />
              <el-option label="已禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>

        <div class="filter-actions">
          <el-button :disabled="!selection.length" @click="handleBatchStatus(1)">
            批量启用
          </el-button>
          <el-button :disabled="!selection.length" @click="handleBatchStatus(0)">
            批量禁用
          </el-button>
          <el-button type="danger" plain :disabled="!selection.length" @click="handleBatchDelete">
            批量删除
          </el-button>
          <el-button type="primary" @click="openCreate">新增用户</el-button>
        </div>
      </div>
    </template>

    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="46" reserve-selection />
      <el-table-column label="用户" min-width="170">
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="30" class="user-cell__avatar">
              {{ row.username.slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div class="user-cell__name">
              <span>{{ row.username }}</span>
              <el-tag v-if="row.isBuiltin" size="small" type="warning" effect="plain">
                内置
              </el-tag>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="mobile" label="手机号" min-width="130" />
      <el-table-column prop="roleName" label="角色" min-width="120" />
      <el-table-column label="状态" width="96">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '已启用' : '已禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后登录" min-width="160">
        <template #default="{ row }">
          <span class="muted">{{ row.lastLoginAt || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(asUser(row))">详情</el-button>
          <el-button link type="primary" @click="openEdit(asUser(row))">编辑</el-button>
          <el-button
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            @click="toggleStatus(asUser(row))"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="primary" @click="handleResetPassword(asUser(row))">
            重置密码
          </el-button>
          <el-button link type="danger" :disabled="row.isBuiltin" @click="handleDelete(asUser(row))">
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有找到匹配的用户" :image-size="80" />
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

  <UserFormDialog
    v-model="formVisible"
    :mode="formMode"
    :user="editingUser"
    :role-options="roleOptions"
    @success="handleFormSuccess"
  />
  <UserDetailDrawer v-model="detailVisible" :user-id="detailUserId" />
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { TableInstance } from 'element-plus'
import PageShell from '@/components/layout/PageShell.vue'
import UserFormDialog from '@/components/system/UserFormDialog.vue'
import UserDetailDrawer from '@/components/system/UserDetailDrawer.vue'
import type { RoleOption, UserVO } from '@/types/system'
import type { UserQuery } from '@/api/user'
import {
  batchDeleteUsers,
  deleteUser,
  getUsers,
  resetUserPassword,
  updateUserStatusBatch
} from '@/api/user'
import { getRoleOptions } from '@/api/role'

const tableRef = ref<TableInstance>()
const loading = ref(false)
const rows = ref<UserVO[]>([])
const total = ref(0)
const selection = ref<UserVO[]>([])
const roleOptions = ref<RoleOption[]>([])

const query = reactive({
  keyword: '',
  roleId: undefined as number | undefined,
  status: undefined as number | undefined,
  page: 1,
  pageSize: 10
})

const formVisible = ref(false)
const formMode = ref<'create' | 'edit'>('create')
const editingUser = ref<UserVO | null>(null)
const detailVisible = ref(false)
const detailUserId = ref<number | null>(null)

onMounted(async () => {
  await loadRoleOptions()
  await load()
})

async function loadRoleOptions() {
  try {
    roleOptions.value = await getRoleOptions()
  } catch {
    roleOptions.value = []
  }
}

async function load() {
  loading.value = true
  try {
    const params: UserQuery = { page: query.page, pageSize: query.pageSize }
    if (query.keyword.trim()) params.keyword = query.keyword.trim()
    if (typeof query.roleId === 'number') params.roleId = query.roleId
    if (typeof query.status === 'number') params.status = query.status
    const result = await getUsers(params)
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
  query.roleId = undefined
  query.status = undefined
  query.page = 1
  load()
}

function handleSizeChange() {
  query.page = 1
  load()
}

function handleSelectionChange(value: UserVO[]) {
  selection.value = value
}

function asUser(row: unknown): UserVO {
  return row as UserVO
}

function openCreate() {
  formMode.value = 'create'
  editingUser.value = null
  formVisible.value = true
}

function openEdit(row: UserVO) {
  formMode.value = 'edit'
  editingUser.value = row
  formVisible.value = true
}

function openDetail(row: UserVO) {
  detailUserId.value = row.id
  detailVisible.value = true
}

function handleFormSuccess() {
  load()
}

async function toggleStatus(row: UserVO) {
  const target = row.status === 1 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${target}用户「${row.username}」吗？`, '状态变更', {
      type: 'warning'
    })
    await updateUserStatusBatch([row.id], row.status === 1 ? 0 : 1)
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
      `确定要${target}选中的 ${selection.value.length} 个用户吗？`,
      '批量操作',
      { type: 'warning' }
    )
    await updateUserStatusBatch(selection.value.map((item) => item.id), status)
    ElMessage.success(`已批量${target}`)
    tableRef.value?.clearSelection()
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleDelete(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteUser(row.id)
    ElMessage.success('用户已删除')
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleBatchDelete() {
  if (!selection.value.length) return
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selection.value.length} 个用户吗？`,
      '批量删除',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
    await batchDeleteUsers(selection.value.map((item) => item.id))
    ElMessage.success('已批量删除')
    tableRef.value?.clearSelection()
    load()
  } catch {
    // 取消或接口异常
  }
}

async function handleResetPassword(row: UserVO) {
  try {
    await ElMessageBox.confirm(`确定要重置「${row.username}」的密码吗？`, '重置密码', {
      type: 'warning'
    })
    const result = await resetUserPassword(row.id)
    await ElMessageBox.alert(`新密码：${result.password}`, '密码已重置', {
      confirmButtonText: '知道了'
    })
  } catch {
    // 取消或接口异常
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.filter-row {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.filter-form {
  display: flex;
  flex-wrap: nowrap;
  align-items: center;
  min-width: 0;

  :deep(.el-form-item) {
    flex-shrink: 0;
    margin-right: 12px;
    margin-bottom: 0;
  }

  :deep(.el-form-item__content) {
    flex-wrap: nowrap;
  }
}

.filter-actions {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;

  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}

@media (max-width: 1400px) {
  .filter-row,
  .filter-form {
    flex-wrap: wrap;
  }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell__avatar {
  color: #fff;
  background: linear-gradient(135deg, #3b82f6, #6366f1);
}

.user-cell__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.muted {
  color: $yszx-text-secondary;
}
</style>
