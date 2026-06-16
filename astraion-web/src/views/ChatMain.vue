<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { wsManager, type WsMessage } from '@/utils/websocket'
import { api } from '@/utils/api'
import MessageList from '@/components/chat/MessageList.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import InitWizard from '@/components/chat/InitWizard.vue'
import DynamicRenderer from '@/components/renderer/DynamicRenderer.vue'
import SettingsDialog from '@/components/SettingsDialog.vue'

const auth = useAuthStore()

// Demo state
const demoLoaded = ref(false)
const demoLoading = ref(false)

// Models
const models = ref<Array<{modelName: string, displayName: string, fieldCount: number}>>([])

// Messages
const messages = ref<WsMessage[]>([])
const streamingContent = ref('')
const isStreaming = ref(false)

// UI state
const showInitWizard = ref(false)
const showSettings = ref(false)
const sidebarOpen = ref(false)

const welcomeMessages = computed(() => {
  const role = auth.user?.role || 'user'
  const name = auth.displayName

  if (role === 'root') {
    return {
      title: `欢迎回来，${name}`,
      subtitle: '您是系统的根管理员，可以通过对话管理整个平台。',
      hints: ['管理用户与权限', '配置系统参数', '查看系统日志', '监控运行状态'],
    }
  } else if (role === 'admin') {
    return {
      title: `你好，${name}`,
      subtitle: '您拥有管理员权限，可以管理业务数据和用户。',
      hints: ['管理业务数据', '审批流程', '查看报表', '用户管理'],
    }
  } else {
    return {
      title: `你好，${name}`,
      subtitle: '很高兴见到你！请随时向我提问或发出指令。',
      hints: ['查询数据', '提交工单', '查看通知', '获取帮助'],
    }
  }
})

// Check if init wizard should show for root
onMounted(async () => {
  if (auth.isRoot && !auth.initialized) {
    try {
      const res = await api.get<{ initialized: boolean }>('/api/v1/system/status')
      if (!res.data.initialized) {
        showInitWizard.value = true
      } else {
        auth.initialized = true
      }
    } catch {
      showInitWizard.value = true
    }
  }

  // Load models for sidebar
  try {
    const modelRes = await api.get<Array<any>>('/api/v1/models')
    if (modelRes.data) {
      models.value = modelRes.data
        .filter((m: any) => !m.builtin)
        .map((m: any) => ({
          modelName: m.modelName,
          displayName: m.displayName || m.modelName,
          fieldCount: m.fields?.length || 0
        }))
    }
  } catch { /* ignore */ }

  // Check demo status
  if (auth.isAdmin) {
    try {
      const demoRes = await api.get<any>('/api/v1/demo/status')
      demoLoaded.value = demoRes.data?.data?.loaded || false
    } catch { /* ignore */ }
  }

  // Connect WebSocket
  if (auth.token) {
    wsManager.connect('/ws', auth.token)
  }

  // Listen for messages
  wsManager.onMessage((msg) => {
    if (msg.metadata?.streaming) {
      streamingContent.value += msg.content
      isStreaming.value = true
    } else if (msg.metadata?.complete) {
      messages.value.push({
        type: 'ai',
        content: streamingContent.value + (msg.content || ''),
        renderData: msg.renderData,
        renderType: msg.renderType,
      })
      streamingContent.value = ''
      isStreaming.value = false
    } else {
      messages.value.push(msg)
    }
  })
})

onUnmounted(() => {
  wsManager.disconnect()
})

function handleSend(text: string) {
  if (!text.trim()) return

  if (isStreaming.value) return

  messages.value.push({
    type: 'user',
    content: text,
  })

  wsManager.send({
    type: 'message',
    content: text,
  })
}

function handleInitComplete() {
  showInitWizard.value = false
  auth.initialized = true
}

async function handleLogout() {
  await auth.logout()
}

async function loadDemo() {
  if (!confirm('将创建 OA 演示数据：部门、员工、请假、报销、公告、会议室。\n可随时在侧栏清除。')) return
  demoLoading.value = true
  try {
    const res = await api.post<any>('/api/v1/demo/load')
    demoLoaded.value = true
    messages.value.push({ type: 'ai', content: res.data?.message || '演示数据加载完成' })
  } catch (err: any) {
    alert(err?.response?.data?.message || '加载失败')
  } finally { demoLoading.value = false }
}

async function clearDemo() {
  if (!confirm('确定清除所有演示数据吗？此操作不可恢复。')) return
  demoLoading.value = true
  try {
    const res = await api.post<any>('/api/v1/demo/clear')
    demoLoaded.value = false
    messages.value.push({ type: 'ai', content: res.data?.message || '演示数据已清除' })
  } catch (err: any) {
    alert(err?.response?.data?.message || '清除失败')
  } finally { demoLoading.value = false }
}

