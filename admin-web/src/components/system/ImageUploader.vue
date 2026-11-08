<template>
  <div class="image-uploader">
    <div v-if="modelValue" class="image-uploader__preview">
      <img :src="modelValue" alt="已上传图片" />
      <div class="image-uploader__mask">
        <el-upload
          :show-file-list="false"
          :before-upload="beforeUpload"
          :http-request="handleUpload"
          accept="image/png,image/jpeg"
        >
          <el-button size="small" :loading="loading">更换</el-button>
        </el-upload>
        <el-button size="small" @click="remove">移除</el-button>
      </div>
    </div>

    <el-upload
      v-else
      class="image-uploader__drop"
      drag
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      accept="image/png,image/jpeg"
    >
      <div v-loading="loading" class="image-uploader__body">
        <el-icon class="image-uploader__icon"><Plus /></el-icon>
        <p>点击或拖拽上传</p>
        <span>{{ hint }}</span>
      </div>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, type UploadRawFile, type UploadRequestOptions } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { uploadAvatar, uploadLogo } from '@/api/setting'

const props = withDefaults(
  defineProps<{
    modelValue: string
    scene: 'logo' | 'avatar'
    hint?: string
  }>(),
  { hint: '' }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const loading = ref(false)

async function beforeUpload(file: UploadRawFile) {
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    ElMessage.error('仅支持 JPG / PNG 图片')
    return false
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }

  try {
    const { width, height } = await readImageSize(file)
    if (props.scene === 'logo' && (width !== 256 || height !== 256)) {
      ElMessage.error('Logo 需为 256×256 的图片')
      return false
    }
    if (props.scene === 'avatar' && (width > 200 || height > 200)) {
      ElMessage.error('默认头像尺寸不能超过 200×200')
      return false
    }
  } catch {
    ElMessage.error('无法读取图片信息')
    return false
  }
  return true
}

async function handleUpload(options: UploadRequestOptions) {
  loading.value = true
  try {
    const result = props.scene === 'logo'
      ? await uploadLogo(options.file as File)
      : await uploadAvatar(options.file as File)
    emit('update:modelValue', result.url)
    options.onSuccess(result)
    ElMessage.success('上传成功')
  } catch (error) {
    options.onError(error as Parameters<typeof options.onError>[0])
  } finally {
    loading.value = false
  }
}

function remove() {
  emit('update:modelValue', '')
}

function readImageSize(file: File): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve({ width: image.naturalWidth, height: image.naturalHeight })
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('图片读取失败'))
    }
    image.src = url
  })
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.image-uploader {
  width: 100%;
  max-width: 280px;
}

.image-uploader__drop {
  :deep(.el-upload-dragger) {
    padding: 0;
    background: #fafbfc;
    border: 1px dashed $yszx-border;
    border-radius: 12px;
  }

  :deep(.el-upload-dragger:hover) {
    border-color: $yszx-primary;
  }
}

.image-uploader__body {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 140px;
  color: $yszx-text-secondary;
  text-align: center;

  p {
    margin: 6px 0 2px;
    color: $yszx-text;
    font-size: 13px;
  }

  span {
    font-size: 11px;
  }
}

.image-uploader__icon {
  color: #94a3b8;
  font-size: 24px;
}

.image-uploader__preview {
  position: relative;
  width: 140px;
  height: 140px;
  overflow: hidden;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 12px;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  &:hover .image-uploader__mask {
    opacity: 1;
  }
}

.image-uploader__mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: rgba(15, 23, 42, 0.55);
  opacity: 0;
  transition: opacity 0.2s ease;
}
</style>
