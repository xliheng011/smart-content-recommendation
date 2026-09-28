import { createRouter, createWebHistory } from 'vue-router'
import { authState, isAdmin, isLoggedIn } from '../store/auth'
import { toastError } from '../store/ui'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomeView.vue'),
    meta: { title: '首页' }
  },
  {
    path: '/explore',
    name: 'explore',
    component: () => import('../views/ExploreView.vue'),
    meta: { title: '发现' }
  },
  {
    path: '/content/:id',
    name: 'detail',
    component: () => import('../views/DetailView.vue'),
    meta: { title: '详情' }
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: { title: '登录', hideDock: true }
  },
  {
    path: '/publish',
    name: 'publish',
    component: () => import('../views/PublishView.vue'),
    meta: { title: '写文章', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { title: '我的', requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'admin',
    component: () => import('../views/AdminView.vue'),
    meta: { title: '管理', requiresAdmin: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/NotFoundView.vue'),
    meta: { title: '未找到' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !isLoggedIn()) {
    toastError('请先登录后再访问')
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.requiresAdmin) {
    if (!isLoggedIn()) {
      toastError('请先登录后再访问')
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    if (!isAdmin()) {
      toastError('当前账号没有管理员权限')
      return { name: 'home' }
    }
  }

  return true
})

router.afterEach((to) => {
  const base = '智能内容推荐'
  document.title = to.meta?.title ? `${to.meta.title} · ${base}` : base
})

export { authState }
export default router
