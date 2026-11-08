import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 练习会话：断点续练进度仅保存在浏览器本地（设计约定未完成练习不落库）。
 */
export interface PracticeProgress {
  practiceId: number
  questionIndex: number
  answers: Record<number, string>
  startedAt: number
}

const STORAGE_KEY = 'yszx_portal_practice_progress'

export const usePracticeStore = defineStore('portal-practice', () => {
  const progress = ref<PracticeProgress | null>(readFromStorage())

  function start(value: PracticeProgress) {
    progress.value = value
    persist()
  }

  function updateIndex(index: number) {
    if (!progress.value) return
    progress.value.questionIndex = index
    persist()
  }

  function saveAnswer(questionId: number, answer: string) {
    if (!progress.value) return
    progress.value.answers[questionId] = answer
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

  function readFromStorage(): PracticeProgress | null {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    try {
      return JSON.parse(raw) as PracticeProgress
    } catch {
      return null
    }
  }

  return { progress, start, updateIndex, saveAnswer, clear }
})
