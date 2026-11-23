<template>
  <el-dialog
    :model-value="modelValue"
    title="面试题检索测试"
    width="820px"
    append-to-body
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="search-tip"
      title="输入关键词验证面试题库内容，检索范围为题干（含已下架题目）"
    />

    <el-form inline class="search-form" @submit.prevent="search">
      <el-form-item label="关键词">
        <el-input
          v-model="keyword"
          placeholder="如：RAG"
          clearable
          style="width: 240px"
          @keyup.enter="search"
        />
      </el-form-item>
      <el-form-item label="学科">
        <el-select v-model="subjectId" placeholder="全部学科" clearable style="width: 180px">
          <el-option
            v-for="subject in subjects"
            :key="subject.id"
            :label="subject.name"
            :value="subject.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="searching" @click="search">检索</el-button>
      </el-form-item>
    </el-form>

    <div v-if="result" class="search-summary">
      关键词「{{ result.keyword }}」命中 <strong>{{ result.total }}</strong> 道面试题
      <span v-if="result.total > result.items.length">（展示前 {{ result.items.length }} 条）</span>
    </div>

    <el-table v-if="result" v-loading="searching" :data="result.items" max-height="400" size="small">
      <el-table-column prop="content" label="题干" min-width="300" show-overflow-tooltip />
      <el-table-column label="学科" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.subjectName || '—' }}</template>
      </el-table-column>
      <el-table-column label="主题" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.topic || '未分类' }}</template>
      </el-table-column>
      <el-table-column label="难度" width="76">
        <template #default="{ row }">{{ interviewDifficultyLabel(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column label="来源" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">
          {{ interviewSourceText(row as InterviewSearchTestItem) }}
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="没有命中任何题目，换个关键词试试" :image-size="70" />
      </template>
    </el-table>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { searchTestInterviewQuestions } from '@/api/interview'
import type { InterviewSearchTestItem, InterviewSearchTestResult } from '@/types/interview'
import type { SubjectCard } from '@/types/content'
import { interviewDifficultyLabel, interviewSourceText } from '@/utils/interview'

defineProps<{
  modelValue: boolean
  subjects: SubjectCard[]
}>()

const emit = defineEmits<{
  'update:modelValue': [boolean]
}>()

const keyword = ref('')
const subjectId = ref<number | undefined>(undefined)
const searching = ref(false)
const result = ref<InterviewSearchTestResult | null>(null)

async function search() {
  const value = keyword.value.trim()
  if (!value) {
    ElMessage.warning('请输入检索关键词')
    return
  }
  searching.value = true
  try {
    result.value = await searchTestInterviewQuestions({
      keyword: value,
      subjectId: subjectId.value,
      limit: 20
    })
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    searching.value = false
  }
}
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.search-tip {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: 4px;
}

.search-summary {
  margin-bottom: 12px;
  color: $yszx-text-secondary;

  strong {
    color: $yszx-primary;
  }
}
</style>