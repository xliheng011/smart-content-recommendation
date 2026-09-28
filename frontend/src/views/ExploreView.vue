<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChevronLeft,
  ChevronRight,
  Compass,
  Search,
  SlidersHorizontal,
  X
} from 'lucide-vue-next'
import { contentApi } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonCards from '../components/SkeletonCards.vue'
import { isLoggedIn } from '../store/auth'
import { toastError } from '../store/ui'
import { categoryColor } from '../utils/format'

const route = useRoute()
const router = useRouter()

const PAGE_SIZE = 9

const items = ref([])
const categories = ref([])
const total = ref(0)
const totalPages = ref(0)
const page = ref(1)

const keyword = ref('')
const category = ref('')
const sort = ref('latest')

const loading = ref(true)
const error = ref('')

const SORTS = [
  { key: 'latest', label: '最新' },
  { key: 'hot', label: '最热' },
  { key: 'views', label: '最多阅读' }
]

const hasFilter = computed(() => Boolean(keyword.value || category.value))

const rangeText = computed(() => {
  if (!total.value) return '暂无内容'
  const start = (page.value - 1) * PAGE_SIZE + 1
  const end = Math.min(page.value * PAGE_SIZE, total.value)
  return `第 ${start}-${end} 条，共 ${total.value} 条`
})

/** 把筛选条件写进地址栏，方便分享与回退 */
function syncQuery() {
  const query = {}

  if (keyword.value) query.keyword = keyword.value
  if (category.value) query.category = category.value
  if (sort.value !== 'latest') query.sort = sort.value
  if (page.value > 1) query.page = String(page.value)

  router.replace({ name: 'explore', query })
}

function readQuery() {
  keyword.value = route.query.keyword ? String(route.query.keyword) : ''
  category.value = route.query.category ? String(route.query.category) : ''
  sort.value = route.query.sort ? String(route.query.sort) : 'latest'
  page.value = route.query.page ? Math.max(Number(route.query.page) || 1, 1) : 1
}

async function load() {
  loading.value = true
  error.value = ''

  try {
    const result = await contentApi.list({
      page: page.value,
      size: PAGE_SIZE,
      keyword: keyword.value || undefined,
      category: category.value || undefined,
      sort: sort.value
    })

    items.value = result.items || []
    total.value = result.total || 0
    totalPages.value = result.totalPages || 0
  } catch (err) {
    error.value = err.message
    items.value = []
    total.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await contentApi.categories()
  } catch {
    categories.value = []
  }
}

/* ---------------- 交互 ---------------- */

let searchTimer = null

function onKeywordInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    syncQuery()
    load()
  }, 380)
}

function clearKeyword() {
  keyword.value = ''
  page.value = 1
  syncQuery()
  load()
}

function pickCategory(name) {
  category.value = category.value === name ? '' : name
  page.value = 1
  syncQuery()
  load()
}

function pickSort(key) {
  sort.value = key
  page.value = 1
  syncQuery()
  load()
}

function resetFilters() {
  keyword.value = ''
  category.value = ''
  sort.value = 'latest'
  page.value = 1
  syncQuery()
  load()
}

