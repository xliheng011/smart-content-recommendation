/**
 * 数字格式化：1200 -> 1.2k
 */
export function formatCount(value) {
  const num = Number(value) || 0

  if (num < 1000) {
    return String(num)
  }

  if (num < 10000) {
    return `${(num / 1000).toFixed(1).replace(/\.0$/, '')}k`
  }

  return `${(num / 10000).toFixed(1).replace(/\.0$/, '')}万`
}

/**
 * 后端返回 "yyyy-MM-dd HH:mm"，这里压缩成 "MM-DD"
 */
export function formatDate(value) {
  if (!value) {
    return ''
  }

  const parts = String(value).split(' ')
  const date = parts[0] || ''

  return date.slice(5) || date
}

/**
 * 分类配色。
 *
 * 黑白灰主题下不再用色相区分分类，改用一条灰阶梯度：
 * 每个分类对应一个固定明度，在卡片顶部的色条、兴趣画像的进度条、
 * 分类分布图上都能读出差别。
 *
 * 注意：这些灰阶是给"色块"用的（色条 / 圆点 / 进度条），
 * 分类文字本身统一用中性文字色 + 前置圆点，避免浅灰文字对比度不足。
 */
const CATEGORY_COLORS = {
  科技: '#101012',
  设计: '#2e2e34',
  商业: '#4a4a52',
  职场: '#66666e',
  生活: '#80808a',
  阅读: '#9b9ba3'
}

/** 未登记分类的备用灰阶，保持同样的明度区间 */
const FALLBACK_PALETTE = [
  '#101012',
  '#2e2e34',
  '#4a4a52',
  '#66666e',
  '#80808a',
  '#9b9ba3'
]

export function categoryColor(name) {
  if (!name) {
    return CATEGORY_COLORS.科技
  }

  if (CATEGORY_COLORS[name]) {
    return CATEGORY_COLORS[name]
  }

  let hash = 0
  for (let i = 0; i < name.length; i += 1) {
    hash = (hash * 31 + name.charCodeAt(i)) % 9973
  }

  return FALLBACK_PALETTE[hash % FALLBACK_PALETTE.length]
}

/**
 * 把 hex 颜色转成带透明度的 rgba，避免依赖 color-mix 的浏览器支持。
 */
export function tint(hex, alpha) {
  const value = String(hex || '').replace('#', '')

  if (value.length !== 6) {
    return `rgba(9, 9, 11, ${alpha})`
  }

  const r = parseInt(value.slice(0, 2), 16)
  const g = parseInt(value.slice(2, 4), 16)
  const b = parseInt(value.slice(4, 6), 16)

  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
