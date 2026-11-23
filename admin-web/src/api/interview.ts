import { http } from './request'
import type { PageResult } from '@/types/api'
import type {
  InterviewImportPreview,
  InterviewImportResult,
  InterviewQuestionDetail,
  InterviewQuestionListItem,
  InterviewQuestionSavePayload,
  InterviewQuestionUpdatePayload,
  InterviewSearchTestPayload,
  InterviewSearchTestResult,
  InterviewTopic,
  InterviewTopicSavePayload
} from '@/types/interview'

export interface InterviewQuestionQuery {
  keyword?: string
  subjectId?: number
  topic?: string
  difficulty?: number
  unclassified?: boolean
  page: number
  pageSize: number
}

export function getInterviewQuestions(params: InterviewQuestionQuery) {
  return http.get<PageResult<InterviewQuestionListItem>>('/interview-questions', params)
}

export function getInterviewQuestionDetail(id: number) {
  return http.get<InterviewQuestionDetail>(`/interview-questions/${id}`)
}

export function createInterviewQuestion(payload: InterviewQuestionSavePayload) {
  return http.post<void>('/interview-questions', payload)
}

export function updateInterviewQuestion(id: number, payload: InterviewQuestionUpdatePayload) {
  return http.put<void>(`/interview-questions/${id}`, payload)
}

export function deleteInterviewQuestion(id: number) {
  return http.delete<void>(`/interview-questions/${id}`)
}

export function updateInterviewQuestionStatus(id: number, action: 'on' | 'off') {
  return http.post<void>(`/interview-questions/${id}/status`, { action })
}

export function batchUpdateInterviewQuestionStatus(ids: number[], action: 'on' | 'off') {
  return http.post<number>('/interview-questions/batch-status', { ids, action })
}

export function batchDeleteInterviewQuestions(ids: number[]) {
  return http.post<number>('/interview-questions/batch-delete', { ids })
}

export function previewInterviewImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<InterviewImportPreview>('/interview-questions/import/preview', formData)
}

export function confirmInterviewImport(token: string) {
  return http.post<InterviewImportResult>('/interview-questions/import/confirm', { token })
}

export async function downloadInterviewTemplate() {
  const blob = await http.getBlob('/interview-questions/import/template')
  saveBlob(blob, '面试题导入模板.xlsx')
}

export function getInterviewTopics() {
  return http.get<InterviewTopic[]>('/interview-topics')
}

export function createInterviewTopic(payload: InterviewTopicSavePayload) {
  return http.post<void>('/interview-topics', payload)
}

export function updateInterviewTopic(id: number, payload: InterviewTopicSavePayload) {
  return http.put<void>(`/interview-topics/${id}`, payload)
}

export function deleteInterviewTopic(id: number) {
  return http.delete<void>(`/interview-topics/${id}`)
}

export function searchTestInterviewQuestions(payload: InterviewSearchTestPayload) {
  return http.post<InterviewSearchTestResult>('/interview-questions/search-test', payload)
}

function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}