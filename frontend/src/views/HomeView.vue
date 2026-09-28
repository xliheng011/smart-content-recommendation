<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  BookOpen,
  Flame,
  Layers,
  RefreshCw,
  Sparkles,
  TrendingUp,
  WandSparkles
} from 'lucide-vue-next'
import { contentApi, recommendApi } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonCards from '../components/SkeletonCards.vue'
import { authState, isLoggedIn } from '../store/auth'
import { toastError, toastSuccess } from '../store/ui'
import { categoryColor, formatCount } from '../utils/format'

const router = useRouter()

const recommendations = ref([])
const hot = ref([])
const categories = ref([])

const loadingRec = ref(false)
const loadingHot = ref(true)
const recError = ref('')
const hotError = ref('')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const heroTitle = computed(() =>
  isLoggedIn() ? `${greeting.value}，${authState.user?.nickname}` : '发现值得读的内容'
)

const heroDesc = computed(() =>
  isLoggedIn()
    ? '下面这些内容来自你的浏览与点赞记录，兴趣越明确，推荐越准。'
    : '登录后系统会记录你的浏览与互动行为，为你生成专属推荐流。'
)

const totalViews = computed(() =>
  hot.value.reduce((sum, item) => sum + (item.viewCount || 0), 0)
)

async function loadRecommendations() {
  if (!isLoggedIn()) {
    recommendations.value = []
    return
  }

  loadingRec.value = true
  recError.value = ''

  try {
    recommendations.value = await recommendApi.forMe()
  } catch (error) {
    recError.value = error.message
  } finally {
    loadingRec.value = false
  }
}

async function loadHot() {
  loadingHot.value = true
  hotError.value = ''

  try {
    hot.value = await recommendApi.hot()
  } catch (error) {
    hotError.value = error.message
  } finally {
    loadingHot.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await contentApi.categories()
  } catch {
    categories.value = []
  }
}

async function refreshAll() {
  await Promise.all([loadRecommendations(), loadHot()])
  toastSuccess('已刷新推荐')
}

/**
 * 点赞：先本地乐观更新，失败再回滚，交互更跟手。
 */
async function toggleLike(item) {
  if (!isLoggedIn()) {
    toastError('请先登录后再点赞')
    router.push({ name: 'login', query: { redirect: '/' } })
    return
  }

  const nextLiked = !item.liked
  const prevLiked = item.liked
  const prevCount = item.likeCount

  item.liked = nextLiked
  item.likeCount = Math.max((prevCount || 0) + (nextLiked ? 1 : -1), 0)

  try {
    const updated = nextLiked
      ? await contentApi.like(item.id)
      : await contentApi.unlike(item.id)

    item.likeCount = updated.likeCount
    item.liked = updated.liked

    // 热门榜里的同一条内容同步状态
    const mirrored = hot.value.find((entry) => entry.id === item.id)
    if (mirrored) {
      mirrored.likeCount = updated.likeCount
      mirrored.liked = updated.liked
    }
  } catch (error) {
    item.liked = prevLiked
    item.likeCount = prevCount
    toastError(error.message)
  }
}

function openDetail(item) {
  router.push({ name: 'detail', params: { id: item.id } })
}

function goExplore(category) {
  router.push({
    name: 'explore',
    query: category ? { category } : {}
  })
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: '/' } })
}

onMounted(() => {
  loadHot()
  loadCategories()
  loadRecommendations()
})

// 登录 / 退出后重新拉取个性化推荐
watch(
  () => authState.token,
  () => loadRecommendations()
)
</script>

