import { reactive } from 'vue'
import { authApi, TOKEN_KEY } from '../api'

const USER_KEY = 'scr_user'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export const authState = reactive({
  token: localStorage.getItem(TOKEN_KEY) || '',
  user: readStoredUser(),
  /** 是否已经尝试过用本地令牌恢复会话 */
  restored: false
})

export function isLoggedIn() {
  return Boolean(authState.token && authState.user)
}

export function isAdmin() {
  return authState.user?.role === 'ADMIN'
}

function persist(data) {
  authState.token = data.token
  authState.user = data.user

  localStorage.setItem(TOKEN_KEY, data.token)
  localStorage.setItem(USER_KEY, JSON.stringify(data.user))
}

export function clearSession() {
  authState.token = ''
  authState.user = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export async function login(payload) {
  const data = await authApi.login(payload)
  persist(data)
  return data
}

export async function register(payload) {
  const data = await authApi.register(payload)
  persist(data)
  return data
}

export async function logout() {
  try {
    await authApi.logout()
  } catch {
    // 令牌可能已失效，忽略即可
  }
  clearSession()
}

/**
 * 用本地令牌向后端确认一次身份，
 * 避免令牌过期后前端仍显示为已登录。
 */
export async function restoreSession() {
  if (!authState.token) {
    authState.restored = true
    return
  }

  try {
    const user = await authApi.me()
    authState.user = user
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } catch {
    clearSession()
  } finally {
    authState.restored = true
  }
}
