import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * AI 面试会话：未完成的会话仅保存在浏览器本地（D16），
 * 结束时由后端单事务落库并生成报告。
 */
export interface InterviewProgress {
  sessionId: string
  subjectId: number
  difficulty: number
  questionCount: number
  currentOrder: number
  answers: Array<{ order: number; studentAnswer: string; durationSeconds: number }>
  scores: Array<{ order: number; score: number; feedback: string }>
  startedAt: number
}

const STORAGE_KEY = 'yszx_portal_interview_progress'

export const useInterviewStore = defineStore('portal-interview', () => {
  const progress = ref<InterviewProgress | null>(readFromStorage())

  function start(value: InterviewProgress) {
    progress.value = value
    persist()
  }

  function update(patch: Partial<InterviewProgress>) {
    if (!progress.value) return
    progress.value = { ...progress.value, ...patch }
    persist()
  }

  function clear() {
    progress.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  function persist() {
    if (progress.value) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(progress.value))
    }
  }

  function readFromStorage(): InterviewProgress | null {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as InterviewProgress
    } catch {
      return null
    }
  }

  return { progress, start, update, clear }
})
