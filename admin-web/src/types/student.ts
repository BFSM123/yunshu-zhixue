export interface StudentVO {
  id: number
  nickname: string
  avatar: string | null
  mobile: string
  status: number
  createdAt: string | null
  lastLoginAt: string | null
  studyDays: number
  practiceCount: number
  interviewCount: number
}

export interface StudentStats {
  studyDays: number
  completedCourseCount: number
  practiceCount: number
  accuracyRate: number
}

export interface StudentDetail {
  id: number
  nickname: string
  avatar: string | null
  mobile: string
  email: string | null
  status: number
  createdAt: string | null
  lastLoginAt: string | null
  stats: StudentStats
  interviewCount: number
}

export interface StudentInterview {
  id: number
  subjectId: number
  subjectName: string | null
  difficulty: number
  questionCount: number
  overallScore: number | null
  totalDuration: number | null
  completedAt: string | null
}

export interface StudentQuery {
  keyword?: string
  status?: number
  page: number
  pageSize: number
}