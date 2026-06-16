<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { WsMessage } from '@/utils/websocket'
import MessageItem from './MessageItem.vue'

const props = defineProps<{
  messages: WsMessage[]
  streamingContent: string
  isStreaming: boolean
  welcome: { title: string; subtitle: string; hints: string[] }
}>()

const listRef = ref<HTMLElement | null>(null)

// Auto-scroll to bottom
watch(
  () => [props.messages.length, props.streamingContent],
  async () => {
    await nextTick()
    if (listRef.value) {
      listRef.value.scrollTop = listRef.value.scrollHeight
    }
  },
  { deep: true },
)
</script>

<template>
  <div ref="listRef" class="message-list">
    <!-- Welcome screen when no messages -->
    <div v-if="messages.length === 0 && !isStreaming" class="welcome">
      <div class="welcome-icon">✦</div>
      <h1 class="welcome-title">{{ welcome.title }}</h1>
      <p class="welcome-subtitle">{{ welcome.subtitle }}</p>
      <div class="welcome-hints">
        <div v-for="(hint, i) in welcome.hints" :key="i" class="hint-chip">
          {{ hint }}
        </div>
      </div>
    </div>

    <!-- Message list -->
    <div v-for="(msg, i) in messages" :key="i" class="message-wrapper">
      <MessageItem :message="msg" />
    </div>

    <!-- Streaming message -->
    <div v-if="isStreaming && streamingContent" class="message-wrapper">
      <MessageItem
        :message="{ type: 'ai', content: streamingContent, metadata: { streaming: true } }"
      />
    </div>

    <!-- Bottom spacer for input area -->
    <div class="bottom-spacer"></div>
  </div>
</template>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 0;
  scroll-behavior: smooth;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  padding: 40px 20px;
  text-align: center;
  animation: welcomeIn 0.6s ease-out;
}

@keyframes welcomeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.welcome-icon {
  font-size: 3rem;
  margin-bottom: 20px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.welcome-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.welcome-subtitle {
  font-size: 0.95rem;
  color: var(--text-secondary);
  margin-bottom: 24px;
  max-width: 400px;
}

.welcome-hints {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
}

.hint-chip {
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  cursor: default;
  transition: all var(--transition-fast);
}

.hint-chip:hover {
  background: var(--bg-hover);
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.message-wrapper {
  padding: 0 16px;
  max-width: 800px;
  margin: 0 auto;
}

.bottom-spacer {
  height: 100px;
}
</style>
