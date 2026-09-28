<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Eye,
  LoaderCircle,
  PenLine,
  Send,
  Sparkles,
  Tag,
  Type
} from 'lucide-vue-next'
import { contentApi } from '../api'
import { toastError, toastSuccess } from '../store/ui'
import { categoryColor } from '../utils/format'

const router = useRouter()

const form = reactive({
  title: '',
  category: '',
  content: ''
})

const categories = ref([])
const publishing = ref(false)

/** 后端摘要长度为 86 个字符，这里保持一致的预览口径 */
const PREVIEW_LENGTH = 86

const FALLBACK_CATEGORIES = ['科技', '设计', '商业', '职场', '生活', '阅读']

const categoryOptions = computed(() =>
  categories.value.length ? categories.value.map((item) => item.name) : FALLBACK_CATEGORIES
)

const titleLength = computed(() => form.title.trim().length)
const contentLength = computed(() => form.content.trim().length)

const preview = computed(() => {
  const flat = form.content.replace(/\s+/g, ' ').trim()

  if (flat.length <= PREVIEW_LENGTH) {
    return flat
  }

  return `${flat.slice(0, PREVIEW_LENGTH)}…`
})

const canSubmit = computed(
  () =>
    titleLength.value > 0 &&
    form.category.trim().length > 0 &&
    contentLength.value > 0 &&
    !publishing.value
)

async function loadCategories() {
  try {
    categories.value = await contentApi.categories()
  } catch {
    categories.value = []
  }
}

function pickCategory(name) {
  form.category = name
}

async function submit() {
  if (!canSubmit.value) {
    toastError('请填写标题、分类与正文')
    return
  }

  publishing.value = true

  try {
    const created = await contentApi.create({
      title: form.title.trim(),
      category: form.category.trim(),
      content: form.content.trim()
    })

    toastSuccess('发布成功，正在跳转到你的文章')

    router.push({ name: 'detail', params: { id: created.id } })
  } catch (err) {
    toastError(err.message)
  } finally {
    publishing.value = false
  }
}

onMounted(loadCategories)
</script>

<template>
  <main class="page">
    <!-- 头部 -->
    <section class="head glass rise-in">
      <div class="head-main">
        <span class="head-mark">
          <PenLine :size="20" />
        </span>

        <div>
          <span class="tag-eyebrow">Write</span>
          <h1 class="head-title">写一篇文章</h1>
          <p class="head-desc">
            发布后会立刻出现在「发现」里，并参与推荐与热度计算。
          </p>
        </div>
      </div>
    </section>

    <div class="columns">
      <!-- 编辑器 -->
      <section class="panel glass">
        <div class="panel-head">
          <span class="panel-title">
            <Type :size="15" />
            正文
          </span>
          <span class="panel-hint">{{ contentLength }} 字</span>
        </div>

        <form
          class="form"
          @submit.prevent="submit"
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

            <div class="counter">{{ titleLength }} / 200</div>
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
              class="input editor"
              placeholder="支持多段，换行会被保留。"
            ></textarea>
          </div>

          <div class="submit-row">
            <button
              class="btn btn-primary"
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
              {{ publishing ? '发布中' : '发布文章' }}
            </button>

            <span class="submit-hint">
              {{ canSubmit ? '确认无误后发布' : '标题、分类、正文都填好后即可发布' }}
            </span>
          </div>
        </form>
      </section>

      <!-- 侧栏 -->
      <aside class="side">
        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Tag :size="15" />
              选择分类
            </span>
          </div>

          <div class="suggestions">
            <button
              v-for="name in categoryOptions"
              :key="name"
              class="chip chip-interactive"
              :class="{ 'chip-active': form.category === name }"
              type="button"
              @click="pickCategory(name)"
            >
              {{ name }}
            </button>
          </div>

          <div class="field">
            <label
              class="field-label"
              for="category"
            >
              或自定义分类
            </label>
            <input
              id="category"
              v-model="form.category"
              class="input"
              type="text"
              maxlength="50"
              placeholder="例如：人工智能"
            />
          </div>
        </section>

        <section class="panel glass">
          <div class="panel-head">
            <span class="panel-title">
              <Eye :size="15" />
              列表摘要预览
            </span>
          </div>

          <div class="preview-card">
            <span class="preview-cat">
              <span
                class="preview-dot"
                :style="{ background: categoryColor(form.category) }"
              ></span>
              {{ form.category || '未分类' }}
            </span>

            <h3 class="preview-title">
              {{ form.title || '标题会显示在这里' }}
            </h3>

            <p class="preview-text">
              {{ preview || '正文的前 86 个字符会作为列表摘要。' }}
            </p>
          </div>
        </section>

        <section class="tips glass-soft glass">
          <span class="panel-title">
            <Sparkles :size="15" />
            小提示
          </span>

          <ul class="tip-list">
            <li>分类会影响推荐算法，尽量选准确的那个。</li>
            <li>发布后你可以在「我的 → 我的文章」里删除它。</li>
            <li>标题控制在 30 字以内，卡片上更耐看。</li>
          </ul>
        </section>
      </aside>
    </div>
  </main>
</template>

<style scoped>
/* ---------- 头部 ---------- */

.head {
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

/* ---------- 布局 ---------- */

.columns {
  display: grid;
  grid-template-columns: 1.5fr 1fr;
  gap: 16px;
  align-items: start;
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  font-variant-numeric: tabular-nums;
}

/* ---------- 表单 ---------- */

.form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.counter {
  font-size: 10.5px;
  color: var(--text-3);
  text-align: right;
  padding-right: 2px;
  font-variant-numeric: tabular-nums;
}

.editor {
  min-height: 320px;
}

.submit-row {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.submit-hint {
  font-size: 11.5px;
  color: var(--text-3);
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-bottom: 18px;
}

/* ---------- 摘要预览 ---------- */

.preview-card {
  padding: 16px;
  border-radius: var(--r-sm);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
}

.preview-cat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px 3px 8px;
  border-radius: var(--r-pill);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.7);
  font-size: 11px;
  font-weight: 700;
  color: var(--text-2);
  margin-bottom: 10px;
}

.preview-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.preview-title {
  font-size: 15px;
  line-height: 1.45;
  margin-bottom: 7px;
}

.preview-text {
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-2);
}

/* ---------- 提示 ---------- */

.tips {
  padding: 20px;
}

.tip-list {
  margin: 14px 0 0;
  padding-left: 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-2);
}

/* ---------- 响应式 ---------- */

@media (max-width: 940px) {
  .columns {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .head {
    padding: 22px 20px;
  }

  .submit-row {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
