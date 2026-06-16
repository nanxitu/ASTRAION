<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

const config = computed(() => ({
  title: (props.data.title as string) || '确认操作',
  message: (props.data.message as string) || '确定要执行此操作吗？',
  confirmLabel: (props.data.confirmLabel as string) || '确认',
  cancelLabel: (props.data.cancelLabel as string) || '取消',
  confirmAction: (props.data.confirmAction as string) || 'confirm',
  cancelAction: (props.data.cancelAction as string) || 'cancel',
  variant: (props.data.variant as string) || 'default', // default, danger, warning
}))

function handleConfirm() {
  const event = new CustomEvent('confirm-action', {
    detail: { action: config.value.confirmAction, confirmed: true },
    bubbles: true,
  })
  document.dispatchEvent(event)
}

function handleCancel() {
  const event = new CustomEvent('confirm-action', {
    detail: { action: config.value.cancelAction, confirmed: false },
    bubbles: true,
  })
  document.dispatchEvent(event)
}

const variantClass = computed(() => `card-${config.value.variant}`)
</script>

<template>
  <div class="confirm-card" :class="variantClass">
    <div class="confirm-icon">
      <template v-if="config.variant === 'danger'">⚠</template>
      <template v-else>✦</template>
    </div>
    <h4 class="confirm-title">{{ config.title }}</h4>
    <p class="confirm-message">{{ config.message }}</p>
    <div class="confirm-actions">
      <button class="btn-secondary" @click="handleCancel">{{ config.cancelLabel }}</button>
      <button
        class="btn-primary"
        :class="{ 'btn-danger': config.variant === 'danger', 'btn-warning': config.variant === 'warning' }"
        @click="handleConfirm"
      >
        {{ config.confirmLabel }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.confirm-card {
  max-width: 400px;
  margin: 12px 0;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  text-align: center;
  box-shadow: var(--shadow-md);
}

.confirm-icon {
  font-size: 2rem;
  margin-bottom: 12px;
}

.card-danger .confirm-icon {
  color: var(--error);
}

.confirm-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.confirm-message {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 20px;
  line-height: 1.5;
}

.confirm-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
}

.card-danger .btn-primary {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  box-shadow: 0 2px 10px rgba(239, 68, 68, 0.3);
}
.card-danger .btn-primary:hover {
  box-shadow: 0 4px 16px rgba(239, 68, 68, 0.45);
}

.card-warning .btn-primary {
  background: linear-gradient(135deg, #f59e0b, #d97706);
  box-shadow: 0 2px 10px rgba(245, 158, 11, 0.3);
}
.card-warning .btn-primary:hover {
  box-shadow: 0 4px 16px rgba(245, 158, 11, 0.45);
}
</style>
