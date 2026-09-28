<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import {
  Activity,
  ChartColumn,
  Eye,
  FileText,
  Heart,
  LoaderCircle,
  RefreshCw,
  Send,
  ShieldCheck,
  UsersRound
} from 'lucide-vue-next'
import { adminApi } from '../api'
import EmptyState from '../components/EmptyState.vue'
import StatTile from '../components/StatTile.vue'
import { toastError, toastSuccess } from '../store/ui'
import { categoryColor, formatCount } from '../utils/format'

const overview = ref(null)
const users = ref([])

const loading = ref(true)
const loadingUsers = ref(true)
const error = ref('')
const publishing = ref(false)

const form = reactive({
  title: '',
  category: '',
  content: ''
})

const CATEGORY_SUGGESTIONS = ['科技', '设计', '商业', '职场', '生活', '阅读']

const maxCategoryCount = computed(() => {
  const list = overview.value?.categories || []
  return list.length ? Math.max(...list.map((item) => item.count)) : 1
})

const canSubmit = computed(
  () =>
    form.title.trim() &&
    form.category.trim() &&
    form.content.trim() &&
    !publishing.value
)

async function loadOverview() {
  loading.value = true
  error.value = ''

  try {
    overview.value = await adminApi.overview()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function loadUsers() {
  loadingUsers.value = true

  try {
    users.value = await adminApi.users()
  } catch {
    users.value = []
  } finally {
    loadingUsers.value = false
  }
}

function reload() {
  loadOverview()
  loadUsers()
}

async function publish() {
  if (!canSubmit.value) {
    toastError('请填写标题、分类与正文')
    return
  }

  publishing.value = true

  try {
    await adminApi.createContent({
      title: form.title.trim(),
      category: form.category.trim(),
      content: form.content.trim()
    })

    toastSuccess('内容发布成功')

    form.title = ''
    form.category = ''
    form.content = ''

    loadOverview()
  } catch (err) {
    toastError(err.message)
  } finally {
    publishing.value = false
  }
}

function pickCategory(name) {
  form.category = name
}

onMounted(reload)
</script>

<template>
  <main class="page">
    <!-- 头部 -->
    <section class="head glass rise-in">
      <div class="head-main">
        <span class="head-mark">
          <ShieldCheck :size="20" />
        </span>

        <div>
          <span class="tag-eyebrow">Admin Console</span>
          <h1 class="head-title">管理后台</h1>
          <p class="head-desc">
            查看平台数据概览、发布新内容，并管理注册用户。
          </p>
        </div>
      </div>

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
        刷新数据
      </button>
    </section>

    <EmptyState
      v-if="error"
      title="数据加载失败"
      :description="error"
      action-text="重新加载"
      @action="reload"
    />

    <template v-else>
      <!-- 概览 -->
      <section class="tiles">
        <StatTile
          label="内容总数"
          :value="loading ? '—' : formatCount(overview?.contentCount)"
        >
          <template #icon>
            <FileText :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="注册用户"
          :value="loading ? '—' : formatCount(overview?.userCount)"
        >
          <template #icon>
            <UsersRound :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="行为记录"
          :value="loading ? '—' : formatCount(overview?.behaviorCount)"
          hint="浏览 + 点赞"
        >
          <template #icon>
            <Activity :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="累计阅读"
          :value="loading ? '—' : formatCount(overview?.totalViews)"
        >
          <template #icon>
            <Eye :size="20" />
          </template>
        </StatTile>

        <StatTile
          label="累计点赞"
          :value="loading ? '—' : formatCount(overview?.totalLikes)"
        >
          <template #icon>
            <Heart :size="20" />
          </template>
        </StatTile>
      </section>

      <div class="columns">
        <!-- 发布内容 -->
        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Send :size="15" />
              发布新内容
            </span>
          </div>

          <form
            class="form"
            @submit.prevent="publish"
          >
            <div class="field">
              <label
                class="field-label"
                for="title"
              >
                标题
              </label>
              <input
                id="title"
                v-model="form.title"
                class="input"
                type="text"
                maxlength="200"
                placeholder="一句话说清这篇内容讲什么"
              />
            </div>

            <div class="field">
              <label
                class="field-label"
                for="category"
              >
                分类
              </label>
              <input
                id="category"
                v-model="form.category"
                class="input"
                type="text"
                maxlength="50"
                placeholder="例如：科技"
              />

              <div class="suggestions">
                <button
                  v-for="name in CATEGORY_SUGGESTIONS"
                  :key="name"
                  class="chip chip-interactive"
                  :class="{ 'chip-active': form.category === name }"
                  type="button"
                  @click="pickCategory(name)"
                >
                  {{ name }}
                </button>
              </div>
            </div>

            <div class="field">
              <label
                class="field-label"
                for="content"
              >
                正文
              </label>
              <textarea
                id="content"
                v-model="form.content"
                class="input"
                placeholder="支持多段，换行会被保留"
              ></textarea>
            </div>

            <button
              class="btn btn-primary btn-block"
              type="submit"
              :disabled="!canSubmit"
            >
              <LoaderCircle
                v-if="publishing"
                :size="16"
                class="spin"
              />
              <Send
                v-else
                :size="15"
              />
              {{ publishing ? '发布中' : '发布内容' }}
            </button>
          </form>
        </section>

        <!-- 分类分布 -->
        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <ChartColumn :size="15" />
              分类分布
            </span>
            <span class="panel-hint">
              共 {{ overview?.categoryCount || 0 }} 个分类
            </span>
          </div>

          <div
            v-if="loading"
            class="stack stack-sm"
          >
            <div
              v-for="n in 4"
              :key="n"
              class="skeleton"
              style="height: 38px"
            ></div>
          </div>

          <EmptyState
            v-else-if="!overview?.categories?.length"
            title="暂无分类数据"
            description="发布内容后这里会展示分类分布。"
          />

          <div
            v-else
            class="dist-list"
          >
            <div
              v-for="cat in overview.categories"
              :key="cat.name"
              class="dist"
            >
              <div class="dist-head">
                <span class="dist-name">
                  <span
                    class="dot"
                    :style="{ background: categoryColor(cat.name) }"
                  ></span>
                  {{ cat.name }}
                </span>
                <span class="dist-count">
                  {{ cat.count }}
                </span>
              </div>

              <div class="bar">
                <span
                  class="bar-fill"
                  :style="{
                    width: `${Math.max((cat.count / maxCategoryCount) * 100, 6)}%`,
                    background: categoryColor(cat.name)
                  }"
                ></span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- 用户列表 -->
      <section class="panel glass">
        <div class="panel-head">
          <span class="panel-title">
            <UsersRound :size="15" />
            用户列表
          </span>
          <span class="panel-hint">{{ users.length }} 位用户</span>
        </div>

        <div
          v-if="loadingUsers"
          class="stack stack-sm"
        >
          <div
            v-for="n in 4"
            :key="n"
            class="skeleton"
            style="height: 52px"
          ></div>
        </div>

        <EmptyState
          v-else-if="!users.length"
          title="暂无用户"
          description="注册用户会出现在这里。"
        />

        <div
          v-else
          class="table"
        >
          <div class="table-head">
            <span>用户</span>
            <span>邮箱</span>
            <span>角色</span>
            <span>ID</span>
          </div>

          <div
            v-for="user in users"
            :key="user.id"
            class="table-row"
          >
            <span class="cell-user">
              <span class="mini-avatar">
                {{ (user.nickname || user.username || '?').charAt(0).toUpperCase() }}
              </span>
              <span class="cell-strong">{{ user.nickname || user.username }}</span>
            </span>

            <span class="cell-dim">{{ user.email || '—' }}</span>

            <span>
              <span
                class="role-badge"
                :class="{ 'role-admin': user.role === 'ADMIN' }"
              >
                {{ user.role === 'ADMIN' ? '管理员' : '用户' }}
              </span>
            </span>

            <span class="cell-dim">#{{ user.id }}</span>
          </div>
        </div>
      </section>
    </template>
  </main>
</template>

<style scoped>
/* ---------- 头部 ---------- */

.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 26px 30px;
  margin-bottom: 20px;
}

