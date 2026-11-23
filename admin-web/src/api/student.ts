import dayjs from 'dayjs'
import { http } from './request'
import type { PageResult } from '@/types/api'
import type { StudentDetail, StudentInterview, StudentQuery, StudentVO } from '@/types/student'

export function getStudents(params: StudentQuery) {
  return http.get<PageResult<StudentVO>>('/students', params)
}

export function getStudentDetail(id: number) {
  return http.get<StudentDetail>(`/students/${id}/detail`)
}

export function getStudentInterviews(id: number, page: number, pageSize: number) {
  return http.get<PageResult<StudentInterview>>(`/students/${id}/interviews`, { page, pageSize })
}

export function updateStudentStatus(id: number, status: number) {
  return http.put<void>(`/students/${id}/status`, { status })
}

export function updateStudentsStatusBatch(ids: number[], status: number) {
  return http.put<void>('/students/batch-status', { ids, status })
}

export function deleteStudent(id: number) {
  return http.delete<void>(`/students/${id}`)
}

export function batchDeleteStudents(ids: number[]) {
  return http.delete<void>('/students/batch', { data: { ids } })
}

export async function exportStudents(params: { keyword?: string; status?: number }) {
  const blob = await http.getBlob('/students/export', params)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `学员列表_${dayjs().format('YYYYMMDD')}.xlsx`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}