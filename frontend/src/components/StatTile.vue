<script setup>
defineProps({
  label: {
    type: String,
    required: true
  },
  value: {
    type: [String, Number],
    default: 0
  },
  hint: {
    type: String,
    default: ''
  }
})
</script>

<template>
  <div class="tile glass">
    <span class="tile-icon">
      <slot name="icon" />
    </span>

    <div class="tile-body">
      <div class="tile-value">{{ value }}</div>
      <div class="tile-label">{{ label }}</div>
      <div
        v-if="hint"
        class="tile-hint"
      >
        {{ hint }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.tile {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

/*
 * 图标块统一为近黑。
 * 原来的彩色 tone 在黑白灰主题下没有等价物：
 * 浅灰配白色图标对比度不足，深灰之间又难以分辨，
 * 所以这里直接统一，靠数值本身的层级来区分主次。
 */
.tile-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: var(--r-sm);
  flex-shrink: 0;
  color: #fff;
  background: var(--brand-grad);
  box-shadow: var(--shadow-brand);
}

.tile-body {
  min-width: 0;
}

.tile-value {
  font-size: 23px;
  font-weight: 750;
  letter-spacing: -0.03em;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}

.tile-label {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 3px;
}

.tile-hint {
  font-size: 11px;
  color: var(--text-3);
  margin-top: 2px;
}
</style>
