<template>
  <el-dialog
    :model-value="modelValue"
    :title="mode === 'create' ? '新增用户' : '编辑用户'"
    width="540px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="form.mobile" placeholder="请输入手机号" maxlength="11" clearable />
        </el-form-item>
      </div>

      <div class="form-grid">
        <el-form-item label="角色" prop="roleId">
          <el-select
            v-model="form.roleId"
            placeholder="请选择角色"
            :disabled="isBuiltin"
            style="width: 100%"
          >
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            active-text="已启用"
            inactive-text="已禁用"
          />
        </el-form-item>
      </div>

      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="选填" clearable />
      </el-form-item>

      <el-form-item v-if="mode === 'create'" label="密码" prop="password">
        <el-input
          v-model="form.password"
          type="password"
          placeholder="请输入密码（6~20 位）"
          show-password
        />
      </el-form-item>

      <el-form-item v-else label="新密码" prop="newPassword">
        <el-input
          v-model="form.newPassword"
          type="password"
          placeholder="留空则不修改密码"
          show-password
        />
      </el-form-item>

      <p v-if="isBuiltin" class="builtin-hint">内置超级管理员账号的角色不可修改。</p>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { RoleOption, UserVO } from '@/types/system'
import { createUser, updateUser } from '@/api/user'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    mode: 'create' | 'edit'
    user?: UserVO | null
    roleOptions: RoleOption[]
  }>(),
  { user: null }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)
const form = reactive({
  username: '',
  mobile: '',
  email: '',
  roleId: undefined as number | undefined,
  password: '',
  newPassword: '',
  status: 1
})

const isBuiltin = computed(() => props.mode === 'edit' && Boolean(props.user?.isBuiltin))

const rules = computed<FormRules>(() => {
  const base: FormRules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 2, max: 50, message: '用户名长度为 2~50 位', trigger: 'blur' }
    ],
    mobile: [
      { required: true, message: '请输入手机号', trigger: 'blur' },
      { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
    ],
    email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
    roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
  }
  if (props.mode === 'create') {
    base.password = [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 20, message: '密码长度为 6~20 位', trigger: 'blur' }
    ]
  } else {
    base.newPassword = [
      { pattern: /^$|^.{6,20}$/, message: '密码长度为 6~20 位', trigger: 'blur' }
    ]
  }
  return base
})

watch(
  () => props.modelValue,
  async (open) => {
    if (!open) return
    if (props.mode === 'edit' && props.user) {
      Object.assign(form, {
        username: props.user.username,
        mobile: props.user.mobile || '',
        email: props.user.email || '',
        roleId: props.user.roleId,
        password: '',
        newPassword: '',
        status: props.user.status
      })
    } else {
      Object.assign(form, {
        username: '',
        mobile: '',
        email: '',
        roleId: undefined,
        password: '',
        newPassword: '',
        status: 1
      })
    }
    await nextTick()
    formRef.value?.clearValidate()
  }
)

async function submit() {
  if (!formRef.value || !form.roleId) {
    formRef.value?.validate().catch(() => false)
    return
  }
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const base = {
      username: form.username.trim(),
      mobile: form.mobile.trim(),
      email: form.email.trim() || undefined,
      roleId: form.roleId,
      status: form.status
    }
    if (props.mode === 'create') {
      await createUser({ ...base, password: form.password })
      ElMessage.success('用户已创建')
    } else {
      await updateUser(props.user!.id, {
        ...base,
        newPassword: form.newPassword || undefined
      })
      ElMessage.success('用户已更新')
    }
    emit('success')
    emit('update:modelValue', false)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.builtin-hint {
  margin: 0;
  color: $yszx-text-secondary;
  font-size: 12px;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
