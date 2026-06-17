import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/utils/api'
import router from '@/router'

export interface UserInfo {
  id: number
  username: string
  displayName: string
  role: 'root' | 'admin' | 'user'
  nickname?: string
  avatar?: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
  initialized: boolean
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<UserInfo | null>(null)
  const initialized = ref(false)

  const isAuthenticated = computed(() => !!token.value && !!user.value)
  const isRoot = computed(() => user.value?.role === 'root')
  const isAdmin = computed(() => user.value?.role === 'root' || user.value?.role === 'admin')
  const displayName = computed(() => user.value?.displayName || user.value?.nickname || user.value?.username || '')

  function saveSession() {
    if (token.value) {
      sessionStorage.setItem('astraion_token', token.value)
      sessionStorage.setItem('astraion_user', JSON.stringify(user.value))
      sessionStorage.setItem('astraion_initialized', String(initialized.value))
    }
  }

  function restoreSession() {
    const savedToken = sessionStorage.getItem('astraion_token')
    const savedUser = sessionStorage.getItem('astraion_user')
    const savedInit = sessionStorage.getItem('astraion_initialized')
    if (savedToken && savedUser) {
      token.value = savedToken
      try {
        user.value = JSON.parse(savedUser) as UserInfo
      } catch {
        clearSession()
        return
      }
      initialized.value = savedInit === 'true'
    }
  }

  function clearSession() {
    token.value = null
    user.value = null
    initialized.value = false
    sessionStorage.removeItem('astraion_token')
    sessionStorage.removeItem('astraion_user')
    sessionStorage.removeItem('astraion_initialized')
  }

  async function login(username: string, password: string): Promise<LoginResponse> {
    const res = await api.post<any>('/api/v1/auth/login', { username, password })
    const body = res.data
    if (body.code !== 0) {
      throw new Error(body.message || '登录失败')
    }
    const d = body.data
    if (!d || !d.token) {
      throw new Error('登录返回数据异常，请重试')
    }
    token.value = d.token
    user.value = d.user
    initialized.value = d.initialized
    saveSession()
    return d
  }

  async function checkInitStatus(): Promise<boolean> {
    try {
      const res = await api.get<any>('/api/v1/system/status')
      initialized.value = res.data.data.initialized
      return res.data.data.initialized
    } catch {
      return false
    }
  }

  function logout() {
    clearSession()
    router.push('/login')
  }

  return {
    token,
    user,
    initialized,
    isAuthenticated,
    isRoot,
    isAdmin,
    displayName,
    login,
    logout,
    checkInitStatus,
    saveSession,
    restoreSession,
    clearSession,
  }
})
