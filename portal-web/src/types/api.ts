export interface FieldErrorItem {
  field: string
  reason: string
}

export interface FileErrorItem {
  row: number
  field: string
  reason: string
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
  fieldErrors?: FieldErrorItem[] | null
  fileErrors?: FileErrorItem[] | null
}

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
}

export interface StudentProfile {
  id: number
  nickname: string
  mobile: string
  email?: string | null
  gender: number
  avatar?: string | null
  preferredSubjectId?: number | null
  guideFinished: number
}

export interface SmsCodeResult {
  expireSeconds: number
}

/** 枚举取值与 DB 存储值一致（04 文档附录 C） */
export const QuestionType = {
  SINGLE: 1,
  MULTIPLE: 2,
  JUDGE: 3,
  SHORT_ANSWER: 4
} as const

export const Difficulty = {
  EASY: 1,
  MEDIUM: 2,
  HARD: 3
} as const
