<template>
  <el-dialog
    :model-value="modelValue"
    :title="`编辑权限${role ? ' - ' + role.name : ''}`"
    width="640px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loading" class="permission-body">
      <el-tree
        ref="treeRef"
        :data="treeData"
        :props="treeProps"
        node-key="id"
        show-checkbox
        default-expand-all
        class="permission-tree"
      />
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存权限</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { ElMessage, ElTree } from 'element-plus'
import type { PermissionNode, RoleVO } from '@/types/system'
import { getPermissionTree, saveRolePermissions } from '@/api/role'

const props = defineProps<{
  modelValue: boolean
  role: RoleVO | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const treeRef = ref<InstanceType<typeof ElTree>>()
const treeData = ref<PermissionNode[]>([])
const treeProps = { label: 'name', children: 'children' }
const loading = ref(false)
const saving = ref(false)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || !props.role) return
    loading.value = true
    try {
      treeData.value = await getPermissionTree(props.role.id)
      await nextTick()
      treeRef.value?.setCheckedKeys(collectChecked(treeData.value))
    } catch {
      // 错误提示由请求拦截器统一处理
    } finally {
      loading.value = false
    }
  }
)

async function submit() {
  if (!props.role || !treeRef.value) return
  saving.value = true
  try {
    const checked = treeRef.value.getCheckedKeys(false) as number[]
    const halfChecked = treeRef.value.getHalfCheckedKeys() as number[]
    await saveRolePermissions(props.role.id, [...new Set([...checked, ...halfChecked])])
    ElMessage.success('权限已保存')
    emit('success')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

function collectChecked(nodes: PermissionNode[]): number[] {
  const result: number[] = []
  for (const node of nodes) {
    if (node.checked) {
      result.push(node.id)
    }
    result.push(...collectChecked(node.children))
  }
  return result
}
</script>

<style scoped lang="scss">
.permission-body {
  min-height: 220px;
  max-height: 480px;
  overflow: auto;
}

.permission-tree {
  padding: 4px 0;
}
</style>
