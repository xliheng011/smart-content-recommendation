<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppDock from './components/AppDock.vue'
import AppHeader from './components/AppHeader.vue'
import AuroraBackground from './components/AuroraBackground.vue'
import ToastHost from './components/ToastHost.vue'

const route = useRoute()

const showDock = computed(() => !route.meta?.hideDock)
</script>

<template>
  <!-- 极光背景铺在最底层，玻璃面板靠 backdrop-filter 透出它 -->
  <AuroraBackground />

  <div class="shell">
    <AppHeader />

    <RouterView v-slot="{ Component }">
      <Transition
        name="fade"
        mode="out-in"
      >
        <component :is="Component" />
      </Transition>
    </RouterView>
  </div>

  <AppDock v-if="showDock" />

  <ToastHost />
</template>

<style scoped>
.shell {
  position: relative;
  z-index: 1;
  min-height: 100vh;
}
</style>
