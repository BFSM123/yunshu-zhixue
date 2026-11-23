<template>
  <el-dialog
    :model-value="modelValue"
    title="Excel 导入题目"
    width="920px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="!preview">
      <el-alert type="info" :closable="false" show-icon class="import-tip">
        <template #title>上传 Excel 表格（.xlsx / .xls）</template>
        <p class="import-tip__desc">
          表头：题干 | 学科 | 阶段 | 题型 | 难度 | 选项A~F | 正确答案 | 解析 | 标签（可选）
        </p>
        <p class="import-tip__desc">
          选择题答案用字母表示，多选多个答案用逗号分隔（如 A,B,C）；判断题答案填写「正确/错误」，
          简答题「正确答案」列填写参考答案。
        </p>
        <p class="import-tip__desc">
          学科与阶段均以文件内容为准，不存在的学科或阶段会在导入时自动创建。
        </p>
        <p class="import-tip__desc">单次最多 500 行、文件不超过 10MB，题目导入后状态为「待审核」。</p>
        <div class="import-tip__actions">
          <el-button link type="primary" :loading="downloadingTemplate" @click="downloadTemplate">
            下载导入模板
          </el-button>
        </div>
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
          <span v-if="importableCount === 0" class="preview-summary__hint">
            全部题目已存在，无需重复导入
          </span>
          <el-tag v-if="preview.duplicateCount" type="info" effect="plain">
            重复跳过 {{ preview.duplicateCount }}
          </el-tag>
          <el-tag v-if="preview.autoCreateSubjectCount" type="info" effect="plain">
            将自动创建 {{ preview.autoCreateSubjectCount }} 个学科
          </el-tag>
          <el-tag v-if="preview.autoCreateStageCount" type="info" effect="plain">
            将自动创建 {{ preview.autoCreateStageCount }} 个阶段
          </el-tag>
        </div>
        <el-button link type="primary" @click="resetPreview">重新选择文件</el-button>
      </div>

      <el-table :data="preview.rows" max-height="420" size="small">
        <el-table-column prop="row" label="行号" width="64" />
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag v-if="row.duplicate" type="info" size="small" effect="plain">
              重复跳过
            </el-tag>
            <el-tag v-else :type="statusTagType(row.status)" size="small" effect="plain">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="学科" min-width="140" show-overflow-tooltip />
        <el-table-column prop="stageName" label="阶段" min-width="160" show-overflow-tooltip />
        <el-table-column label="题型" width="76">
          <template #default="{ row }">{{ questionTypeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column label="难度" width="76">
          <template #default="{ row }">{{ questionDifficultyLabel(row.difficulty) }}</template>
        </el-table-column>
        <el-table-column label="提示" min-width="240">
          <template #default="{ row }">
            <span v-if="row.messages.length" class="row-errors">
              {{ row.messages.join('；') }}
            </span>
            <span v-else class="row-ok">—</span>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <template v-if="!preview">
        <el-button
          type="primary"
          :loading="parsing"
          :disabled="!file"
          @click="parse"
        >
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
import {
  confirmQuestionImport,
  downloadQuestionTemplate,
  previewQuestionImport
} from '@/api/question'
import type { QuestionImportPreview, QuestionImportStatus } from '@/types/question'
import { questionDifficultyLabel, questionTypeLabel } from '@/utils/question'

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
const preview = ref<QuestionImportPreview | null>(null)
const parsing = ref(false)
const importing = ref(false)
const downloadingTemplate = ref(false)

const importableCount = computed(() => {
  if (!preview.value) return 0
  // 有效 + 警告 中剔除完全重复的行，重复行确认导入时会自动跳过。
  return preview.value.validCount + preview.value.warningCount - preview.value.duplicateCount
})

watch(
  () => props.modelValue,
  (open) => {
    if (open) reset()
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
    preview.value = await previewQuestionImport(file.value)
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
  const notices: string[] = []
  if (preview.value.duplicateCount > 0) {
    notices.push(`${preview.value.duplicateCount} 行完全重复将跳过`)
  }
  if (preview.value.invalidCount > 0) {
    notices.push(`${preview.value.invalidCount} 行无效数据会被忽略`)
  }
  if (notices.length > 0) {
    try {
      await ElMessageBox.confirm(
        `将导入 ${importableCount.value} 道题目，${notices.join('，')}。确定导入吗？`,
        '确认导入',
        { type: 'warning', confirmButtonText: '导入', cancelButtonText: '取消' }
      )
    } catch {
      return
    }
  }

  importing.value = true
  try {
    const result = await confirmQuestionImport(preview.value.token)
    const subjectTip = result.createdSubjectCount
      ? `，自动创建 ${result.createdSubjectCount} 个学科`
      : ''
    const stageTip = result.createdStageCount
      ? `，自动创建 ${result.createdStageCount} 个阶段`
      : ''
    const skipTip = result.skippedCount
      ? `，跳过重复 ${result.skippedCount} 道`
      : ''
    ElMessage.success(`已导入 ${result.importedCount} 道题目${skipTip}${subjectTip}${stageTip}`)
    emit('update:modelValue', false)
    emit('success')
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    importing.value = false
  }
}

async function downloadTemplate() {
  downloadingTemplate.value = true
  try {
    await downloadQuestionTemplate()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    downloadingTemplate.value = false
  }
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

function statusLabel(status: QuestionImportStatus): string {
  if (status === 'valid') return '有效'
  if (status === 'warning') return '警告'
  return '无效'
}

function statusTagType(status: QuestionImportStatus): 'success' | 'warning' | 'danger' {
  if (status === 'valid') return 'success'
  if (status === 'warning') return 'warning'
  return 'danger'
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.import-tip {
  margin-bottom: 18px;
}

.import-tip__desc {
  margin: 6px 0 0;
  line-height: 1.7;
}

.import-tip__actions {
  margin-top: 8px;
}

.import-uploader {
  margin-top: 8px;
}

.import-uploader__icon {
  margin-bottom: 8px;
  font-size: 42px;
  color: $yszx-text-secondary;
}

.file-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-top: 8px;
  background: #f8fafc;
  border: 1px solid $yszx-border;
  border-radius: 10px;
}

.file-card__icon {
  font-size: 26px;
  color: $yszx-primary;
}

.file-card__info {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 2px;
}

.file-card__name {
  font-weight: 600;
}

.file-card__size {
  color: $yszx-text-secondary;
  font-size: 12px;
}

.preview-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.preview-summary__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.preview-summary__hint {
  color: $yszx-text-secondary;
  font-size: 13px;
}

.row-errors {
  color: #dc2626;
}

.row-ok {
  color: $yszx-text-secondary;
}
</style>
