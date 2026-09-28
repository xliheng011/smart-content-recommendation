<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Calendar,
  Eye,
  Heart,
  RefreshCw,
  Sparkles,
  UserRound
} from 'lucide-vue-next'
import { contentApi } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonCards from '../components/SkeletonCards.vue'
import { authState, isLoggedIn } from '../store/auth'
import { toastError, toastSuccess } from '../store/ui'
import { categoryColor, formatCount } from '../utils/format'

const route = useRoute()
const router = useRouter()

const article = ref(null)
const similar = ref([])

const loading = ref(true)
const loadingSimilar = ref(false)
const error = ref('')
const liking = ref(false)

const accent = computed(() => categoryColor(article.value?.category))

const paragraphs = computed(() => {
  const text = article.value?.content || ''
  return text
    .split(/\n+/)
    .map((line) => line.trim())
    .filter(Boolean)
})

async function loadArticle() {
  loading.value = true
  error.value = ''
  similar.value = []

  const id = route.params.id

  try {
    article.value = await contentApi.detail(id)
    loadSimilar(id)
  } catch (err) {
    error.value = err.message
    article.value = null
  } finally {
    loading.value = false
  }
}

async function loadSimilar(id) {
  loadingSimilar.value = true

  try {
    similar.value = await contentApi.similar(id, 3)
  } catch {
    similar.value = []
  } finally {
    loadingSimilar.value = false
  }
}

async function toggleLike() {
  if (!article.value) return

  if (!isLoggedIn()) {
    toastError('请先登录后再点赞')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }

  const nextLiked = !article.value.liked

  liking.value = true

  try {
    const updated = nextLiked
      ? await contentApi.like(article.value.id)
      : await contentApi.unlike(article.value.id)

    article.value.liked = updated.liked
    article.value.likeCount = updated.likeCount

    toastSuccess(nextLiked ? '已加入喜欢' : '已取消点赞')
  } catch (err) {
    toastError(err.message)
  } finally {
    liking.value = false
  }
}

function openDetail(item) {
  router.push({ name: 'detail', params: { id: item.id } })
}

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push({ name: 'explore' })
  }
}

onMounted(loadArticle)

// 在详情页内点相关阅读时，路由参数变化但组件复用，需要手动重新拉取
watch(() => route.params.id, (next, prev) => {
  if (route.name === 'detail' && next !== prev) {
    loadArticle()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
})

watch(() => authState.token, () => {
  if (route.name === 'detail') loadArticle()
})
</script>

<template>
  <main class="page">
    <button
      class="btn btn-glass back"
      type="button"
      @click="goBack"
    >
      <ArrowLeft :size="15" />
      返回
    </button>

    <SkeletonCards
      v-if="loading"
      :count="1"
    />

    <EmptyState
      v-else-if="error"
      title="内容加载失败"
      :description="error"
      action-text="重新加载"
      @action="loadArticle"
    />

    <template v-else-if="article">
      <article class="article glass rise-in">
        <span
          class="article-accent"
          :style="{ background: accent }"
        ></span>

        <header class="article-head">
          <span class="article-cat">
            <span
              class="article-cat-dot"
              :style="{ background: accent }"
            ></span>
            {{ article.category || '推荐' }}
          </span>

          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-meta">
            <span class="meta-item">
              <UserRound :size="14" />
              {{ article.authorName || '匿名作者' }}
            </span>

            <span
              v-if="article.createdAt"
              class="meta-item"
            >
              <Calendar :size="14" />
              {{ article.createdAt }}
            </span>

            <span class="meta-item">
              <Eye :size="14" />
              {{ formatCount(article.viewCount) }} 次阅读
            </span>
          </div>
        </header>

        <hr class="glass-divider" />

        <div class="article-body">
          <p
            v-for="(line, index) in paragraphs"
            :key="index"
          >
            {{ line }}
          </p>
        </div>

        <footer class="article-foot">
          <button
            class="like-btn"
            :class="{ 'like-btn-on': article.liked }"
            type="button"
            :disabled="liking"
            @click="toggleLike"
          >
            <Heart
              :size="17"
              :fill="article.liked ? 'currentColor' : 'none'"
            />
            <span>
              {{ article.liked ? '已喜欢' : '喜欢这篇内容' }}
            </span>
            <span class="like-count">{{ formatCount(article.likeCount) }}</span>
          </button>

          <button
            class="btn btn-ghost"
            type="button"
            @click="loadArticle"
          >
            <RefreshCw :size="14" />
            刷新
          </button>
        </footer>
      </article>

      <!-- 相关阅读 -->
      <section
        v-if="loadingSimilar || similar.length"
        class="similar"
      >
        <div class="similar-head">
          <span class="tag-eyebrow">
            <Sparkles :size="12" />
            Related
          </span>
          <h2 class="similar-title">相关阅读</h2>
        </div>

        <SkeletonCards
          v-if="loadingSimilar"
          :count="3"
        />

        <div
          v-else
          class="grid-cards stagger"
        >
          <ContentCard
            v-for="item in similar"
            :key="item.id"
            :item="item"
            @open="openDetail"
          />
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
.back {
  margin-bottom: 18px;
}

/* ---------- 文章 ---------- */

.article {
  position: relative;
  padding: 44px 48px;
  overflow: hidden;
}

.article-accent {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 4px;
  opacity: 0.8;
}

.article-head {
  margin-bottom: 24px;
}

.article-cat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 13px 5px 11px;
  border-radius: var(--r-pill);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.65);
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-2);
}

.article-cat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.article-title {
  margin: 18px 0 0;
  font-size: clamp(24px, 3.2vw, 34px);
  line-height: 1.34;
  letter-spacing: -0.03em;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 18px;
  margin-top: 18px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-3);
}

.article-body {
  margin-top: 26px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.article-body p {
  font-size: 15.5px;
  line-height: 1.95;
  color: var(--text-2);
}

.article-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 34px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
}

.like-btn {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  padding: 12px 22px;
  border-radius: var(--r-pill);
  font-size: 13.5px;
  font-weight: 650;
  color: var(--text-2);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--glass-border);
  box-shadow: var(--shadow-sm);
  transition: all 0.26s var(--ease);
}

.like-btn:hover:not(:disabled) {
  color: var(--text-1);
  border-color: var(--line-strong);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.like-btn-on {
  color: #fff;
  background: var(--brand-grad);
  border-color: transparent;
  box-shadow: var(--shadow-brand);
}

.like-btn-on:hover:not(:disabled) {
  color: #fff;
}

.like-count {
  padding-left: 9px;
  margin-left: 2px;
  border-left: 1px solid currentColor;
  opacity: 0.75;
  font-variant-numeric: tabular-nums;
}

/* ---------- 相关阅读 ---------- */

.similar {
  margin-top: 34px;
}

.similar-head {
  margin-bottom: 16px;
}

.similar-title {
  font-size: 20px;
  margin-top: 5px;
}

/* ---------- 响应式 ---------- */

@media (max-width: 640px) {
  .article {
    padding: 28px 20px;
  }

  .article-body p {
    font-size: 15px;
    line-height: 1.85;
  }

  .article-foot {
    flex-direction: column;
    align-items: stretch;
  }

  .like-btn {
    justify-content: center;
  }
}
</style>
