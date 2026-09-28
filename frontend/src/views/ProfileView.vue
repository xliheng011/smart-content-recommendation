<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  Activity,
  Eye,
  FileText,
  Heart,
  LogOut,
  PenLine,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Trash2,
  UserRound
} from 'lucide-vue-next'
import { contentApi, userApi } from '../api'
import ContentCard from '../components/ContentCard.vue'
import EmptyState from '../components/EmptyState.vue'
import SkeletonCards from '../components/SkeletonCards.vue'
import StatTile from '../components/StatTile.vue'
import { authState, isAdmin, logout } from '../store/auth'
import { toastError, toastSuccess } from '../store/ui'
import { categoryColor, formatCount, formatDate } from '../utils/format'

const router = useRouter()

const stats = ref(null)
const loading = ref(true)
const error = ref('')

/** 三个列表各自维护加载状态，切到哪个才拉哪个 */
const lists = reactive({
  history: { items: [], loading: false, loaded: false },
  liked: { items: [], loading: false, loaded: false },
  mine: { items: [], loading: false, loaded: false }
})

const activeTab = ref('history')

/** 删除前的二次确认（用行内两步确认代替原生弹窗） */
const pendingDelete = ref(null)

const TABS = [
  { key: 'history', label: '阅读历史', icon: Eye },
  { key: 'liked', label: '我的喜欢', icon: Heart },
  { key: 'mine', label: '我的文章', icon: FileText }
]

const avatarText = computed(() => {
  const source = authState.user?.nickname || authState.user?.username || '?'
  return source.trim().charAt(0).toUpperCase()
})

const maxCategoryScore = computed(() => {
  const list = stats.value?.favoriteCategories || []
  return list.length ? Math.max(...list.map((item) => item.count)) : 1
})

const current = computed(() => lists[activeTab.value])

