<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AtSign,
  KeyRound,
  LoaderCircle,
  ShieldCheck,
  Sparkles,
  UserRound
} from 'lucide-vue-next'
import { login, register } from '../store/auth'
import { toastError, toastSuccess } from '../store/ui'
const route = useRoute()
const router = useRouter()

const mode = ref('login')
const role = ref('USER')
const submitting = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  email: ''
})

const DEMO_ACCOUNTS = [
  { label: '普通用户', username: 'demo', password: 'demo123', role: 'USER' },
  { label: '管理员', username: 'admin', password: 'admin123', role: 'ADMIN' }
]

const isRegister = computed(() => mode.value === 'register')

const submitText = computed(() => {
  if (submitting.value) return '处理中'
  return isRegister.value ? '注册并登录' : '登录'
})

function switchMode(next) {
  if (mode.value === next) return

  mode.value = next
  form.value.password = ''
}

function fillDemo(account) {
  mode.value = 'login'
  role.value = account.role
  form.value.username = account.username
  form.value.password = account.password
}

async function submit() {
  if (submitting.value) return

  const username = form.value.username.trim()
  const password = form.value.password

  if (!username || !password) {
    toastError('请填写用户名和密码')
    return
  }

  if (isRegister.value && password.length < 6) {
    toastError('密码至少需要 6 位')
    return
  }

  submitting.value = true

  try {
    if (isRegister.value) {
      await register({
        username,
        password,
        nickname: form.value.nickname.trim() || undefined,
        email: form.value.email.trim() || undefined
      })
      toastSuccess('注册成功，已自动登录')
    } else {
      await login({ username, password, role: role.value })
      toastSuccess('登录成功')
    }

    const redirect = route.query.redirect
    router.replace(typeof redirect === 'string' && redirect ? redirect : '/')
  } catch (error) {
    toastError(error.message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="login-page">
    <div class="login-card glass rise-in">
      <!-- 品牌 -->
      <div class="brand">
        <span class="brand-mark">
          <Sparkles :size="20" />
        </span>
        <h1 class="brand-title">Smart Recommend</h1>
        <p class="brand-desc">智能内容推荐系统</p>
      </div>

      <!-- 模式切换 -->
      <div class="tabs">
        <button
          class="tab"
          :class="{ 'tab-active': !isRegister }"
          type="button"
          @click="switchMode('login')"
        >
          登录
        </button>

        <button
          class="tab"
          :class="{ 'tab-active': isRegister }"
          type="button"
          @click="switchMode('register')"
        >
          注册
        </button>
      </div>

      <form
        class="form"
        @submit.prevent="submit"
      >
        <!-- 身份选择（仅登录） -->
        <div
          v-if="!isRegister"
          class="field"
        >
          <span class="field-label">登录身份</span>

          <div class="role-group">
            <button
              class="role"
              :class="{ 'role-active': role === 'USER' }"
              type="button"
              @click="role = 'USER'"
            >
              <UserRound :size="15" />
              普通用户
            </button>

            <button
              class="role"
              :class="{ 'role-active': role === 'ADMIN' }"
              type="button"
              @click="role = 'ADMIN'"
            >
              <ShieldCheck :size="15" />
              管理员
            </button>
          </div>
        </div>

        <div class="field">
          <label
            class="field-label"
            for="username"
          >
            用户名
          </label>

          <div class="input-wrap">
            <UserRound
              :size="15"
              class="input-icon"
            />
            <input
              id="username"
              v-model="form.username"
              class="input input-iconed"
              type="text"
              autocomplete="username"
              placeholder="请输入用户名"
            />
          </div>
        </div>

        <div class="field">
          <label
            class="field-label"
            for="password"
          >
            密码
          </label>

          <div class="input-wrap">
            <KeyRound
              :size="15"
              class="input-icon"
            />
            <input
              id="password"
              v-model="form.password"
              class="input input-iconed"
              type="password"
              :autocomplete="isRegister ? 'new-password' : 'current-password'"
              :placeholder="isRegister ? '至少 6 位字符' : '请输入密码'"
            />
          </div>
        </div>

        <template v-if="isRegister">
          <div class="field">
            <label
              class="field-label"
              for="nickname"
            >
              昵称 <span class="optional">选填</span>
            </label>

            <input
              id="nickname"
              v-model="form.nickname"
              class="input"
              type="text"
              placeholder="不填则使用用户名"
            />
          </div>

          <div class="field">
            <label
              class="field-label"
              for="email"
            >
              邮箱 <span class="optional">选填</span>
            </label>

            <div class="input-wrap">
              <AtSign
                :size="15"
                class="input-icon"
              />
              <input
                id="email"
                v-model="form.email"
                class="input input-iconed"
                type="email"
                autocomplete="email"
                placeholder="name@example.com"
              />
            </div>
          </div>
        </template>

        <button
          class="btn btn-primary btn-block submit"
          type="submit"
          :disabled="submitting"
        >
          <LoaderCircle
            v-if="submitting"
            :size="16"
            class="spin"
          />
          {{ submitText }}
        </button>
      </form>

      <!-- 演示账号 -->
      <div class="demo">
        <div class="demo-head">
          <span>演示账号（点击填充）</span>
        </div>

        <div class="demo-list">
          <button
            v-for="account in DEMO_ACCOUNTS"
            :key="account.username"
            class="demo-item"
            type="button"
            @click="fillDemo(account)"
          >
            <span class="demo-label">{{ account.label }}</span>
            <span class="demo-value">
              {{ account.username }} / {{ account.password }}
            </span>
          </button>
        </div>
      </div>
    </div>
  </main>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 120px);
  padding: 40px 20px 60px;
}

.login-card {
  width: 100%;
  max-width: 424px;
  padding: 34px 32px 28px;
}

/* ---------- 品牌 ---------- */

.brand {
  text-align: center;
  margin-bottom: 24px;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  border-radius: 16px;
  background: var(--brand-grad);
  color: #fff;
  box-shadow: var(--shadow-brand);
  margin-bottom: 14px;
}

.brand-title {
  font-size: 21px;
  letter-spacing: -0.02em;
}

.brand-desc {
  margin-top: 5px;
  font-size: 12.5px;
  color: var(--text-3);
}

/* ---------- 切换 ---------- */

.tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  border-radius: var(--r-pill);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--glass-border-soft);
  margin-bottom: 22px;
}

