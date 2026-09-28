<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  Compass,
  House,
  LayoutDashboard,
  PenLine,
  UserRound
} from 'lucide-vue-next'
import { isAdmin, isLoggedIn } from '../store/auth'

const route = useRoute()

const items = computed(() => {
  const list = [
    { name: 'home', label: '首页', icon: House },
    { name: 'explore', label: '发现', icon: Compass }
  ]

  if (isLoggedIn()) {
    list.push({ name: 'publish', label: '写文章', icon: PenLine })
  }

  list.push(
    isLoggedIn()
      ? { name: 'profile', label: '我的', icon: UserRound }
      : { name: 'login', label: '登录', icon: UserRound }
  )

  if (isAdmin()) {
    list.push({ name: 'admin', label: '管理', icon: LayoutDashboard })
  }

  return list
})

function isActive(name) {
  if (name === 'home') {
    return route.name === 'home'
  }
  return route.name === name
}
</script>

<template>
  <nav class="dock glass">
    <RouterLink
      v-for="item in items"
      :key="item.name"
      class="dock-item"
      :class="{ 'dock-item-active': isActive(item.name) }"
      :to="{ name: item.name }"
    >
      <component
        :is="item.icon"
        :size="19"
      />
      <span class="dock-label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.dock {
  position: fixed;
  left: 50%;
  bottom: 22px;
  transform: translateX(-50%);
  z-index: 50;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 7px;
  border-radius: var(--r-pill);
  box-shadow: var(--shadow-lg), inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.dock-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  min-width: 62px;
  padding: 8px 12px;
  border-radius: var(--r-pill);
  color: var(--text-3);
  transition: all 0.26s var(--ease);
}

.dock-item:hover {
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.75);
}

.dock-label {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.dock-item-active {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: var(--shadow-brand);
}

.dock-item-active:hover {
  color: #fff;
  background: var(--brand-grad);
}

@media (max-width: 520px) {
  .dock {
    bottom: 14px;
    gap: 2px;
    padding: 6px;
  }

  .dock-item {
    min-width: 54px;
    padding: 7px 9px;
  }
}
</style>