.head-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.head-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border-radius: 16px;
  background: var(--brand-grad);
  color: #fff;
  box-shadow: var(--shadow-brand);
  flex-shrink: 0;
}

.head-title {
  font-size: 22px;
  margin-top: 4px;
}

.head-desc {
  margin-top: 5px;
  font-size: 12.5px;
  color: var(--text-2);
}

/* ---------- 概览 ---------- */

.tiles {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

/* ---------- 双栏 ---------- */

.columns {
  display: grid;
  grid-template-columns: 1.15fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.panel {
  padding: 22px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 18px;
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

/* ---------- 表单 ---------- */

.form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 3px;
}

/* ---------- 分类分布 ---------- */

.dist-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.dist-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 7px;
}

.dist-name {
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

.dist-count {
  font-size: 11px;
  font-weight: 700;
  padding: 2px 9px;
  border-radius: var(--r-pill);
  color: var(--text-2);
  background: var(--fill-strong);
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

/* ---------- 用户表 ---------- */

.table {
  display: flex;
  flex-direction: column;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 1.4fr 1.5fr 0.8fr 0.5fr;
  gap: 14px;
  align-items: center;
}

.table-head {
  padding: 0 12px 10px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-3);
  letter-spacing: 0.04em;
  border-bottom: 1px solid var(--line);
}

.table-row {
  padding: 12px;
  border-radius: var(--r-xs);
  font-size: 13px;
  transition: background 0.22s var(--ease);
}

.table-row:hover {
  background: rgba(255, 255, 255, 0.7);
}

.cell-user {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.mini-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--brand-grad);
  color: #fff;
  font-size: 11.5px;
  font-weight: 700;
  flex-shrink: 0;
}

.cell-strong {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-dim {
  color: var(--text-3);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.role-badge {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: var(--r-pill);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-2);
  background: var(--line);
}

.role-admin {
  color: #fff;
  background: var(--brand-1);
}

/* ---------- 响应式 ---------- */

@media (max-width: 900px) {
  .columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .head {
    flex-direction: column;
    align-items: flex-start;
  }

  .table-head {
    display: none;
  }

  .table-row {
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    padding: 14px 12px;
    border-bottom: 1px solid var(--line);
  }
}
</style>