.tab {
  flex: 1;
  padding: 9px;
  border-radius: var(--r-pill);
  font-size: 13px;
  font-weight: 650;
  color: var(--text-2);
  transition: all 0.26s var(--ease);
}

.tab:hover {
  color: var(--brand-1);
}

.tab-active {
  color: #fff;
  background: var(--brand-grad);
  box-shadow: var(--shadow-brand);
}

.tab-active:hover {
  color: #fff;
}

/* ---------- 表单 ---------- */

.form {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.optional {
  font-weight: 500;
  color: var(--text-3);
  font-size: 11px;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 15px;
  color: var(--text-3);
  pointer-events: none;
}

.input-iconed {
  padding-left: 41px;
}

.role-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.role {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 10px;
  border-radius: var(--r-sm);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-2);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--glass-border-soft);
  transition: all 0.24s var(--ease);
}

.role:hover {
  color: var(--brand-1);
  border-color: var(--line-strong);
}

.role-active {
  color: var(--brand-1);
  background: rgba(255, 255, 255, 0.9);
  border-color: rgba(9, 9, 11, 0.42);
  box-shadow: 0 0 0 3px var(--ring);
}

.submit {
  margin-top: 5px;
  padding: 13px;
  font-size: 14px;
}

/* ---------- 演示账号 ---------- */

.demo {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--line);
}

.demo-head {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-3);
  text-transform: uppercase;
  margin-bottom: 10px;
  text-align: center;
}

.demo-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.demo-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 10px 12px;
  border-radius: var(--r-sm);
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid var(--glass-border-soft);
  text-align: left;
  transition: all 0.24s var(--ease);
}

.demo-item:hover {
  background: rgba(255, 255, 255, 0.9);
  border-color: var(--line-strong);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.demo-label {
  font-size: 11.5px;
  font-weight: 700;
  color: var(--brand-1);
}

.demo-value {
  font-size: 11px;
  color: var(--text-3);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 420px) {
  .login-card {
    padding: 26px 20px 22px;
  }

  .demo-list {
    grid-template-columns: 1fr;
  }
}
</style>
