import { http } from './request'
import type {
  StagePayload,
  SubjectCard,
  SubjectPayload,
  SubjectStats
} from '@/types/content'

export function getSubjectStats() {
  return http.get<SubjectStats>('/subjects/stats')
}

export function getSubjects() {
  return http.get<SubjectCard[]>('/subjects')
}

export function createSubject(payload: SubjectPayload) {
  return http.post<void>('/subjects', payload)
}

export function updateSubject(id: number, payload: SubjectPayload) {
  return http.put<void>(`/subjects/${id}`, payload)
}

export function sortSubject(id: number, move: 'up' | 'down') {
  return http.put<void>('/subjects/sort', { id, move })
}

export function deleteSubject(id: number) {
  return http.delete<void>(`/subjects/${id}`)
}

export function createStage(subjectId: number, payload: StagePayload) {
  return http.post<void>(`/subjects/${subjectId}/stages`, payload)
}

export function updateStage(subjectId: number, stageId: number, payload: StagePayload) {
  return http.put<void>(`/subjects/${subjectId}/stages/${stageId}`, payload)
}

export function sortStage(subjectId: number, id: number, move: 'up' | 'down') {
  return http.put<void>(`/subjects/${subjectId}/stages/sort`, { id, move })
}

export function updateStageStatus(subjectId: number, stageId: number, status: number) {
  return http.put<void>(`/subjects/${subjectId}/stages/${stageId}/status`, { status })
}

export function deleteStage(subjectId: number, stageId: number) {
  return http.delete<void>(`/subjects/${subjectId}/stages/${stageId}`)
}
