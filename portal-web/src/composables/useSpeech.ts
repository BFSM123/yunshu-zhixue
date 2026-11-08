import { onUnmounted, ref } from 'vue'

interface SpeechRecognitionLike {
  lang: string
  continuous: boolean
  interimResults: boolean
  start: () => void
  stop: () => void
  onresult: ((event: SpeechResultEventLike) => void) | null
  onerror: ((event: unknown) => void) | null
  onend: (() => void) | null
}

interface SpeechResultEventLike {
  results: ArrayLike<ArrayLike<{ transcript: string }>>
}

type SpeechRecognitionCtor = new () => SpeechRecognitionLike

function getRecognitionCtor(): SpeechRecognitionCtor | null {
  const win = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor
    webkitSpeechRecognition?: SpeechRecognitionCtor
  }
  return win.SpeechRecognition || win.webkitSpeechRecognition || null
}

/**
 * 浏览器语音识别（需求 3.8.2）：识别结果写入输入框，不支持时由调用方降级为文字作答。
 */
export function useSpeechRecognition() {
  const supported = typeof window !== 'undefined' && Boolean(getRecognitionCtor())
  const listening = ref(false)
  let recognition: SpeechRecognitionLike | null = null

  function start(onResult: (text: string) => void) {
    const Ctor = getRecognitionCtor()
    if (!Ctor) return false

    recognition = new Ctor()
    recognition.lang = 'zh-CN'
    recognition.continuous = true
    recognition.interimResults = false
    recognition.onresult = (event) => {
      const results = Array.from(event.results)
      const text = results.map((item) => item[0]?.transcript ?? '').join('')
      if (text) onResult(text)
    }
    recognition.onerror = () => {
      listening.value = false
    }
    recognition.onend = () => {
      listening.value = false
    }
    recognition.start()
    listening.value = true
    return true
  }

  function stop() {
    recognition?.stop()
    recognition = null
    listening.value = false
  }

  onUnmounted(stop)

  return { supported, listening, start, stop }
}

/**
 * 浏览器语音播报（SpeechSynthesis）。
 */
export function useSpeechSynthesis() {
  const supported = typeof window !== 'undefined' && 'speechSynthesis' in window

  function speak(text: string) {
    if (!supported || !text) return
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text)
    utterance.lang = 'zh-CN'
    window.speechSynthesis.speak(utterance)
  }

  function cancel() {
    if (supported) {
      window.speechSynthesis.cancel()
    }
  }

  onUnmounted(cancel)

  return { supported, speak, cancel }
}
