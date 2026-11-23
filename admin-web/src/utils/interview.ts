import type { InterviewQuestionListItem } from '@/types/interview'

export const INTERVIEW_DIFFICULTY_OPTIONS = [
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 }
] as const

export const INTERVIEW_STATUS_OPTIONS = [
  { label: '已上架', value: 1 },
  { label: '已下架', value: 2 }
] as const

export const INTERVIEW_SOURCE_OPTIONS = [
  { label: '文档解析', value: 1 },
  { label: '手工录入', value: 2 },
  { label: '手动导入', value: 3 }
] as const

export function interviewDifficultyLabel(difficulty: number | null | undefined): string {
  return INTERVIEW_DIFFICULTY_OPTIONS.find((item) => item.value === difficulty)?.label ?? '—'
}

export function interviewStatusLabel(status: number | null | undefined): string {
  return INTERVIEW_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? '—'
}

export function interviewSourceLabel(sourceType: number | null | undefined): string {
  return INTERVIEW_SOURCE_OPTIONS.find((item) => item.value === sourceType)?.label ?? '—'
}

export function interviewDifficultyTagType(
  difficulty: number | null | undefined
): 'success' | 'warning' | 'danger' {
  if (difficulty === 1) return 'success'
  if (difficulty === 2) return 'warning'
  return 'danger'
}

export function interviewStatusTagType(
  status: number | null | undefined
): 'success' | 'info' {
  return status === 1 ? 'success' : 'info'
}

export function interviewSourceTagType(
  sourceType: number | null | undefined
): 'primary' | 'success' | 'warning' {
  if (sourceType === 1) return 'warning'
  if (sourceType === 3) return 'success'
  return 'primary'
}

/**
 * 来源列文案：手工录入只显示录入方式；
 * 文档解析与手动导入展示「学科 · 文件名 · 第 N 题」。
 */
export function interviewSourceText(
  row: Pick<InterviewQuestionListItem, 'sourceType' | 'subjectName' | 'sourceDocName' | 'sourceSeq'>
): string {
  if (row.sourceType === 2) {
    return '手工录入'
  }
  const parts = [
    row.subjectName,
    row.sourceDocName,
    row.sourceSeq ? `第 ${row.sourceSeq} 题` : null
  ].filter((part): part is string => Boolean(part))
  return parts.length > 0 ? parts.join(' · ') : interviewSourceLabel(row.sourceType)
}

/** 是否需要题干旁的来源标识（手工录入与手动导入需要）。 */
export function showSourceBadge(sourceType: number | null | undefined): boolean {
  return sourceType === 2 || sourceType === 3
}