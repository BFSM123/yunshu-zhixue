import { http } from './request'
import type { RouteDetail, RouteItem, RoutePayload } from '@/types/content'

export function getRoutes() {
  return http.get<RouteItem[]>('/routes')
}

export function createRoute(payload: RoutePayload) {
  return http.post<void>('/routes', payload)
}

export function updateRoute(id: number, payload: RoutePayload) {
  return http.put<void>(`/routes/${id}`, payload)
}

export function updateRouteStatus(id: number, status: number) {
  return http.post<void>(`/routes/${id}/status`, { status })
}

export function deleteRoute(id: number) {
  return http.delete<void>(`/routes/${id}`)
}

export function getRouteDetail(id: number) {
  return http.get<RouteDetail>(`/routes/${id}/detail`)
}

export function addRouteCourses(id: number, courseIds: number[]) {
  return http.post<void>(`/routes/${id}/courses`, { courseIds })
}

export function removeRouteCourse(id: number, courseId: number) {
  return http.delete<void>(`/routes/${id}/courses/${courseId}`)
}

export function sortRouteCourses(id: number, routeCourseIds: number[]) {
  return http.put<void>(`/routes/${id}/courses/sort`, { routeCourseIds })
}