function goPage(next) {
  if (next < 1 || next > totalPages.value || next === page.value) {
    return
  }

  page.value = next
  syncQuery()
  load()

  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function openDetail(item) {
  router.push({ name: 'detail', params: { id: item.id } })
}

async function toggleLike(item) {
  if (!isLoggedIn()) {
    toastError('请先登录后再点赞')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
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
  } catch (err) {
    item.liked = prevLiked
    item.likeCount = prevCount
    toastError(err.message)
  }
}

/* ---------------- 生命周期 ---------------- */

onMounted(() => {
  readQuery()
  load()
  loadCategories()
})

// 支持从其它页面跳转过来时带上不同的筛选参数
watch(
  () => route.query,
  (next, prev) => {
    if (route.name !== 'explore') return
    if (JSON.stringify(next) === JSON.stringify(prev)) return
    readQuery()
    load()
  }
)
</script>

<template>
  <main class="page">
    <!-- 头部 -->
    <section class="head glass rise-in">
      <span class="tag-eyebrow">
        <Compass :size="12" />
        Explore
      </span>

      <h1 class="head-title">发现内容</h1>

      <p class="head-desc">
        支持关键字搜索、分类筛选与多种排序方式，快速定位你关心的主题。
      </p>

      <!-- 搜索框 -->
      <div class="search">
        <Search
          :size="17"
          class="search-icon"
        />

        <input
          v-model="keyword"
          class="search-input"
          type="search"
          placeholder="搜索标题或正文，例如：设计、推荐系统、阅读"
          @input="onKeywordInput"
        />

        <button
          v-if="keyword"
          class="search-clear"
          type="button"
          aria-label="清空搜索"
          @click="clearKeyword"
        >
          <X :size="15" />
        </button>
      </div>

      <!-- 筛选 -->
      <div class="filters">
        <div class="filter-row">
          <span class="filter-label">
            <SlidersHorizontal :size="12" />
            分类
          </span>

          <div class="chips">
            <button
              class="chip chip-interactive"
              :class="{ 'chip-active': !category }"
              type="button"
              @click="pickCategory('')"
            >
              全部
            </button>

            <button
              v-for="cat in categories"
              :key="cat.name"
              class="chip chip-interactive"
              :class="{ 'chip-active': category === cat.name }"
              type="button"
              @click="pickCategory(cat.name)"
            >
              <span
                v-if="category !== cat.name"
                class="dot"
                :style="{ background: categoryColor(cat.name) }"
              ></span>
              {{ cat.name }}
              <span class="chip-count">{{ cat.count }}</span>
            </button>
          </div>
        </div>

        <div class="filter-row">
          <span class="filter-label">排序</span>

          <div class="chips">
            <button
              v-for="option in SORTS"
              :key="option.key"
              class="chip chip-interactive"
              :class="{ 'chip-active': sort === option.key }"
              type="button"
              @click="pickSort(option.key)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </div>
    </section>

    <!-- 结果 -->
    <section class="result-head">
      <span class="result-count">{{ rangeText }}</span>

      <button
        v-if="hasFilter"
        class="btn btn-ghost"
        type="button"
        @click="resetFilters"
      >
        <X :size="14" />
        清空筛选
      </button>
    </section>

    <SkeletonCards
      v-if="loading"
      :count="6"
    />

    <EmptyState
      v-else-if="error"
      title="内容加载失败"
      :description="error"
      action-text="重新加载"
      @action="load"
    />

    <EmptyState
      v-else-if="!items.length"
      title="没有找到匹配的内容"
      description="换个关键字，或者清空筛选条件再试试。"
      action-text="清空筛选"
      @action="resetFilters"
    />

    <div
      v-else
      class="grid-cards stagger"
    >
      <ContentCard
        v-for="item in items"
        :key="item.id"
        :item="item"
        @open="openDetail"
        @like="toggleLike"
      />
    </div>

    <!-- 分页 -->
    <div
      v-if="totalPages > 1"
      class="pager glass"
    >
      <button
        class="pager-btn"
        type="button"
        :disabled="page <= 1"
        @click="goPage(page - 1)"
      >
        <ChevronLeft :size="16" />
        上一页
      </button>

      <span class="pager-info">{{ page }} / {{ totalPages }}</span>

      <button
        class="pager-btn"
        type="button"
        :disabled="page >= totalPages"
        @click="goPage(page + 1)"
      >
        下一页
        <ChevronRight :size="16" />
      </button>
    </div>
  </main>
</template>

<style scoped>
/* ---------- 头部 ---------- */

.head {
  padding: 30px 32px;
  margin-bottom: 20px;
  text-align: center;
}

.head-title {
  margin-top: 14px;
  font-size: clamp(25px, 3.4vw, 34px);
  letter-spacing: -0.03em;
}

.head-desc {
  margin: 12px auto 0;
  max-width: 520px;
  font-size: 13.5px;
  color: var(--text-2);
  line-height: 1.7;
}

/* ---------- 搜索 ---------- */

.search {
  position: relative;
  display: flex;
  align-items: center;
  max-width: 560px;
  margin: 24px auto 0;
}

.search-icon {
  position: absolute;
  left: 17px;
  color: var(--text-3);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 13px 44px 13px 45px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--glass-border);
  outline: none;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: var(--shadow-sm);
  transition: border-color 0.22s var(--ease), box-shadow 0.22s var(--ease),
    background 0.22s var(--ease);
}

.search-input::-webkit-search-cancel-button {
  display: none;
}

.search-input:focus {
  background: #fff;
  border-color: rgba(9, 9, 11, 0.42);
  box-shadow: 0 0 0 4px var(--ring);
}

.search-clear {
  position: absolute;
  right: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  color: var(--text-3);
  transition: background 0.2s var(--ease), color 0.2s var(--ease);
}

.search-clear:hover {
  background: var(--fill);
  color: var(--text-1);
}

/* ---------- 筛选 ---------- */

.filters {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.filter-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.filter-label {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding-top: 6px;
  font-size: 11.5px;
  font-weight: 700;
  color: var(--text-3);
  flex-shrink: 0;
  letter-spacing: 0.04em;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  flex: 1;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.chip-count {
  font-size: 10px;
  opacity: 0.7;
  font-variant-numeric: tabular-nums;
}

/* ---------- 结果 ---------- */

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 4px 2px 14px;
}

.result-count {
  font-size: 12.5px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

/* ---------- 分页 ---------- */

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 26px;
  padding: 10px 14px;
  border-radius: var(--r-pill);
}

.pager-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 8px 16px;
  border-radius: var(--r-pill);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  transition: all 0.22s var(--ease);
}

.pager-btn:hover:not(:disabled) {
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.8);
}

.pager-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pager-info {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-1);
  font-variant-numeric: tabular-nums;
}

/* ---------- 响应式 ---------- */

@media (max-width: 640px) {
  .head {
    padding: 24px 18px;
  }

  .filter-row {
    flex-direction: column;
    gap: 8px;
  }

  .filter-label {
    padding-top: 0;
  }
}
</style>
