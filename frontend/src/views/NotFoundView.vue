<script setup>
import { useRouter } from 'vue-router'
import { Compass, House } from 'lucide-vue-next'

const router = useRouter()
</script>

<template>
  <main class="page notfound">
    <div class="card glass rise-in">
      <div class="code grad-text">404</div>

      <h1 class="title">这个页面不存在</h1>

      <p class="desc">
        链接可能已经失效，或者地址输错了。回到首页继续浏览吧。
      </p>

      <div class="actions">
        <button
          class="btn btn-primary"
          type="button"
          @click="router.push({ name: 'home' })"
        >
          <House :size="15" />
          返回首页
        </button>

        <button
          class="btn btn-glass"
          type="button"
          @click="router.push({ name: 'explore' })"
        >
          <Compass :size="15" />
          去发现
        </button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.notfound {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 200px);
}

.card {
  width: 100%;
  max-width: 460px;
  padding: 48px 36px;
  text-align: center;
}

.code {
  font-size: 74px;
  font-weight: 800;
  letter-spacing: -0.05em;
  line-height: 1;
}

.title {
  margin-top: 14px;
  font-size: 21px;
}

.desc {
  margin-top: 12px;
  font-size: 13.5px;
  color: var(--text-2);
  line-height: 1.75;
}

.actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 26px;
}
</style>
