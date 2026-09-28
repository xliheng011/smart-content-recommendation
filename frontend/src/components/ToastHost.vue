<script setup>
import { Check, CircleAlert, Info, X } from 'lucide-vue-next'
import { dismissToast, toasts } from '../store/ui'

const ICONS = {
  success: Check,
  error: CircleAlert,
  info: Info
}
</script>

<template>
  <div class="toast-host">
    <TransitionGroup name="toast">
      <div
        v-for="item in toasts"
        :key="item.id"
        class="toast glass glass-strong"
        :class="`toast-${item.type}`"
      >
        <span class="toast-icon">
          <component
            :is="ICONS[item.type] || Info"
            :size="15"
          />
        </span>

        <span class="toast-text">{{ item.message }}</span>

        <button
          class="toast-close"
          type="button"
          aria-label="关闭提示"
          @click="dismissToast(item.id)"
        >
          <X :size="13" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 200;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 9px;
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 10px 12px 10px 14px;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-1);
  pointer-events: auto;
  max-width: min(90vw, 460px);
}

.toast-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  flex-shrink: 0;
  color: #fff;
}

.toast-success .toast-icon {
  background: var(--success);
}

.toast-error .toast-icon {
  background: var(--danger);
}

.toast-info .toast-icon {
  background: var(--brand-1);
}

.toast-text {
  flex: 1;
}

.toast-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: var(--text-3);
  flex-shrink: 0;
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.toast-close:hover {
  background: var(--fill);
  color: var(--text-1);
}

/* 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.3s var(--ease), transform 0.3s var(--ease);
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-14px) scale(0.96);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}
</style>
