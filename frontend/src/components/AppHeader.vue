<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Compass,
  House,
  LayoutDashboard,
  LogOut,
  PenLine,
  Sparkles,
  UserRound
} from 'lucide-vue-next'
import { authState, isAdmin, isLoggedIn, logout } from '../store/auth'
import { toastSuccess } from '../store/ui'

const router = useRouter()
const route = useRoute()

const menuOpen = ref(false)

const links = computed(() => {
  const base = [
    { name: 'home', label: '首页', icon: House },
    { name: 'explore', label: '发现', icon: Compass }
  ]

  if (isLoggedIn()) {
    base.push({ name: 'publish', label: '写文章', icon: PenLine })
    base.push({ name: 'profile', label: '我的', icon: UserRound })
  }

  if (isAdmin()) {
    base.push({ name: 'admin', label: '管理', icon: LayoutDashboard })
  }

  return base
})

/**
 * 路由变化后收起下拉。
 * 菜单项本身也写了 @click 收起，但导航栏/Dock 触发的跳转同样需要关闭，
 * 统一在这里兜底，避免菜单"跟着页面走"。
 */
watch(
  () => route.fullPath,
  () => {
    menuOpen.value = false
  }
)

const avatarText = computed(() => {
  const source = authState.user?.nickname || authState.user?.username || '?'
  return source.trim().charAt(0).toUpperCase()
})

async function handleLogout() {
  menuOpen.value = false
  await logout()
  toastSuccess('已退出登录')
  router.push({ name: 'home' })
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}
</script>

<template>
  <header class="header">
    <div class="header-inner">
      <RouterLink
        class="brand"
        :to="{ name: 'home' }"
      >
        <span class="brand-mark">
          <Sparkles :size="16" />
        </span>

        <span class="brand-text">
          <strong>Smart Recommend</strong>
          <em>智能内容推荐</em>
        </span>
      </RouterLink>

      <nav class="nav">
        <RouterLink
          v-for="link in links"
          :key="link.name"
          class="nav-item"
          :class="{ 'nav-item-active': route.name === link.name }"
          :to="{ name: link.name }"
        >
          <component
            :is="link.icon"
            :size="15"
          />
          <span>{{ link.label }}</span>
        </RouterLink>
      </nav>

      <div
        v-if="isLoggedIn()"
        class="user"
      >
        <button
          class="user-trigger"
          type="button"
          @click="menuOpen = !menuOpen"
        >
          <span class="avatar">{{ avatarText }}</span>
          <span class="user-name">{{ authState.user?.nickname }}</span>
        </button>

        <Transition name="pop">
          <div
            v-if="menuOpen"
            class="menu glass glass-strong"
          >
            <div class="menu-head">
              <span class="avatar avatar-sm">{{ avatarText }}</span>
              <div>
                <div class="menu-name">{{ authState.user?.nickname }}</div>
                <div class="menu-role">
                  {{ isAdmin() ? '管理员' : '普通用户' }}
                </div>
              </div>
            </div>

            <hr class="glass-divider" />

            <RouterLink
              class="menu-item"
              :to="{ name: 'publish' }"
              @click="menuOpen = false"
            >
              <PenLine :size="15" />
              写文章
            </RouterLink>

            <RouterLink
              class="menu-item"
              :to="{ name: 'profile' }"
              @click="menuOpen = false"
            >
              <UserRound :size="15" />
              个人中心
            </RouterLink>

            <RouterLink
              v-if="isAdmin()"
              class="menu-item"
              :to="{ name: 'admin' }"
              @click="menuOpen = false"
            >
              <LayoutDashboard :size="15" />
              管理后台
            </RouterLink>

            <button
              class="menu-item menu-item-danger"
              type="button"
              @click="handleLogout"
            >
              <LogOut :size="15" />
              退出登录
            </button>
          </div>
        </Transition>
      </div>

      <button
        v-else
        class="btn btn-primary btn-login"
        type="button"
        @click="goLogin"
      >
        登录
      </button>
    </div>

    <!-- 点击空白关闭下拉 -->
    <div
      v-if="menuOpen"
      class="menu-mask"
      @click="menuOpen = false"
    ></div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 60;
  padding: 12px 20px 0;
}

.header-inner {
  max-width: 1120px;
  margin: 0 auto;
  height: 58px;
  padding: 0 12px 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;

  /*
   * 关键：这里的 backdrop-filter 会让 .header-inner 生成一个新的层叠上下文，
   * 于是内部 .menu 的 z-index 只在 .header-inner 内部有效，
   * 无法盖住同为 .header 子元素的 .menu-mask。
   * 结果是遮罩把下拉菜单整个罩住、吃掉所有点击（表现为"点了没反应"）。
   * 给 .header-inner 自身提升层级，让它整体位于遮罩之上即可。
   */
  position: relative;
  z-index: 70;

  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--glass-border);
  border-radius: var(--r-pill);
  box-shadow: var(--shadow-md), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* ---------- 品牌 ---------- */

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 11px;
  background: var(--brand-grad);
  color: #fff;
  box-shadow: var(--shadow-brand);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.brand-text strong {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.2px;
}

.brand-text em {
  font-size: 10px;
  font-style: normal;
  color: var(--text-3);
  letter-spacing: 0.04em;
}

/* ---------- 导航 ---------- */

.nav {
  display: flex;
  align-items: center;
  gap: 3px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  transition: all 0.24s var(--ease);
}

.nav-item:hover {
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.72);
}

.nav-item-active {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: var(--shadow-brand);
}

.nav-item-active:hover {
  color: #fff;
  background: var(--brand-grad);
}

/* ---------- 用户 ---------- */

.user {
  position: relative;
  flex-shrink: 0;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 4px 12px 4px 4px;
  border-radius: var(--r-pill);
  transition: background 0.24s var(--ease);
}

.user-trigger:hover {
  background: rgba(255, 255, 255, 0.8);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  box-shadow: var(--shadow-brand);
  flex-shrink: 0;
}

.avatar-sm {
  width: 32px;
  height: 32px;
  font-size: 13px;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-1);
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-login {
  padding: 9px 20px;
  flex-shrink: 0;
}

/* ---------- 下拉菜单 ---------- */

.menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 216px;
  padding: 10px;
  z-index: 70;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.menu-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px 10px;
}

.menu-name {
  font-size: 13px;
  font-weight: 700;
}

.menu-role {
  font-size: 11px;
  color: var(--text-3);
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 9px;
  width: 100%;
  padding: 9px 10px;
  border-radius: var(--r-xs);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-2);
  text-align: left;
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.menu-item:hover {
  background: var(--fill);
  color: var(--brand-1);
}

.menu-item-danger:hover {
  background: rgba(164, 38, 44, 0.09);
  color: var(--danger);
}

.menu-mask {
  position: fixed;
  inset: 0;
  z-index: 65;
}

.pop-enter-active,
.pop-leave-active {
  transition: opacity 0.2s var(--ease), transform 0.2s var(--ease);
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}

/* ---------- 响应式 ---------- */

@media (max-width: 860px) {
  .nav {
    display: none;
  }

  .user-name {
    display: none;
  }
}

@media (max-width: 520px) {
  .header {
    padding: 10px 12px 0;
  }

  .brand-text em {
    display: none;
  }
}
</style>
