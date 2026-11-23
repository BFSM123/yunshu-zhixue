export interface QuestionStats {
  total: number
  onShelf: number
  pending: number
  single: number
  multiple: number
  judgment: number
  shortAnswer: number
}

export interface QuestionListItem {
  id: number
  content: string
  stageId: number
  stageName: string | null
  subjectId: number | null
  subjectName: string | null
  type: number
  difficulty: number
  status: number
  tags: string | null
  createdAt: string
}

export interface QuestionOption {
  id: number
  optionKey: string
  optionText: string
  isCorrect: number
  sortOrder: number
}

export interface QuestionDetail {
  id: number
  content: string
  stageId: number
  stageName: string | null
  subjectId: number | null
  subjectName: string | null
  type: number
  difficulty: number
  status: number
  rejectReason: string | null
  tags: string | null
  answer: string | null
  analysis: string | null
  options: QuestionOption[]
  doneCount: number
  correctCount: number
  accuracyRate: number
  createdAt: string
  updatedAt: string
}

export interface QuestionOptionPayload {
  optionText: string
  correct: boolean
}

export interface QuestionSavePayload {
  content: string
  stageId: number
  type: number
  difficulty: number
  tags?: string
  analysis?: string
  answer?: string
  options?: QuestionOptionPayload[]
}

export interface QuestionAuditPayload {
  pass: boolean
  rejectReason?: string
}

export type QuestionImportStatus = 'valid' | 'warning' | 'invalid'

export interface QuestionImportOption {
  optionKey: string
  optionText: string
  correct: boolean
}

export interface QuestionImportRow {
  row: number
  content: string
  subjectName: string
  subjectId: number | null
  subjectAutoCreate: boolean
  stageName: string
  stageId: number | null
  stageAutoCreate: boolean
  type: number | null
  difficulty: number | null
  answer: string | null
  analysis: string | null
  tags: string | null
  options: QuestionImportOption[]
  /** 完全重复（库中已存在或文件内重复），确认导入时跳过。 */
  duplicate: boolean
  status: QuestionImportStatus
  messages: string[]
}

export interface QuestionImportPreview {
  token: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  duplicateCount: number
  autoCreateSubjectCount: number
  autoCreateStageCount: number
  rows: QuestionImportRow[]
}

export interface QuestionImportResult {
  importedCount: number
  skippedCount: number
  createdSubjectCount: number
  createdStageCount: number
}
