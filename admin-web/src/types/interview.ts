export interface InterviewQuestionListItem {
  id: number
  content: string
  subjectId: number
  subjectName: string | null
  topic: string | null
  difficulty: number
  status: number
  tags: string | null
  sourceType: number
  sourceDocName: string | null
  sourceSeq: number | null
}

export interface InterviewQuestionDetail extends InterviewQuestionListItem {
  referenceAnswer: string | null
  referencedCount: number
  createdAt: string
  updatedAt: string
}

export interface InterviewQuestionSavePayload {
  subjectId: number
  content: string
  referenceAnswer?: string
  topic?: string
  difficulty: number
  tags?: string
}

export interface InterviewQuestionUpdatePayload {
  topic?: string
  difficulty: number
}

export interface InterviewTopic {
  id?: number | null
  name: string
  subjectId?: number | null
  subjectName?: string | null
  sortOrder?: number | null
  questionCount: number
  inVocabulary: boolean
}

export interface InterviewTopicSavePayload {
  name: string
  subjectId?: number | null
  sortOrder?: number
}

export type InterviewImportStatus = 'valid' | 'warning' | 'invalid'

export interface InterviewImportRow {
  row: number
  content: string
  subjectName: string
  subjectId: number | null
  subjectAutoCreate: boolean
  topic: string | null
  topicNew: boolean
  difficulty: number | null
  tags: string | null
  referenceAnswer: string | null
  duplicate: boolean
  status: InterviewImportStatus
  messages: string[]
}

export interface InterviewImportPreview {
  token: string
  sourceDocName: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  duplicateCount: number
  autoCreateSubjectCount: number
  newTopicCount: number
  rows: InterviewImportRow[]
}

export interface InterviewImportResult {
  importedCount: number
  skippedCount: number
  createdSubjectCount: number
}

export interface InterviewSearchTestPayload {
  keyword: string
  subjectId?: number
  limit?: number
}

export interface InterviewSearchTestItem {
  id: number
  content: string
  subjectId: number
  subjectName: string | null
  topic: string | null
  difficulty: number
  sourceType: number
  sourceDocName: string | null
  sourceSeq: number | null
}

export interface InterviewSearchTestResult {
  keyword: string
  total: number
  items: InterviewSearchTestItem[]
}