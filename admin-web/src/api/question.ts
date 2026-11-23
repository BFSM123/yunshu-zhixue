import dayjs from 'dayjs'
import { http } from './request'
import type { PageResult } from '@/types/api'
import type {
  QuestionAuditPayload,
  QuestionDetail,
  QuestionImportPreview,
  QuestionImportResult,
  QuestionListItem,
  QuestionSavePayload,
  QuestionStats
} from '@/types/question'

export interface QuestionQuery {
  keyword?: string
  subjectId?: number
  stageId?: number
  type?: number
  difficulty?: number
  status?: number
  page: number
  pageSize: number
}

export type QuestionFilter = Omit<QuestionQuery, 'page' | 'pageSize'>

export function getQuestionStats() {
  return http.get<QuestionStats>('/questions/stats')
}

export function getQuestions(params: QuestionQuery) {
  return http.get<PageResult<QuestionListItem>>('/questions', params)
}

export function getQuestionDetail(id: number) {
  return http.get<QuestionDetail>(`/questions/${id}`)
}

export function createQuestion(payload: QuestionSavePayload) {
  return http.post<void>('/questions', payload)
}

export function updateQuestion(id: number, payload: QuestionSavePayload) {
  return http.put<void>(`/questions/${id}`, payload)
}

export function deleteQuestion(id: number) {
  return http.delete<void>(`/questions/${id}`)
}

export function auditQuestion(id: number, payload: QuestionAuditPayload) {
  return http.post<void>(`/questions/${id}/audit`, payload)
}

export function updateQuestionStatus(id: number, action: 'on' | 'off') {
  return http.post<void>(`/questions/${id}/status`, { action })
}

export function resubmitQuestion(id: number) {
  return http.post<void>(`/questions/${id}/resubmit`)
}

export function batchUpdateQuestionStatus(ids: number[], action: 'on' | 'off') {
  return http.post<number>('/questions/batch-status', { ids, action })
}

export function batchDeleteQuestions(ids: number[]) {
  return http.post<number>('/questions/batch-delete', { ids })
}

export function batchAuditQuestions(ids: number[], pass: boolean, rejectReason?: string) {
  return http.post<number>('/questions/batch-audit', { ids, pass, rejectReason })
}

export function previewQuestionImport(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<QuestionImportPreview>('/questions/import/preview', formData)
}

export function confirmQuestionImport(token: string) {
  return http.post<QuestionImportResult>('/questions/import/confirm', { token })
}

export async function downloadQuestionTemplate() {
  const blob = await http.getBlob('/questions/import/template')
  saveBlob(blob, '题库导入模板.xlsx')
}

export async function exportQuestions(params: QuestionFilter) {
  const blob = await http.getBlob('/questions/export', params)
  saveBlob(blob, `题库导出_${dayjs().format('YYYYMMDD')}.xlsx`)
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
