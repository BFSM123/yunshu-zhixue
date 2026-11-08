<template>
  <div class="page-container">
    <PageHeader title="角色权限" description="仅超级管理员可访问；权限保存为全量覆盖。">
      <template #actions>
        <el-button type="primary" @click="createVisible = true">新建角色</el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="role-area">
      <div v-if="roles.length" class="role-grid">
        <article
          v-for="role in roles"
          :key="role.id"
          class="role-card"
          :class="{ 'role-card--builtin': role.code === 'super_admin' }"
          @click="openPermission(role)"
        >
          <div class="role-card__head">
            <div class="role-card__title">
              <h3>{{ role.name }}</h3>
              <span class="role-code">{{ role.code }}</span>
            </div>
            <el-tag size="small" effect="plain" type="info">{{ role.userCount }} 位用户</el-tag>
          </div>

          <p class="role-desc">{{ role.description || '暂无描述' }}</p>

          <div class="role-tags">
            <el-tag
              v-for="label in role.permissionLabels.slice(0, 4)"
              :key="label"
              size="small"
              effect="plain"
              :type="role.code === 'super_admin' ? 'warning' : 'info'"
            >
              {{ label }}
            </el-tag>
            <span v-if="role.permissionLabels.length > 4" class="role-tags__more">
              +{{ role.permissionLabels.length - 4 }}
            </span>
          </div>

          <div class="role-card__foot" @click.stop>
            <el-button
              link
              type="primary"
              :disabled="role.code === 'super_admin'"
              @click="openPermission(role)"
            >
              编辑权限
            </el-button>
            <span v-if="role.userCount > 0 && role.code !== 'super_admin'" class="role-hint">
              有用户时不可删除
            </span>
            <el-button
              link
              type="danger"
              :disabled="role.code === 'super_admin' || role.userCount > 0"
              @click="handleDelete(role)"
            >
              删除
            </el-button>
          </div>
        </article>
      </div>

      <el-empty v-else-if="!loading" description="暂无角色数据，点击右上角新增角色" />
    </div>

    <RoleCreateDialog v-model="createVisible" @success="load" />
    <RolePermissionDialog v-model="permissionVisible" :role="editingRole" @success="load" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/layout/PageHeader.vue'
import RoleCreateDialog from '@/components/system/RoleCreateDialog.vue'
import RolePermissionDialog from '@/components/system/RolePermissionDialog.vue'
import type { RoleVO } from '@/types/system'
import { deleteRole, getRoles } from '@/api/role'

const loading = ref(false)
const roles = ref<RoleVO[]>([])
const createVisible = ref(false)
const permissionVisible = ref(false)
const editingRole = ref<RoleVO | null>(null)

onMounted(load)

async function load() {
  loading.value = true
  try {
    roles.value = await getRoles()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function openPermission(role: RoleVO) {
  if (role.code === 'super_admin') return
  editingRole.value = role
  permissionVisible.value = true
}

async function handleDelete(role: RoleVO) {
  if (role.userCount > 0) {
    ElMessage.warning('该角色下仍有用户，不可删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除角色「${role.name}」吗？`, '删除角色', {
      type: 'warning',
      confirmButtonText: '删除',
      confirmButtonClass: 'el-button--danger'
    })
    await deleteRole(role.id)
    ElMessage.success('角色已删除')
    load()
  } catch {
    // 取消或接口异常
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.role-area {
  min-height: 220px;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.role-card {
  display: flex;
  flex-direction: column;
  padding: 22px;
  background: $yszx-surface;
  border: 1px solid $yszx-border;
  border-radius: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    border-color: rgba(37, 99, 235, 0.35);
    box-shadow: 0 16px 32px -20px rgba(15, 23, 42, 0.3);
    transform: translateY(-2px);
  }

  &--builtin {
    background: linear-gradient(180deg, #fffdf7, #fff);
    border-color: rgba(245, 185, 66, 0.35);
  }
}

.role-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.role-card__title {
  display: flex;
  flex-direction: column;
  gap: 6px;

  h3 {
    margin: 0;
    font-size: 17px;
    font-weight: 700;
  }
}

.role-code {
  color: $yszx-text-secondary;
  font-family: 'SFMono-Regular', Consolas, monospace;
  font-size: 11px;
  letter-spacing: 0.04em;
}

.role-desc {
  min-height: 20px;
  margin: 14px 0;
  color: $yszx-text-secondary;
  font-size: 13px;
  line-height: 1.7;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  min-height: 24px;
}

.role-tags__more {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.role-card__foot {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.role-hint {
  margin-right: auto;
  color: #94a3b8;
  font-size: 11px;
}
</style>
