<template>
  <el-dialog
    :model-value="modelValue"
    title="批量导入课程"
    width="880px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="!preview">
      <el-alert type="info" :closable="false" show-icon class="import-tip">
        <template #title>上传 Excel 表格（.xlsx / .xls）</template>
        <p class="import-tip__desc">
          表头：课程名称 | 所属学科 | 来源 | 时长（分钟） | BV号 | 封面 | 播放链接
        </p>
        <p class="import-tip__desc">
          所属学科支持多个学科，以逗号分隔（第一个为主学科）；不存在的学科导入时将自动创建。
        </p>
        <p class="import-tip__desc">
          单次最多 500 行、文件不超过 10MB；BV 号优先取表格列，缺失时从播放链接解析。
        </p>
      </el-alert>

      <el-upload
        v-if="!file"
        class="import-uploader"
        drag
        accept=".xlsx,.xls"
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFileChange"
      >
        <el-icon class="import-uploader__icon"><UploadFilled /></el-icon>
        <div class="el-upload__text">将表格拖到此处，或<em>点击选择文件</em></div>
        <template #tip>
          <div class="el-upload__tip">仅支持 .xlsx / .xls，单个文件不超过 10MB</div>
        </template>
      </el-upload>

      <div v-else class="file-card">
        <el-icon class="file-card__icon"><Document /></el-icon>
        <div class="file-card__info">
          <span class="file-card__name">{{ fileName }}</span>
          <span class="file-card__size">{{ formatFileSize(fileSize) }}</span>
        </div>
        <el-button link type="danger" @click="clearFile">移除</el-button>
      </div>
    </template>

    <template v-else>
      <div class="preview-summary">
        <div class="preview-summary__tags">
          <el-tag type="success" effect="plain">有效 {{ preview.validCount }}</el-tag>
          <el-tag type="warning" effect="plain">警告 {{ preview.warningCount }}</el-tag>
          <el-tag type="danger" effect="plain">无效 {{ preview.invalidCount }}</el-tag>
          <span class="preview-summary__hint">共解析 {{ preview.totalRows }} 行</span>
        </div>
        <el-button link type="primary" @click="resetPreview">重新选择文件</el-button>
      </div>

      <el-table :data="preview.rows" max-height="380" size="small">
        <el-table-column prop="row" label="行号" width="64" />
        <el-table-column label="状态" width="82">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column label="时长" width="112">
          <template #default="{ row }">
            {{ row.durationMinutes ? formatDuration(row.durationMinutes) : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="学科" min-width="160">
          <template #default="{ row }">
            <div>{{ row.subjectName || '其他学科' }}</div>
            <div v-if="row.extraSubjectNames.length" class="row-extra">
              关联：{{ row.extraSubjectNames.join('、') }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="76">
          <template #default="{ row }">
            {{ row.source === 1 ? 'B站' : row.source === 2 ? '其他' : '—' }}
          </template>
        </el-table-column>
        <el-table-column label="提示" min-width="240">
          <template #default="{ row }">
            <span v-if="row.errors.length" class="row-errors">{{ row.errors.join('；') }}</span>
            <span v-else class="row-ok">—</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <template v-if="!preview">
        <el-button type="primary" :loading="parsing" :disabled="!file" @click="parse">
          解析预览
        </el-button>
      </template>
      <template v-else>
        <el-button @click="resetPreview">返回修改</el-button>
        <el-button
          type="primary"
          :loading="importing"
          :disabled="importableCount === 0"
          @click="confirmImport"
        >
          确认导入（{{ importableCount }}）
        </el-button>
      </template>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus'
import { Document, UploadFilled } from '@element-plus/icons-vue'
import { confirmCourseImport, previewCourseImport } from '@/api/course'
import type { CourseImportPreview, CourseImportStatus } from '@/types/content'

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
  success: []
}>()

const MAX_FILE_SIZE = 10 * 1024 * 1024

const file = ref<File | null>(null)
const fileName = ref('')
const fileSize = ref(0)
const preview = ref<CourseImportPreview | null>(null)
const parsing = ref(false)
const importing = ref(false)

const importableCount = computed(() =>
  preview.value ? preview.value.validCount + preview.value.warningCount : 0
)

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      reset()
    }
  }
)

function reset() {
  file.value = null
  fileName.value = ''
  fileSize.value = 0
  preview.value = null
}

function handleFileChange(uploadFile: UploadFile) {
  const raw = uploadFile.raw
  if (!raw) return

  const name = raw.name.toLowerCase()
  if (!name.endsWith('.xlsx') && !name.endsWith('.xls')) {
    ElMessage.error('仅支持 .xlsx / .xls 表格文件')
    return
  }
  if (raw.size > MAX_FILE_SIZE) {
    ElMessage.error('文件大小不能超过 10MB')
    return
  }
  file.value = raw
  fileName.value = raw.name
  fileSize.value = raw.size
}

function clearFile() {
  file.value = null
  fileName.value = ''
  fileSize.value = 0
}

async function parse() {
  if (!file.value) {
    ElMessage.warning('请先选择表格文件')
    return
  }
  parsing.value = true
  try {
    preview.value = await previewCourseImport(file.value)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    parsing.value = false
  }
}

function resetPreview() {
  preview.value = null
}

async function confirmImport() {
  if (!preview.value || importableCount.value === 0) return

  if (preview.value.invalidCount > 0) {
    try {
      await ElMessageBox.confirm(
        `将导入 ${importableCount.value} 门课程，${preview.value.invalidCount} 行无效数据会被忽略。确定导入吗？`,
        '确认导入',
        { type: 'warning', confirmButtonText: '导入', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }

  importing.value = true
  try {
    const result = await confirmCourseImport(preview.value.token)
    ElMessage.success(`已导入 ${result.importedCount} 门课程`)
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    importing.value = false
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function statusLabel(status: CourseImportStatus): string {
  if (status === 'valid') return '有效'
  if (status === 'warning') return '警告'
  return '无效'
}

function statusTagType(status: CourseImportStatus): 'success' | 'warning' | 'danger' {
  if (status === 'valid') return 'success'
  if (status === 'warning') return 'warning'
  return 'danger'
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

.import-tip {
  margin-bottom: 16px;
}

.import-tip__desc {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.6;
}

.import-uploader :deep(.el-upload-dragger) {
  padding: 28px 16px;
}

.import-uploader__icon {
  margin-bottom: 8px;
  color: $yszx-primary;
  font-size: 44px;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 10px;
}

.file-card__icon {
  color: $yszx-primary;
  font-size: 28px;
}

.file-card__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-width: 0;
}

.file-card__name {
  overflow: hidden;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-card__size {
  margin-top: 2px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.preview-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.preview-summary__tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-summary__hint {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.row-extra {
  margin-top: 2px;
  color: $yszx-text-secondary;
  font-size: 12px;
}

.row-errors {
  color: #b45309;
  font-size: 12px;
}

.row-ok {
  color: $yszx-text-secondary;
  font-size: 12px;
}
</style>
