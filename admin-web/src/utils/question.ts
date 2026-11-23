export const QUESTION_TYPE_OPTIONS = [
  { label: '单选', value: 1 },
  { label: '多选', value: 2 },
  { label: '判断', value: 3 },
  { label: '简答', value: 4 }
] as const

export const QUESTION_DIFFICULTY_OPTIONS = [
  { label: '简单', value: 1 },
  { label: '中等', value: 2 },
  { label: '困难', value: 3 }
] as const

export const QUESTION_STATUS_OPTIONS = [
  { label: '已上架', value: 1 },
  { label: '已下架', value: 2 },
  { label: '待审核', value: 3 },
  { label: '已驳回', value: 5 }
] as const

export function questionTypeLabel(type: number | null | undefined): string {
  return QUESTION_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? '—'
}

export function questionDifficultyLabel(difficulty: number | null | undefined): string {
  return QUESTION_DIFFICULTY_OPTIONS.find((item) => item.value === difficulty)?.label ?? '—'
}

export function questionStatusLabel(status: number | null | undefined): string {
  return QUESTION_STATUS_OPTIONS.find((item) => item.value === status)?.label ?? '—'
}

export function questionTypeTagType(
  type: number | null | undefined
): 'primary' | 'success' | 'warning' | 'info' {
  if (type === 1) return 'primary'
  if (type === 2) return 'success'
  if (type === 3) return 'warning'
  return 'info'
}

export function questionDifficultyTagType(
  difficulty: number | null | undefined
): 'success' | 'warning' | 'danger' {
  if (difficulty === 1) return 'success'
  if (difficulty === 2) return 'warning'
  return 'danger'
}

export function questionStatusTagType(
  status: number | null | undefined
): 'success' | 'warning' | 'danger' | 'info' {
  if (status === 1) return 'success'
  if (status === 3) return 'warning'
  if (status === 5) return 'danger'
  return 'info'
}
