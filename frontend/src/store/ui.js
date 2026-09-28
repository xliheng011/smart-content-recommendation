import { reactive } from 'vue'

let seq = 0

export const toasts = reactive([])

export function dismissToast(id) {
  const index = toasts.findIndex((item) => item.id === id)
  if (index > -1) {
    toasts.splice(index, 1)
  }
}

export function notify(message, type = 'info', duration = 2800) {
  if (!message) {
    return
  }

  const id = ++seq

  toasts.push({ id, message, type })

  // 最多同时显示 3 条
  if (toasts.length > 3) {
    toasts.shift()
  }

  if (duration > 0) {
    setTimeout(() => dismissToast(id), duration)
  }
}

export const toastSuccess = (message) => notify(message, 'success')

export const toastError = (message) => notify(message, 'error', 3600)

export const toastInfo = (message) => notify(message, 'info')