async function loadStats() {
  loading.value = true
  error.value = ''

  try {
    stats.value = await userApi.myStats()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function loadTab(key, force = false) {
  const state = lists[key]

  if (state.loading || (state.loaded && !force)) {
    return
  }

  state.loading = true

  try {
    if (key === 'history') {
      state.items = await contentApi.history(30)
    } else if (key === 'liked') {
      state.items = await contentApi.liked()
    } else {
      state.items = await contentApi.mine()
    }

    state.loaded = true
  } catch (err) {
    state.items = []
    toastError(err.message)
  } finally {
    state.loading = false
  }
}

function reload() {
  loadStats()

  lists.history.loaded = false
  lists.liked.loaded = false
  lists.mine.loaded = false

  loadTab(activeTab.value, true)
}

function openDetail(item) {
  router.push({ name: 'detail', params: { id: item.id } })
}

async function toggleLike(item) {
  const prevLiked = item.liked
  const prevCount = item.likeCount

  item.liked = false
  item.likeCount = Math.max((prevCount || 0) - 1, 0)

  try {
    await contentApi.unlike(item.id)

    lists.liked.items = lists.liked.items.filter(
      (entry) => entry.id !== item.id
    )

    toastSuccess('已从喜欢中移除')
    loadStats()
  } catch (err) {
    item.liked = prevLiked
    item.likeCount = prevCount
    toastError(err.message)
  }
}

async function confirmDelete(item) {
  try {
    await contentApi.remove(item.id)

    lists.mine.items = lists.mine.items.filter(
      (entry) => entry.id !== item.id
    )

    // 文章被删后，它也会从阅读历史里消失，标记为待刷新
    lists.history.loaded = false

    pendingDelete.value = null

    toastSuccess('文章已删除')
    loadStats()
  } catch (err) {
    toastError(err.message)
  }
}

async function handleLogout() {
  await logout()
  toastSuccess('已退出登录')
  router.push({ name: 'home' })
}

watch(activeTab, (key) => loadTab(key), { immediate: true })

onMounted(() => {
  loadStats()

  // "我发布的文章"是要展示在统计磁贴上的，先拉一次，
  // 否则这个数字要等用户点开对应 Tab 才会出现。
  loadTab('mine')
})
</script>

<template>
  <main class="page">
    <!-- 用户信息 -->
    <section class="profile glass rise-in">
      <div class="profile-main">
        <span class="avatar">{{ avatarText }}</span>

        <div class="profile-text">
          <h1 class="profile-name">
            {{ authState.user?.nickname || authState.user?.username }}
          </h1>

          <div class="profile-tags">
            <span class="chip">
              @{{ authState.user?.username }}
            </span>

            <span
              class="chip"
              :class="{ 'chip-admin': isAdmin() }"
            >
              <ShieldCheck
                v-if="isAdmin()"
                :size="12"
              />
              {{ isAdmin() ? '管理员' : '普通用户' }}
            </span>

            <span
              v-if="authState.user?.email"
              class="chip"
            >
              {{ authState.user.email }}
            </span>
          </div>
        </div>
      </div>

      <div class="profile-actions">
        <button
          class="btn btn-primary"
          type="button"
          @click="router.push({ name: 'publish' })"
        >
          <PenLine :size="15" />
          写文章
        </button>

        <button
          class="btn btn-glass"
          type="button"
          :disabled="loading"
          @click="reload"
        >
          <RefreshCw
            :size="15"
            :class="{ spin: loading }"
          />
          刷新
        </button>

        <button
          class="btn btn-glass"
          type="button"
          @click="handleLogout"
        >
          <LogOut :size="15" />
          退出
        </button>
      </div>
    </section>

    <!-- 统计 -->
    <EmptyState
      v-if="error"
      title="数据加载失败"
      :description="error"
      action-text="重新加载"
      @action="reload"
    />

    <template v-else>
      <section class="tiles">
        <StatTile
          label="阅读过的内容"
          :value="loading ? '—' : formatCount(stats?.viewedCount)"
        >
          <template #icon>
            <Eye :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="点赞过的内容"
          :value="loading ? '—' : formatCount(stats?.likedCount)"
        >
          <template #icon>
            <Heart :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="我发布的文章"
          :value="lists.mine.loaded ? formatCount(lists.mine.items.length) : '—'"
          hint="在「我的文章」中管理"
        >
          <template #icon>
            <FileText :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="行为记录总数"
          :value="loading ? '—' : formatCount(stats?.behaviorCount)"
          hint="浏览与点赞累计"
        >
          <template #icon>
            <Activity :size="20" />
          </template>
        </StatTile>
      </section>

      <div class="columns">
        <!-- 兴趣画像 -->
        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Sparkles :size="15" />
              兴趣画像
            </span>
            <span class="panel-hint">点赞 3 分 · 浏览 1 分</span>
          </div>

          <div
            v-if="loading"
            class="stack stack-sm"
          >
            <div
              v-for="n in 3"
              :key="n"
              class="skeleton"
              style="height: 34px"
            ></div>
          </div>

          <EmptyState
            v-else-if="!stats?.favoriteCategories?.length"
            title="还没有足够的兴趣数据"
            description="去浏览或点赞一些内容，这里就会生成你的兴趣分布。"
            action-text="去发现"
            @action="router.push({ name: 'explore' })"
          />

          <div
            v-else
            class="interest-list"
          >
            <div
              v-for="cat in stats.favoriteCategories"
              :key="cat.name"
              class="interest"
            >
              <div class="interest-head">
                <span class="interest-name">
                  <span
                    class="dot"
                    :style="{ background: categoryColor(cat.name) }"
                  ></span>
                  {{ cat.name }}
                </span>
                <span class="interest-score">{{ cat.count }} 分</span>
              </div>

              <div class="bar">
                <span
                  class="bar-fill"
                  :style="{
                    width: `${Math.max((cat.count / maxCategoryScore) * 100, 6)}%`,
                    background: categoryColor(cat.name)
                  }"
                ></span>
              </div>
            </div>
          </div>
        </section>

        <!-- 最近动态 -->
        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Activity :size="15" />
              最近动态
            </span>
            <span class="panel-hint">最近互动过的 10 篇</span>
          </div>

          <SkeletonCards
            v-if="loading"
            variant="row"
            :count="4"
          />

          <EmptyState
            v-else-if="!stats?.recentBehaviors?.length"
            title="暂无行为记录"
            description="浏览内容后，这里会记录你的操作轨迹。"
          />

          <ul
            v-else
            class="behavior-list"
          >
            <li
              v-for="item in stats.recentBehaviors"
              :key="item.id"
              class="behavior"
              @click="openDetail({ id: item.contentId })"
            >
              <span
                class="behavior-icon"
                :class="
                  item.behaviorType === 'LIKE'
                    ? 'behavior-like'
                    : 'behavior-view'
                "
              >
                <Heart
                  v-if="item.behaviorType === 'LIKE'"
                  :size="12"
                />
                <Eye
                  v-else
                  :size="12"
                />
              </span>

              <div class="behavior-body">
                <div class="behavior-title clamp-2">
                  {{ item.contentTitle }}
                </div>
                <div class="behavior-meta">
                  <span>{{ item.category || '未分类' }}</span>
                  <span>·</span>
                  <span>{{ formatDate(item.createdAt) }}</span>
                </div>
              </div>
            </li>
          </ul>
        </section>
      </div>

      <!-- 我的内容：Tab 切换 -->
      <section class="tabs-section">
        <div class="section-head">
          <div>
            <span class="tag-eyebrow">
              <UserRound :size="12" />
              My Library
            </span>
            <h2 class="section-title">我的内容</h2>
          </div>
        </div>

        <div class="tabs glass">
          <button
            v-for="tab in TABS"
            :key="tab.key"
            class="tab"
            :class="{ 'tab-active': activeTab === tab.key }"
            type="button"
            @click="activeTab = tab.key"
          >
            <component
              :is="tab.icon"
              :size="15"
            />
            {{ tab.label }}
            <span
              v-if="lists[tab.key].loaded"
              class="tab-count"
            >
              {{ lists[tab.key].items.length }}
            </span>
          </button>
        </div>

        <!-- 阅读历史 -->
        <template v-if="activeTab === 'history'">
          <SkeletonCards
            v-if="current.loading"
            :count="3"
          />

          <EmptyState
            v-else-if="!current.items.length"
            title="还没有阅读记录"
            description="打开任意一篇内容，它就会出现在这里，方便你回头再看。"
            action-text="去发现"
            @action="router.push({ name: 'explore' })"
          >
            <template #icon>
              <Eye :size="26" />
            </template>
          </EmptyState>

          <div
            v-else
            class="grid-cards stagger"
          >
            <ContentCard
              v-for="item in current.items"
              :key="item.id"
              :item="item"
              @open="openDetail"
              @like="toggleLike"
            />
          </div>
        </template>

        <!-- 我的喜欢 -->
        <template v-else-if="activeTab === 'liked'">
          <SkeletonCards
            v-if="current.loading"
            :count="3"
          />

          <EmptyState
            v-else-if="!current.items.length"
            title="还没有喜欢的内容"
            description="在内容详情页点击喜欢，就会收藏到这里。"
            action-text="去发现"
            @action="router.push({ name: 'explore' })"
          >
            <template #icon>
              <Heart :size="26" />
            </template>
          </EmptyState>

          <div
            v-else
            class="grid-cards stagger"
          >
            <ContentCard
              v-for="item in current.items"
              :key="item.id"
              :item="item"
              @open="openDetail"
              @like="toggleLike"
            />
          </div>
        </template>

        <!-- 我的文章 -->
        <template v-else>
          <div
            v-if="current.loading"
            class="stack stack-sm"
          >
            <div
              v-for="n in 3"
              :key="n"
              class="skeleton"
              style="height: 72px"
            ></div>
          </div>

          <EmptyState
            v-else-if="!current.items.length"
            title="还没有发布过文章"
            description="写一篇吧，发布后会立刻出现在「发现」里。"
            action-text="去写文章"
            @action="router.push({ name: 'publish' })"
          >
            <template #icon>
              <PenLine :size="26" />
            </template>
          </EmptyState>

          <ul
            v-else
            class="mine-list"
          >
            <li
              v-for="item in current.items"
              :key="item.id"
              class="mine-row glass"
              @click="openDetail(item)"
            >
              <span
                class="mine-accent"
                :style="{ background: categoryColor(item.category) }"
              ></span>

              <div class="mine-body">
                <h3 class="mine-title clamp-1">
                  {{ item.title }}
                </h3>

                <div class="mine-meta">
                  <span class="mine-cat">
                    <span
                      class="mine-cat-dot"
                      :style="{ background: categoryColor(item.category) }"
                    ></span>
                    {{ item.category || '未分类' }}
                  </span>

                  <span>{{ formatDate(item.createdAt) }}</span>
                  <span>·</span>
                  <span>{{ formatCount(item.viewCount) }} 阅读</span>
                  <span>·</span>
                  <span>{{ formatCount(item.likeCount) }} 喜欢</span>
                </div>
              </div>

              <div
                class="mine-actions"
                @click.stop
              >
                <template v-if="pendingDelete === item.id">
                  <button
                    class="btn-mini btn-mini-danger"
                    type="button"
                    @click="confirmDelete(item)"
                  >
                    确认删除
                  </button>
                  <button
                    class="btn-mini"
                    type="button"
                    @click="pendingDelete = null"
                  >
                    取消
                  </button>
                </template>

                <button
                  v-else
                  class="btn-mini"
                  type="button"
                  @click="pendingDelete = item.id"
                >
                  <Trash2 :size="13" />
                  删除
                </button>
              </div>
            </li>
          </ul>
        </template>
      </section>
    </template>
  </main>
</template>

<style scoped>
/* ---------- 用户信息 ---------- */

.profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 30px;
  margin-bottom: 20px;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 18px;
  min-width: 0;
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 62px;
  height: 62px;
  border-radius: 20px;
  background: var(--brand-grad);
  color: #fff;
  font-size: 25px;
  font-weight: 750;
  box-shadow: var(--shadow-brand);
  flex-shrink: 0;
}

