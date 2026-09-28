<script setup>
import { computed } from 'vue'
import { Eye, Flame, Heart } from 'lucide-vue-next'
import { categoryColor, formatCount, formatDate } from '../utils/format'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  /** 推荐理由（个性化推荐流里展示） */
  reason: {
    type: String,
    default: ''
  },
  /** 榜单名次 */
  rank: {
    type: Number,
    default: 0
  },
  /** 热度分（热门榜展示） */
  score: {
    type: Number,
    default: 0
  }
})

const emit = defineEmits(['open', 'like'])

const accent = computed(() => categoryColor(props.item.category))

const summary = computed(
  () => props.item.preview || props.item.content || ''
)

const liked = computed(() => Boolean(props.item.liked))
</script>

<template>
  <article
    class="card glass glass-interactive"
    @click="emit('open', item)"
  >
    <!-- 分类色条：给玻璃卡片一点稳定的识别色 -->
    <span
      class="card-accent"
      :style="{ background: accent }"
    ></span>

    <header class="card-head">
      <!--
        分类标识用"灰点 + 中性文字"：
        浅灰直接当文字色会对比度不足，圆点只承载色阶信息，文字保持可读。
      -->
      <span class="cat">
        <span
          class="cat-dot"
          :style="{ background: accent }"
        ></span>
        {{ item.category || '推荐' }}
      </span>

      <span
        v-if="reason"
        class="reason"
      >
        {{ reason }}
      </span>

      <span
        v-else-if="rank"
        class="rank"
      >
        <Flame :size="13" />
        No.{{ rank }}
      </span>
    </header>

    <h3 class="card-title clamp-2">
      {{ item.title }}
    </h3>

    <p class="card-summary clamp-3">
      {{ summary }}
    </p>

    <footer class="card-foot">
      <div class="meta">
        <span class="author">{{ item.authorName || '匿名作者' }}</span>

        <!-- 阅读历史里优先展示"什么时候读的"，比发布时间更有意义 -->
        <template v-if="item.viewedAt">
          <span class="dot">·</span>
          <span class="date date-viewed">
            阅读于 {{ formatDate(item.viewedAt) }}
          </span>
        </template>

        <template v-else-if="item.createdAt">
          <span class="dot">·</span>
          <span class="date">{{ formatDate(item.createdAt) }}</span>
        </template>
      </div>

      <div class="actions">
        <span
          v-if="score > 0"
          class="stat stat-hot"
        >
          <Flame :size="13" />
          {{ formatCount(score) }}
        </span>

        <span class="stat">
          <Eye :size="13" />
          {{ formatCount(item.viewCount) }}
        </span>

        <button
          class="like"
          :class="{ 'like-on': liked }"
          type="button"
          :aria-label="liked ? '取消点赞' : '点赞'"
          @click.stop="emit('like', item)"
        >
          <Heart
            :size="13"
            :fill="liked ? 'currentColor' : 'none'"
          />
          {{ formatCount(item.likeCount) }}
        </button>
      </div>
    </footer>
  </article>
</template>

<style scoped>
.card {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 20px 20px 16px;
  cursor: pointer;
  overflow: hidden;
}

.card-accent {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  opacity: 0.75;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.cat {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 11px 4px 9px;
  border-radius: var(--r-pill);
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.6);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-2);
  flex-shrink: 0;
}

.cat-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.reason {
  font-size: 11px;
  font-weight: 600;
  color: var(--brand-1);
  background: var(--brand-soft);
  padding: 4px 10px;
  border-radius: var(--r-pill);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 700;
  color: var(--text-2);
}

.card-title {
  font-size: 16.5px;
  line-height: 1.45;
  margin-bottom: 8px;
  transition: color 0.24s var(--ease);
}

.card:hover .card-title {
  color: var(--brand-1);
}

.card-summary {
  font-size: 12.5px;
  line-height: 1.72;
  color: var(--text-2);
  margin-bottom: 16px;
  flex: 1;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 13px;
  border-top: 1px solid var(--line);
}

.meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11.5px;
  color: var(--text-3);
  min-width: 0;
}

.author {
  max-width: 92px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-viewed {
  color: var(--brand-1);
  font-weight: 600;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.stat,
.like {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11.5px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
  transition: color 0.22s var(--ease);
}

.stat-hot {
  color: var(--text-2);
  font-weight: 600;
}

.like {
  padding: 3px 8px;
  border-radius: var(--r-pill);
}

.like:hover {
  color: var(--text-1);
  background: var(--fill);
}

/* 已点赞：实心近黑心形，比彩色更克制 */
.like-on {
  color: var(--text-1);
  font-weight: 700;
}
</style>
