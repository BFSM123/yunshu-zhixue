import { http } from './request'
import type { PageResult } from '@/types/api'
import type {
  CourseImportPreview,
  CourseImportResult,
  CourseItem,
  CoursePayload,
  CourseStats
} from '@/types/content'

export interface CourseQuery {
  keyword?: string
  subjectId?: number
  source?: number
  page: number
  pageSize: number
}

export function getCourseStats() {
  return http.get<CourseStats>('/courses/stats')
}

export function getCourses(params: CourseQuery) {
  return http.get<PageResult<CourseItem>>('/courses', params)
}

export function createCourse(payload: CoursePayload) {
  return http.post<void>('/courses', payload)
}

export function updateCourse(id: number, payload: CoursePayload) {
  return http.put<void>(`/courses/${id}`, payload)
}

export function deleteCourse(id: number) {
  return http.delete<void>(`/courses/${id}`)
}

export function previewCourseImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<CourseImportPreview>('/courses/import/preview', formData)
}

export function confirmCourseImport(token: string) {
  return http.post<CourseImportResult>('/courses/import/confirm', { token })
}