.profile-name {
  font-size: 24px;
  letter-spacing: -0.02em;
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 10px;
}

/* 管理员身份用"反白"标识：黑底白字，是单色主题下最明确的层级提示 */
.chip-admin {
  color: #fff;
  background: var(--brand-1);
  border-color: transparent;
}

.profile-actions {
  display: flex;
  gap: 9px;
  flex-shrink: 0;
}

/* ---------- 统计 ---------- */

.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

/* ---------- 双栏 ---------- */

.columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  /* 两块面板内容长度不同，不拉伸才能避免出现大片空白 */
  align-items: start;
  gap: 16px;
  margin-bottom: 30px;
}

.panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 16px;
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

/* ---------- 兴趣画像 ---------- */

.interest-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.interest-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
}

.interest-name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.interest-score {
  font-size: 11.5px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

.bar {
  height: 7px;
  border-radius: var(--r-pill);
  background: var(--line);
  overflow: hidden;
}

.bar-fill {
  display: block;
  height: 100%;
  border-radius: var(--r-pill);
  opacity: 0.85;
  transition: width 0.7s var(--ease);
}

/* ---------- 行为 ---------- */

.behavior-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.behavior {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 9px 10px;
  border-radius: var(--r-xs);
  cursor: pointer;
  transition: background 0.22s var(--ease), transform 0.22s var(--ease);
}

.behavior:hover {
  background: rgba(255, 255, 255, 0.7);
  transform: translateX(3px);
}

.behavior-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  flex-shrink: 0;
  color: #fff;
}