function newConversation() {
  messages.value = []
  streamingContent.value = ''
}
</script>

<template>
  <div class="chat-layout">
    <!-- Sidebar -->
    <aside class="chat-sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-header">
        <div class="sidebar-logo">✦</div>
        <span class="sidebar-brand">ASTRAION</span>
      </div>

      <div class="sidebar-user">
        <div class="user-avatar">{{ auth.displayName.charAt(0).toUpperCase() }}</div>
        <div class="user-info">
          <span class="user-name">{{ auth.displayName }}</span>
          <span class="user-role">{{ auth.user?.role }}</span>
        </div>
      </div>

      <div class="sidebar-actions">
        <button class="sidebar-btn" @click="newConversation">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新对话
        </button>

        <button v-if="auth.isAdmin" class="sidebar-btn" @click="showSettings = true">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 012.83-2.83l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 014 0v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 010 4h-.09a1.65 1.65 0 00-1.51 1z"/>
          </svg>
          设置
        </button>
      </div>

      <!-- Models Panel -->
      <div v-if="models.length > 0" class="sidebar-section">
        <div class="section-title">业务模型</div>
        <div class="model-list">
          <button
            v-for="m in models"
            :key="m.modelName"
            class="model-item"
            @click="handleSend(`显示${m.displayName}列表`)"
          >
            <span class="model-icon">📋</span>
            <span class="model-name">{{ m.displayName }}</span>
            <span class="model-count">{{ m.fieldCount }}字段</span>
          </button>
        </div>
      </div>

      <!-- Demo Data Section (admin only) -->
      <div v-if="auth.isAdmin" class="sidebar-section">
        <div class="section-title">演示数据</div>
        <button
          v-if="!demoLoaded"
          class="sidebar-btn demo-btn"
          :disabled="demoLoading"
          @click="loadDemo"
        >
          <span v-if="demoLoading" class="spinner-sm"></span>
          <span v-else>📦</span>
          加载 OA 演示数据
        </button>
        <button
          v-else
          class="sidebar-btn demo-btn danger"
          :disabled="demoLoading"
          @click="clearDemo"
        >
          🗑 清除演示数据
        </button>
      </div>

      <div class="sidebar-footer">
        <div class="ws-indicator" :class="wsManager.status.value">
          <span class="ws-dot"></span>
          {{ wsManager.status.value === 'connected' ? '已连接' : wsManager.status.value === 'connecting' ? '连接中' : '未连接' }}
        </div>
        <button class="sidebar-btn logout-btn" @click="handleLogout">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
          </svg>
          退出
        </button>
      </div>
    </aside>

    <!-- Mobile sidebar toggle -->
    <button class="sidebar-toggle" @click="sidebarOpen = !sidebarOpen">
      <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="3" y1="6" x2="21" y2="6"/><line x1="3" y1="12" x2="21" y2="12"/><line x1="3" y1="18" x2="21" y2="18"/>
      </svg>
    </button>

    <!-- Overlay for mobile sidebar -->
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <!-- Main chat area -->
    <main class="chat-main">
      <!-- Init Wizard overlay -->
      <Transition name="slide-up">
        <div v-if="showInitWizard" class="init-overlay">
          <InitWizard @complete="handleInitComplete" />
        </div>
      </Transition>

      <!-- Settings dialog -->
      <SettingsDialog v-if="showSettings" @close="showSettings = false" />

      <!-- Messages -->
      <MessageList
        :messages="messages"
        :streaming-content="streamingContent"
        :is-streaming="isStreaming"
        :welcome="welcomeMessages"
      />

      <!-- Input -->
      <ChatInput
        :disabled="isStreaming || showInitWizard"
        @send="handleSend"
      />
    </main>
  </div>
</template>

<style scoped>
.chat-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* Sidebar */
.chat-sidebar {
  width: 260px;
  min-width: 260px;
  display: flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  padding: 20px 16px;
  transition: transform var(--transition-normal);
  z-index: 20;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
}

.sidebar-logo {
  font-size: 1.4rem;
  color: var(--accent-primary);
}

.sidebar-brand {
  font-size: 1.1rem;
  font-weight: 700;
  background: linear-gradient(135deg, #D4A017, #E8B830);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: 1px;
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-blue));
  color: #fff;
  font-weight: 700;
  font-size: 0.9rem;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: var(--text-primary);
}

.user-role {
  font-size: 0.75rem;
  color: var(--text-muted);
  text-transform: uppercase;
}

.sidebar-actions {
  flex: 1;
}

.sidebar-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 0.9rem;
  transition: all var(--transition-fast);
}

