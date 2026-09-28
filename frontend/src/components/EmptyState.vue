<script setup>
import { Inbox } from 'lucide-vue-next'

defineProps({
  title: {
    type: String,
    default: '这里还没有内容'
  },
  description: {
    type: String,
    default: ''
  },
  actionText: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['action'])
</script>

<template>
  <div class="empty glass glass-soft">
    <span class="empty-icon">
      <slot name="icon">
        <Inbox :size="26" />
      </slot>
    </span>

    <h3 class="empty-title">{{ title }}</h3>

    <p
      v-if="description"
      class="empty-desc"
    >
      {{ description }}
    </p>

    <button
      v-if="actionText"
      class="btn btn-glass empty-action"
      type="button"
      @click="emit('action')"
    >
      {{ actionText }}
    </button>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 52px 26px;
}

.empty-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  margin-bottom: 16px;
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-sm);
}

.empty-title {
  font-size: 16px;
  margin-bottom: 7px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-2);
  max-width: 420px;
  line-height: 1.7;
}

.empty-action {
  margin-top: 18px;
}
</style>
