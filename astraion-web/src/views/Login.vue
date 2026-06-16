<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/api'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const loading = ref(false)
const errorMsg = ref('')
const systemReady = ref(true) // becomes false if DB not initialized (root missing)

onMounted(async () => {
  // Detect if the backend reports system not initialized
  try {
    const res = await api.get<any>('/api/v1/system/status')
    systemReady.value = res.data.data.initialized
  } catch {
    // Backend unavailable — still show login
  }
})

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    errorMsg.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const data = await auth.login(username.value, password.value)
    if (!data.initialized && data.user.role === 'root') {
      // Root user needs to initialize the system
      router.push('/chat')
    } else {
      const redirect = (route.query.redirect as string) || '/chat'
      router.push(redirect)
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '登录失败，请重试'
    errorMsg.value = msg
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- Decorative nebula glow -->
    <div class="login-glow"></div>

    <div class="login-card">
      <!-- Branding -->
      <div class="login-brand">
        <div class="login-logo">
          <svg viewBox="0 0 200 160" class="logo-icon">
            <defs>
              <linearGradient id="starGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stop-color="#D4A017"/>
                <stop offset="100%" stop-color="#FFF8E7"/>
              </linearGradient>
              <filter id="starGlow">
                <feGaussianBlur stdDeviation="2" result="blur"/>
                <feMerge><feMergeNode in="blur"/><feMergeNode in="SourceGraphic"/></feMerge>
              </filter>
            </defs>
            <!-- 北斗七星连线 -->
            <line x1="160" y1="20" x2="130" y2="55" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <line x1="130" y1="55" x2="100" y2="50" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <line x1="100" y1="50" x2="70" y2="70" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <line x1="70" y1="70" x2="45" y2="60" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <line x1="45" y1="60" x2="30" y2="80" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <line x1="30" y1="80" x2="50" y2="110" stroke="url(#starGrad)" stroke-width="1" opacity="0.6"/>
            <!-- 北斗七星 -->
            <circle cx="160" cy="20" r="5" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="130" cy="55" r="4.5" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="100" cy="50" r="4" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="70" cy="70" r="5" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="45" cy="60" r="3.5" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="30" cy="80" r="4.5" fill="#FFF8E7" filter="url(#starGlow)"/>
            <circle cx="50" cy="110" r="4" fill="#FFF8E7" filter="url(#starGlow)"/>
            <!-- 北极星 -->
            <line x1="160" y1="20" x2="175" y2="5" stroke="url(#starGrad)" stroke-width="0.5" stroke-dasharray="3,3" opacity="0.4"/>
            <circle cx="175" cy="5" r="3" fill="#FFD700" filter="url(#starGlow)" opacity="0.9"/>
            <text x="178" y="9" fill="#D4A017" font-size="8" opacity="0.7">北极星</text>
          </svg>
        </div>
        <h1 class="login-title">ASTRAION</h1>
        <p class="login-subtitle">星辰造物 · AI 管理平台</p>
      </div>

      <!-- System not initialized hint -->
      <div v-if="!systemReady" class="init-hint">
        <span class="init-hint-icon">✦</span>
        <span>系统尚未初始化</span>
      </div>

      <!-- Login form -->
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-group">
          <label for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            type="text"
            class="input-base"
            placeholder="请输入用户名"
            autocomplete="username"
            :disabled="loading"
          />
        </div>

        <div class="form-group">
          <label for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="input-base"
            placeholder="请输入密码"
            autocomplete="current-password"
            :disabled="loading"
          />
        </div>

        <div v-if="errorMsg" class="error-message">
          <svg viewBox="0 0 20 20" fill="currentColor" width="16" height="16">
            <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd"/>
          </svg>
          {{ errorMsg }}
        </div>

        <button type="submit" class="btn-primary login-btn" :disabled="loading">
          <span v-if="loading" class="spinner"></span>
          <span v-else>登 录</span>
        </button>
      </form>

      <p class="login-footer">
        首次使用？请联系管理员创建账号
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 20px;
  overflow: hidden;
}

.login-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 600px;
  height: 600px;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(212, 160, 23, 0.06) 0%, transparent 70%);
  pointer-events: none;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 48px 36px 36px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  animation: cardIn 0.6s ease-out;
}

@keyframes cardIn {
  from {
    opacity: 0;
    transform: translateY(20px) scale(0.97);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.login-brand {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.logo-icon {
  width: 120px;
  height: 96px;
}

.login-title {
  font-size: 1.75rem;
  font-weight: 700;
  background: linear-gradient(135deg, #D4A017, #E8B830);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 2px;
}

.login-subtitle {
  margin-top: 6px;
  font-size: 0.85rem;
  color: var(--text-muted);
  letter-spacing: 3px;
}

.init-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  margin-bottom: 20px;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: var(--radius-md);
  color: var(--warning);
  font-size: 0.85rem;
}

.init-hint-icon {
  font-size: 1rem;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-group label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: var(--radius-md);
  color: var(--error);
  font-size: 0.85rem;
}

.login-btn {
  width: 100%;
  padding: 12px;
  font-size: 1rem;
  letter-spacing: 4px;
}

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  font-size: 0.8rem;
  color: var(--text-muted);
}
</style>
