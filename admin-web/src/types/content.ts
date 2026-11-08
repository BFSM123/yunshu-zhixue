export interface SubjectStats {
  totalSubjects: number
  subjectsWithStages: number
  totalStages: number
  totalQuestions: number
}

export interface StageItem {
  id: number
  name: string
  icon: string | null
  sortOrder: number
  status: number
  questionCount: number
}

export interface SubjectCard {
  id: number
  name: string
  icon: string
  description: string
  themeColor: string
  sortOrder: number
  courseCount: number
  routeCount: number
  questionCount: number
  stages: StageItem[]
}

export interface SubjectPayload {
  name: string
  icon: string
  description: string
  sortOrder?: number
  themeColor: string
}

export interface StagePayload {
  name: string
  icon?: string
  sortOrder?: number
}

export interface CourseStats {
  totalCourses: number
  totalDurationHours: number
  relatedSubjectCount: number
}

export interface CourseItem {
  id: number
  name: string
  coverUrl: string | null
  durationMinutes: number
  source: number
  link: string | null
  bvid: string | null
  subjectId: number | null
  subjectName: string
  subjectIds: number[]
  subjectNames: string[]
}

export interface CoursePayload {
  name: string
  coverUrl?: string
  durationMinutes: number
  source: number
  link?: string
  subjectId: number | null
  subjectIds: number[]
}

export type CourseImportStatus = 'valid' | 'warning' | 'invalid'

export interface CourseImportRow {
  row: number
  name: string
  durationMinutes: number | null
  subjectName: string
  subjectId: number | null
  extraSubjectNames: string[]
  source: number | null
  link: string | null
  coverUrl: string | null
  bvid: string | null
  status: CourseImportStatus
  errors: string[]
}

export interface CourseImportPreview {
  token: string
  totalRows: number
  validCount: number
  warningCount: number
  invalidCount: number
  rows: CourseImportRow[]
}

export interface CourseImportResult {
  importedCount: number
}