<template>
  <main class="page">
    <!-- ============ Hero ============ -->
    <section class="hero glass rise-in">
      <span class="tag-eyebrow">
        <Sparkles :size="12" />
        Personalized Recommendation
      </span>

      <h1 class="hero-title">{{ heroTitle }}</h1>

      <p class="hero-desc">{{ heroDesc }}</p>

      <div class="hero-stats">
        <div class="hero-stat">
          <Layers :size="15" />
          <strong>{{ categories.length }}</strong>
          <span>个分类</span>
        </div>

        <div class="hero-divider"></div>

        <div class="hero-stat">
          <Flame :size="15" />
          <strong>{{ hot.length }}</strong>
          <span>条热门</span>
        </div>

        <div class="hero-divider"></div>

        <div class="hero-stat">
          <TrendingUp :size="15" />
          <strong>{{ formatCount(totalViews) }}</strong>
          <span>累计阅读</span>
        </div>
      </div>

      <div
        v-if="!isLoggedIn()"
        class="hero-cta"
      >
        <button
          class="btn btn-primary"
          type="button"
          @click="goLogin"
        >
          登录体验个性化推荐
          <ArrowRight :size="15" />
        </button>
      </div>
    </section>

    <!-- ============ 主体 ============ -->
    <div class="layout">
      <!-- 推荐流 -->
      <section class="main-col">
        <div class="section-head">
          <div>
            <span class="tag-eyebrow">
              <WandSparkles :size="12" />
              For You
            </span>
            <h2 class="section-title">为你推荐</h2>
          </div>

          <button
            class="btn btn-glass"
            type="button"
            :disabled="loadingRec || !isLoggedIn()"
            @click="refreshAll"
          >
            <RefreshCw
              :size="15"
              :class="{ spin: loadingRec }"
            />
            刷新
          </button>
        </div>

        <SkeletonCards
          v-if="loadingRec"
          :count="4"
        />

        <EmptyState
          v-else-if="!isLoggedIn()"
          title="登录后开启个性化推荐"
          description="系统会分析你浏览和点赞过的内容，逐步学习你的兴趣偏好。"
          action-text="去登录"
          @action="goLogin"
        >
          <template #icon>
            <Sparkles :size="26" />
          </template>
        </EmptyState>

        <EmptyState
          v-else-if="recError"
          title="推荐加载失败"
          :description="recError"
          action-text="重新加载"
          @action="loadRecommendations"
        />

        <EmptyState
          v-else-if="!recommendations.length"
          title="暂时没有可推荐的内容"
          description="先去发现页浏览几篇内容，系统会更快了解你的偏好。"
          action-text="去发现"
          @action="goExplore()"
        />

        <div
          v-else
          class="grid-cards stagger"
        >
          <ContentCard
            v-for="item in recommendations"
            :key="item.id"
            :item="item"
            :reason="item.reason"
            @open="openDetail"
            @like="toggleLike"
          />
        </div>
      </section>

      <!-- 侧栏 -->
      <aside class="side-col">
        <!-- 热门榜 -->
        <div class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Flame :size="15" />
              热门榜单
            </span>
            <span class="panel-hint">点赞×3 + 阅读</span>
          </div>

          <SkeletonCards
            v-if="loadingHot"
            variant="row"
            :count="5"
          />

          <p
            v-else-if="hotError"
            class="panel-error"
          >
            {{ hotError }}
          </p>

          <ol
            v-else
            class="rank-list"
          >
            <li
              v-for="item in hot"
              :key="item.id"
              class="rank-item"
              @click="openDetail(item)"
            >
              <span
                class="rank-no"
                :class="{ 'rank-no-top': item.rank <= 3 }"
              >
                {{ item.rank }}
              </span>

              <div class="rank-body">
                <div class="rank-title clamp-2">{{ item.title }}</div>
                <div class="rank-meta">
                  <span>{{ item.authorName }}</span>
                  <span>·</span>
                  <span>{{ formatCount(item.likeCount) }} 赞</span>
                </div>
              </div>
            </li>
          </ol>
        </div>

        <!-- 分类 -->
        <div
          v-if="categories.length"
          class="panel glass"
        >
          <div class="panel-head">
            <span class="panel-title">
              <BookOpen :size="15" />
              分类浏览
            </span>
          </div>

          <div class="cat-list">
            <button
              v-for="cat in categories"
              :key="cat.name"
              class="cat-item"
              type="button"
              @click="goExplore(cat.name)"
            >
              <span
                class="cat-dot"
                :style="{ background: categoryColor(cat.name) }"
              ></span>
              <span class="cat-name">{{ cat.name }}</span>
              <span class="cat-count">{{ cat.count }}</span>
            </button>
          </div>
        </div>
      </aside>
    </div>
  </main>
</template>

<style scoped>
/* ---------- Hero ---------- */

.hero {
  padding: 40px 38px;
  margin-bottom: 30px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.hero-title {
  margin-top: 16px;
  font-size: clamp(28px, 4vw, 42px);
  letter-spacing: -0.035em;
}

.hero-desc {
  margin-top: 14px;
  max-width: 560px;
  font-size: 14px;
  line-height: 1.75;
  color: var(--text-2);
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-top: 26px;
  padding: 12px 24px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid var(--glass-border);
}

.hero-stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  color: var(--text-2);
}

.hero-stat strong {
  font-size: 15px;
  font-weight: 750;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}

.hero-divider {
  width: 1px;
  height: 16px;
  background: var(--line-strong);
}

.hero-cta {
  margin-top: 24px;
}

/* ---------- 布局 ---------- */

.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 322px;
  gap: 22px;
  align-items: start;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.section-title {
  font-size: 21px;
  margin-top: 5px;
}

/* ---------- 侧栏面板 ---------- */

.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
  position: sticky;
  top: 86px;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 14px;
  font-weight: 700;
}

.panel-hint {
  font-size: 10.5px;
  color: var(--text-3);
}

.panel-error {
  font-size: 12.5px;
  color: var(--danger);
  padding: 12px 0;
}

/* ---------- 榜单 ---------- */

.rank-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.rank-item {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 10px;
  border-radius: var(--r-xs);
  cursor: pointer;
  transition: background 0.22s var(--ease), transform 0.22s var(--ease);
}

.rank-item:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateX(3px);
}

.rank-no {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
  color: var(--text-3);
  background: var(--line);
  font-variant-numeric: tabular-nums;
}

.rank-no-top {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: 0 4px 12px rgba(9, 9, 11, 0.28);
}

.rank-body {
  min-width: 0;
  flex: 1;
}

.rank-title {
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text-1);
}

.rank-item:hover .rank-title {
  color: var(--brand-1);
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
  font-size: 10.5px;
  color: var(--text-3);
}

/* ---------- 分类 ---------- */

.cat-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border-radius: var(--r-xs);
  font-size: 13px;
  color: var(--text-2);
  transition: background 0.22s var(--ease), color 0.22s var(--ease);
}

.cat-item:hover {
  background: rgba(255, 255, 255, 0.7);
  color: var(--brand-1);
}

.cat-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cat-name {
  flex: 1;
  text-align: left;
  font-weight: 600;
}

.cat-count {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: var(--r-pill);
  color: var(--text-2);
  background: var(--fill-strong);
  font-variant-numeric: tabular-nums;
}

/* ---------- 响应式 ---------- */

@media (max-width: 980px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .side-col {
    position: static;
    order: 2;
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 30px 20px;
  }

  .hero-stats {
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
    padding: 12px 18px;
  }

  .hero-divider {
    display: none;
  }
}
</style>
