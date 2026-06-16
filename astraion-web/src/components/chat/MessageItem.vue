<script setup lang="ts">
import { computed } from 'vue'
import type { WsMessage } from '@/utils/websocket'
import DynamicRenderer from '@/components/renderer/DynamicRenderer.vue'

const props = defineProps<{
  message: WsMessage
}>()

const isUser = computed(() => props.message.type === 'user')
const isSystem = computed(() => props.message.type === 'system')
const isError = computed(() => props.message.type === 'error')
const isAi = computed(() => props.message.type === 'ai')
const isStreaming = computed(() => !!props.message.metadata?.streaming)

const hasRenderData = computed(() => !!props.message.renderData)
</script>

<template>
  <div
    class="message-item"
    :class="{
      'message-user': isUser,
      'message-ai': isAi,
      'message-system': isSystem,
      'message-error': isError,
      'message-streaming': isStreaming,
    }"
  >
    <!-- Avatar -->
    <div class="message-avatar">
      <template v-if="isUser">
        <div class="avatar-user">{{ message.content.charAt(0).toUpperCase() || 'U' }}</div>
      </template>
      <template v-else-if="isSystem">
        <div class="avatar-system">⚙</div>
      </template>
      <template v-else-if="isError">
        <div class="avatar-error">!</div>
      </template>
      <template v-else>
        <div class="avatar-ai">✦</div>
      </template>
    </div>

    <!-- Content -->
    <div class="message-content">
      <div class="message-header">
        <span class="message-author">
          {{ isUser ? '我' : isSystem ? '系统' : isError ? '错误' : 'ASTRAION' }}
        </span>
        <span v-if="isStreaming" class="streaming-badge">思考中...</span>
      </div>

      <!-- Render data mode -->
      <template v-if="hasRenderData && isAi">
        <DynamicRenderer :render-type="message.renderType || 'table'" :data="message.renderData!" />
      </template>

      <!-- Text content -->
      <template v-else>
        <div class="message-text" v-html="message.content"></div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.message-item {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  margin: 4px 0;
  border-radius: var(--radius-lg);
  transition: background var(--transition-fast);
}

.message-item.message-user {
  background: transparent;
}

.message-item.message-ai {
  background: var(--bg-secondary);
}

.message-item.message-system {
  background: rgba(59, 130, 246, 0.05);
  border-left: 3px solid var(--info);
}

.message-item.message-error {
  background: rgba(239, 68, 68, 0.05);
  border-left: 3px solid var(--error);
}

.message-item.message-streaming {
  border-left: 3px solid var(--accent-primary);
}

.message-avatar {
  flex-shrink: 0;
}

.avatar-user,
.avatar-ai,
.avatar-system,
.avatar-error {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 0.85rem;
  font-weight: 700;
}

.avatar-user {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
}

.avatar-ai {
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-blue));
  color: #fff;
  font-size: 0.9rem;
}

.avatar-system {
  background: var(--bg-tertiary);
  color: var(--info);
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.avatar-error {
  background: rgba(239, 68, 68, 0.15);
  color: var(--error);
  font-weight: 800;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.message-author {
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
}

.streaming-badge {
  font-size: 0.75rem;
  color: var(--accent-primary);
  animation: blinkPulse 1s infinite;
}

@keyframes blinkPulse {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

.message-text {
  font-size: 0.95rem;
  line-height: 1.7;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
