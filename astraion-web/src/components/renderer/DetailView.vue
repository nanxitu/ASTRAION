<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

interface DetailItem {
  label: string
  key: string
  type?: string
}

interface DetailConfig {
  title?: string
  fields: DetailItem[]
  values: Record<string, unknown>
}

const config = computed<DetailConfig>(() => ({
  title: props.data.title as string | undefined,
  fields: (props.data.fields as DetailItem[]) || [],
  values: (props.data.values as Record<string, unknown>) || {},
}))

const actions = computed(() => props.data.actions as { label: string; action: string; variant?: string }[] | undefined)

function formatValue(value: unknown, type?: string): string {
  if (value === null || value === undefined) return '-'
  if (type === 'datetime') {
    try {
      return new Intl.DateTimeFormat('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(String(value)))
    } catch {
      return String(value)
    }
  }
  if (type === 'boolean') {
    return value ? '是' : '否'
  }
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function handleAction(action: string) {
  const event = new CustomEvent('detail-action', {
    detail: { action },
    bubbles: true,
  })
  document.dispatchEvent(event)
}
</script>

<template>
  <div class="detail-view">
    <h3 v-if="config.title" class="detail-title">{{ config.title }}</h3>
    <div class="detail-grid">
      <div
        v-for="field in config.fields"
        :key="field.key"
        class="detail-row"
        :class="{ 'detail-row-full': field.type === 'textarea' || field.type === 'longtext' }"
      >
        <div class="detail-label">{{ field.label }}</div>
        <div class="detail-value">
          <template v-if="field.type === 'image' && config.values[field.key]">
            <img :src="String(config.values[field.key])" class="detail-image" alt="" />
          </template>
          <template v-else-if="field.type === 'boolean'">
            <span :class="config.values[field.key] ? 'val-true' : 'val-false'">
              {{ formatValue(config.values[field.key], field.type) }}
            </span>
          </template>
          <template v-else>
            {{ formatValue(config.values[field.key], field.type) }}
          </template>
        </div>
      </div>
    </div>

    <div v-if="actions && actions.length" class="detail-actions">
      <button
        v-for="act in actions"
        :key="act.action"
        class="action-btn"
        :class="act.variant === 'primary' ? 'action-primary' : act.variant === 'danger' ? 'action-danger' : 'action-default'"
        @click="handleAction(act.action)"
      >
        {{ act.label }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.detail-view {
  margin: 8px 0;
  padding: 20px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.detail-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

.detail-row {
  display: flex;
  flex-direction: column;
  padding: 10px 12px;
  border-bottom: 1px solid var(--border-color);
}

.detail-row:nth-last-child(-n+2) {
  border-bottom: none;
}

.detail-row-full {
  grid-column: 1 / -1;
}

.detail-label {
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-value {
  font-size: 0.9rem;
  color: var(--text-primary);
  word-break: break-word;
}

.val-true {
  color: var(--success);
  font-weight: 600;
}

.val-false {
  color: var(--text-muted);
}

.detail-image {
  max-width: 200px;
  max-height: 120px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.action-btn {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  font-size: 0.82rem;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-default {
  background: var(--bg-input);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
}
.action-default:hover {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}

.action-primary {
  background: var(--accent-primary);
  border: 1px solid transparent;
  color: #fff;
}
.action-primary:hover {
  background: var(--accent-secondary);
}

.action-danger {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--error);
}
.action-danger:hover {
  background: rgba(239, 68, 68, 0.25);
}
</style>
