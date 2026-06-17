<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

interface CardAction {
  label: string
  tool: string
  variant?: string
}

interface CardItem {
  id: number | string
  title: string
  fields: Record<string, unknown>
  actions?: CardAction[]
}

const config = computed(() => props.data.config as Record<string, unknown> || {})
const items = computed(() => (props.data.items as CardItem[]) || [])
</script>

<template>
  <div class="card-list">
    <div v-if="items.length === 0" class="card-empty">无数据</div>
    <div v-for="item in items" :key="item.id" class="card-item">
      <div class="card-header">{{ item.title }}</div>
      <div class="card-body">
        <div v-for="(val, key) in item.fields" :key="key" class="card-row">
          <span class="card-label">{{ key }}</span>
          <span class="card-value">{{ val }}</span>
        </div>
      </div>
      <div v-if="item.actions && item.actions.length" class="card-actions">
        <button
          v-for="act in item.actions"
          :key="act.label"
          class="card-btn"
          :class="act.variant === 'danger' ? 'btn-danger' : act.variant === 'primary' ? 'btn-primary-card' : ''"
          @click="$emit('action', { tool: act.tool, id: item.id })"
        >
          {{ act.label }}
        </button>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default { emits: ['action'] }
</script>

<style scoped>
.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.card-empty {
  padding: 20px;
  text-align: center;
  color: var(--text-muted);
}
.card-item {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.card-header {
  padding: 10px 14px;
  font-weight: 600;
  font-size: 0.95rem;
  background: var(--bg-tertiary);
  border-bottom: 1px solid var(--border-color);
  color: var(--accent-primary);
}
.card-body {
  padding: 10px 14px;
}
.card-row {
  display: flex;
  padding: 3px 0;
  font-size: 0.85rem;
}
.card-label {
  width: 80px;
  flex-shrink: 0;
  color: var(--text-muted);
}
.card-value {
  color: var(--text-primary);
}
.card-actions {
  display: flex;
  gap: 8px;
  padding: 8px 14px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
}
.card-btn {
  padding: 4px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  color: var(--text-primary);
  font-size: 0.8rem;
  cursor: pointer;
}
.card-btn:hover {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
}
.btn-primary-card {
  background: var(--accent-primary);
  color: #000;
  border-color: var(--accent-primary);
}
.btn-danger {
  color: var(--error);
  border-color: var(--error);
}
.btn-danger:hover {
  background: rgba(239,68,68,0.1);
}
</style>
