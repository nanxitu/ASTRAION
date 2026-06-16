<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  send: [text: string]
}>()

const props = defineProps<{
  disabled?: boolean
}>()

const input = ref('')
const inputRef = ref<HTMLTextAreaElement | null>(null)

function handleSend() {
  const text = input.value.trim()
  if (!text || props.disabled) return
  emit('send', text)
  input.value = ''
  autoResize()
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function autoResize() {
  const el = inputRef.value
  if (el) {
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 200) + 'px'
  }
}
</script>

<template>
  <div class="chat-input-area">
    <div class="chat-input-wrapper">
      <textarea
        ref="inputRef"
        v-model="input"
        class="chat-input"
        :disabled="disabled"
        placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
        rows="1"
        @keydown="handleKeydown"
        @input="autoResize"
      ></textarea>
      <button
        class="send-btn"
        :disabled="disabled || !input.trim()"
        @click="handleSend"
        title="发送"
      >
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.chat-input-area {
  padding: 16px 24px 24px;
  background: linear-gradient(to top, var(--bg-primary), transparent);
}

.chat-input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  max-width: 800px;
  margin: 0 auto;
  padding: 8px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.chat-input-wrapper:focus-within {
  border-color: var(--border-focus);
  box-shadow: var(--shadow-md), 0 0 0 3px var(--accent-glow);
}

.chat-input {
  flex: 1;
  padding: 8px 4px;
  background: transparent;
  border: none;
  color: var(--text-primary);
  font-size: 0.95rem;
  line-height: 1.5;
  resize: none;
  max-height: 200px;
  outline: none;
  font-family: var(--font-sans);
}

.chat-input::placeholder {
  color: var(--text-muted);
}

.chat-input:disabled {
  opacity: 0.4;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  min-width: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-blue));
  color: #fff;
  transition: all var(--transition-fast);
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 0 12px rgba(108, 99, 255, 0.4);
}

.send-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}
</style>