/* 浏览 = 浅灰，点赞 = 近黑，用明度差区分两种行为 */
.behavior-view {
  background: linear-gradient(135deg, #a1a1aa, #71717a);
}

.behavior-like {
  background: linear-gradient(135deg, #3f3f46, #101012);
}

.behavior-body {
  min-width: 0;
  flex: 1;
}

.behavior-title {
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.5;
}

.behavior:hover .behavior-title {
  color: var(--brand-1);
}

.behavior-meta {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 3px;
  font-size: 10.5px;
  color: var(--text-3);
}

/* ---------- Tab ---------- */

.section-head {
  margin-bottom: 16px;
}

.section-title {
  font-size: 20px;
  margin-top: 5px;
}

.tabs {
  display: flex;
  gap: 4px;
  padding: 6px;
  border-radius: var(--r-pill);
  margin-bottom: 20px;
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 9px 18px;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-2);
  white-space: nowrap;
  transition: all 0.26s var(--ease);
}

.tab:hover {
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.72);
}

.tab-active {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: var(--shadow-brand);
}

.tab-active:hover {
  color: #fff;
  background: var(--brand-grad);
}

.tab-count {
  font-size: 10.5px;
  font-weight: 700;
  padding: 1px 7px;
  border-radius: var(--r-pill);
  background: var(--line);
  font-variant-numeric: tabular-nums;
}

.tab-active .tab-count {
  background: rgba(255, 255, 255, 0.28);
}

/* ---------- 我的文章 ---------- */

.clamp-1 {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.mine-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mine-row {
  position: relative;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  overflow: hidden;
  cursor: pointer;
  border-radius: var(--r-md);
  transition: transform 0.28s var(--ease), box-shadow 0.28s var(--ease),
    background 0.28s var(--ease);
}

.mine-row:hover {
  transform: translateY(-2px);
  background: var(--glass-bg-strong);
  box-shadow: var(--shadow-lg), inset 0 1px 0 rgba(255, 255, 255, 0.95);
}

.mine-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  opacity: 0.8;
}

.mine-body {
  flex: 1;
  min-width: 0;
}

.mine-title {
  font-size: 15px;
  line-height: 1.45;
  margin-bottom: 7px;
  transition: color 0.24s var(--ease);
}

.mine-row:hover .mine-title {
  color: var(--brand-1);
}

.mine-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  font-size: 11.5px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

.mine-cat {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 9px 2px 7px;
  border-radius: var(--r-pill);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.65);
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-2);
}

.mine-cat-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  flex-shrink: 0;
}

.mine-actions {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
}

.btn-mini {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 13px;
  border-radius: var(--r-pill);
  font-size: 11.5px;
  font-weight: 600;
  color: var(--text-2);
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid var(--glass-border);
  transition: all 0.22s var(--ease);
}

.btn-mini:hover {
  color: var(--brand-1);
  border-color: var(--line-strong);
}

.btn-mini-danger {
  color: var(--danger);
  border-color: rgba(164, 38, 44, 0.3);
  background: rgba(164, 38, 44, 0.09);
}

.btn-mini-danger:hover {
  color: #fff;
  background: var(--danger);
  border-color: transparent;
}

/* ---------- 响应式 ---------- */

@media (max-width: 900px) {
  .columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .profile {
    flex-direction: column;
    align-items: flex-start;
    padding: 22px 20px;
  }

  .profile-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .profile-actions .btn {
    flex: 1;
  }

  .mine-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .mine-actions {
    width: 100%;
  }
}
</style>
