import dayjs from 'dayjs'
import { http } from './request'
import type { PageResult } from '@/types/api'
import type { OperationLogQuery, OperationLogVO } from '@/types/system'

export function getLogs(params: OperationLogQuery) {
  return http.get<PageResult<OperationLogVO>>('/logs', params)
}

export function getLogDetail(id: number) {
  return http.get<OperationLogVO>(`/logs/${id}`)
}

export function clearLogs() {
  return http.delete<void>('/logs')
}

export async function exportLogs(params: Omit<OperationLogQuery, 'page' | 'pageSize'>) {
  const blob = await http.getBlob('/logs/export', params)
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `operation-logs-${dayjs().format('YYYYMMDD')}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}
