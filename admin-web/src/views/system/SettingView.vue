<template>
  <div class="page-container">
    <PageHeader title="系统设置" description="平台名称、简介、Logo 与默认头像。" />

    <el-card v-loading="loading" shadow="never" class="setting-card">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="110px"
        class="setting-form"
      >
        <el-form-item label="平台名称" prop="siteName">
          <el-input
            v-model="form.siteName"
            placeholder="请输入平台名称"
            maxlength="100"
            style="max-width: 460px"
          />
        </el-form-item>

        <el-form-item label="平台描述">
          <el-input
            v-model="form.siteDescription"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入平台描述"
            style="max-width: 620px"
          />
        </el-form-item>

        <el-form-item label="平台 Logo">
          <ImageUploader
            v-model="form.logoUrl"
            scene="logo"
            hint="JPG / PNG，尺寸 256×256"
          />
        </el-form-item>

        <el-form-item label="默认头像">
          <ImageUploader
            v-model="form.defaultAvatar"
            scene="avatar"
            hint="JPG / PNG，不超过 200×200"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
          <el-button @click="reset">恢复默认</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/layout/PageHeader.vue'
import ImageUploader from '@/components/system/ImageUploader.vue'
import { getSettings, resetSettings, updateSettings } from '@/api/setting'

const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  siteName: '',
  siteDescription: '',
  logoUrl: '',
  defaultAvatar: ''
})

const rules: FormRules = {
  siteName: [
    { required: true, message: '请输入平台名称', trigger: 'blur' },
    { max: 100, message: '平台名称不能超过 100 位', trigger: 'blur' }
  ]
}

onMounted(load)

async function load() {
  loading.value = true
  try {
    const result = await getSettings()
    Object.assign(form, result)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await updateSettings({ ...form })
    ElMessage.success('设置已保存')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    saving.value = false
  }
}

async function reset() {
  try {
    await ElMessageBox.confirm('确定要恢复默认设置吗？当前配置将被覆盖。', '恢复默认设置', {
      type: 'warning'
    })
    await resetSettings()
    await load()
    ElMessage.success('已恢复默认设置')
  } catch {
    // 取消或接口异常
  }
}
</script>

<style scoped lang="scss">
.setting-card {
  max-width: 860px;
}

.setting-form {
  padding-top: 8px;
}
</style>