.sidebar-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

/* Models Panel */
.sidebar-section {
  padding: 12px 0;
  border-top: 1px solid var(--border-color);
  margin-top: 8px;
}

.section-title {
  font-size: 0.7rem;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: var(--text-muted);
  padding: 0 12px 8px;
}

.model-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  max-height: 200px;
  overflow-y: auto;
}

.model-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: transparent;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 0.82rem;
  text-align: left;
  transition: all var(--transition-fast);
}

.model-item:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.model-icon {
  font-size: 0.85rem;
}

.model-name {
  flex: 1;
}

.model-count {
  font-size: 0.68rem;
  color: var(--text-muted);
}

.demo-btn {
  font-size: 0.8rem;
}

.demo-btn.danger {
  color: var(--error) !important;
  opacity: 0.8;
}
.demo-btn.danger:hover {
  opacity: 1;
  background: rgba(239, 68, 68, 0.15) !important;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.ws-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 0.78rem;
  color: var(--text-muted);
}

.ws-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-muted);
}

.ws-indicator.connected .ws-dot {
  background: var(--success);
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.5);
}

.ws-indicator.connecting .ws-dot {
  background: var(--warning);
  animation: pulse 0.8s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}

.logout-btn {
  color: var(--error) !important;
  opacity: 0.7;
}
.logout-btn:hover {
  opacity: 1;
  background: rgba(239, 68, 68, 0.1) !important;
}

/* Main area — 深邃星空背景 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  background: #060612;
  overflow: hidden;
}

/* 星空粒子动画 */
.chat-main::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image:
    radial-gradient(1px 1px at 5% 8%, rgba(255,255,255,0.8), transparent),
    radial-gradient(1px 1px at 15% 22%, rgba(255,255,255,0.6), transparent),
    radial-gradient(1.2px 1.2px at 25% 15%, rgba(200,220,255,0.9), transparent),
    radial-gradient(0.8px 0.8px at 35% 45%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 48% 12%, rgba(255,255,255,0.7), transparent),
    radial-gradient(1.3px 1.3px at 55% 35%, rgba(180,200,255,0.8), transparent),
    radial-gradient(0.9px 0.9px at 65% 18%, rgba(255,255,255,0.6), transparent),
    radial-gradient(1px 1px at 75% 55%, rgba(220,230,255,0.7), transparent),
    radial-gradient(0.7px 0.7px at 82% 28%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1.1px 1.1px at 90% 42%, rgba(200,220,255,0.8), transparent),
    radial-gradient(0.6px 0.6px at 8% 65%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1px 1px at 18% 78%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1.4px 1.4px at 30% 62%, rgba(180,210,255,0.7), transparent),
    radial-gradient(0.8px 0.8px at 42% 72%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1.2px 1.2px at 58% 68%, rgba(200,220,255,0.6), transparent),
    radial-gradient(0.7px 0.7px at 68% 82%, rgba(255,255,255,0.5), transparent),
    radial-gradient(1px 1px at 78% 58%, rgba(220,240,255,0.6), transparent),
    radial-gradient(0.9px 0.9px at 88% 75%, rgba(255,255,255,0.4), transparent),
    radial-gradient(1.1px 1.1px at 12% 38%, rgba(180,200,255,0.6), transparent),
    radial-gradient(0.5px 0.5px at 95% 10%, rgba(255,255,255,0.7), transparent);
  background-size: 300px 300px;
  pointer-events: none;
  z-index: 0;
  animation: starDrift 60s linear infinite;
}

/* 深空星云 */
.chat-main::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(ellipse at 20% 50%, rgba(25, 25, 80, 0.4) 0%, transparent 60%),
    radial-gradient(ellipse at 80% 20%, rgba(15, 15, 60, 0.3) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 80%, rgba(20, 20, 50, 0.35) 0%, transparent 55%);
  pointer-events: none;
  z-index: 0;
}

@keyframes starDrift {
  from { transform: translateY(0); }
  to { transform: translateY(-300px); }
}

/* Init overlay */
.init-overlay {
  position: absolute;
  inset: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(10, 10, 26, 0.85);
  backdrop-filter: blur(4px);
}

/* Mobile */
.sidebar-toggle {
  display: none;
  position: fixed;
  top: 12px;
  left: 12px;
  z-index: 25;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-primary);
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 15;
}

@media (max-width: 768px) {
  .sidebar-toggle {
    display: flex;
  }
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    transform: translateX(-100%);
    z-index: 20;
    box-shadow: var(--shadow-lg);
  }
  .chat-sidebar.open {
    transform: translateX(0);
  }
  .sidebar-overlay {
    display: block;
  }
  .chat-main {
    padding-top: 56px;
  }
}
</style>
