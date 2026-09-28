import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import router from './router'
import { restoreSession } from './store/auth'

const app = createApp(App)

app.use(router)

// 挂载前先确认本地令牌是否仍然有效，避免出现"假登录"状态
restoreSession().finally(() => {
  app.mount('#app')
})
