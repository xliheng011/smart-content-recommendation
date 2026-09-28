import axios from 'axios'

export const TOKEN_KEY = 'scr_token'

/**
 * 统一的业务异常。
 * 后端返回 { code, message, data }，拦截器已经把 message 抽出来了。
 */
export class ApiError extends Error {
  constructor(message, code) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

/* ---------------- 请求拦截：自动附带令牌 ---------------- */

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

/* ---------------- 响应拦截：拆信封 + 归一化错误 ---------------- */

http.interceptors.response.use(
  (response) => {
    const body = response.data

    // 后端统一结构：{ code, message, data }
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 200) {
        return body.data
      }
      throw new ApiError(body.message || '请求失败', body.code)
    }

    return body
  },
  (error) => {
    if (error instanceof ApiError) {
      return Promise.reject(error)
    }

    const body = error.response?.data

    let message = body?.message

    if (!message) {
      if (error.code === 'ECONNABORTED') {
        message = '请求超时，请稍后重试'
      } else if (!error.response) {
        message = '无法连接服务器，请确认后端服务已启动'
      } else {
        message = `请求失败（${error.response.status}）`
      }
    }

    return Promise.reject(
      new ApiError(message, body?.code ?? error.response?.status ?? 0)
    )
  }
)

/* ============================
   接口定义
   ============================ */

export const authApi = {
  login: (payload) => http.post('/auth/login', payload),
  register: (payload) => http.post('/auth/register', payload),
  logout: () => http.post('/auth/logout'),
  me: () => http.get('/auth/me')
}

export const contentApi = {
  list: (params) => http.get('/contents', { params }),
  detail: (id) => http.get(`/contents/${id}`),
  categories: () => http.get('/contents/categories'),
  liked: () => http.get('/contents/liked'),
  /** 我发布的文章 */
  mine: () => http.get('/contents/mine'),
  /** 我的阅读历史 */
  history: (limit = 30) => http.get('/contents/history', { params: { limit } }),
  similar: (id, limit = 4) =>
    http.get(`/contents/${id}/similar`, { params: { limit } }),
  /** 发布文章（登录用户即可） */
  create: (payload) => http.post('/contents', payload),
  /** 删除文章（管理员可删任意，普通用户仅限自己发布的） */
  remove: (id) => http.delete(`/contents/${id}`),
  like: (id) => http.post(`/contents/${id}/like`),
  unlike: (id) => http.delete(`/contents/${id}/like`)
}

export const recommendApi = {
  forMe: () => http.get('/recommend/me'),
  hot: () => http.get('/recommend/hot')
}

export const userApi = {
  me: () => http.get('/users/me'),
  myStats: () => http.get('/users/me/stats'),
  stats: (id) => http.get(`/users/${id}/stats`)
}

export const adminApi = {
  overview: () => http.get('/admin/overview'),
  users: () => http.get('/admin/users'),
  createContent: (payload) => http.post('/admin/contents', payload),
  deleteContent: (id) => http.delete(`/admin/contents/${id}`)
}

export const healthApi = {
  check: () => http.get('/health')
}

export default http
