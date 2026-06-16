<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

interface TableColumn {
  key: string
  label: string
  type?: string
  width?: string
}

interface TableAction {
  label: string
  action: string
  variant?: 'primary' | 'danger' | 'default'
}

interface TableConfig {
  columns: TableColumn[]
  rows: Record<string, unknown>[]
  actions?: TableAction[]
  title?: string
  pageSize?: number
}

const config = computed<TableConfig>(() => {
  return {
    columns: (props.data.columns as TableColumn[]) || [],
    rows: (props.data.rows as Record<string, unknown>[]) || [],
    actions: props.data.actions as TableAction[] | undefined,
    title: props.data.title as string | undefined,
    pageSize: (props.data.pageSize as number) || 10,
  }
})

function formatValue(value: unknown, _type?: string): string {
  if (value === null || value === undefined) return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function handleAction(action: string, row: Record<string, unknown>) {
  // Dispatch action event — parent handles the logic
  const event = new CustomEvent('table-action', {
    detail: { action, row },
    bubbles: true,
  })
  document.dispatchEvent(event)
}

function getActionClass(variant?: string): string {
  switch (variant) {
    case 'primary':
      return 'action-primary'
    case 'danger':
      return 'action-danger'
    default:
      return 'action-default'
  }
}
</script>

<template>
  <div class="table-view">
    <div v-if="config.title" class="table-title">{{ config.title }}</div>
    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th v-for="col in config.columns" :key="col.key" :style="{ width: col.width || 'auto' }">
              {{ col.label }}
            </th>
            <th v-if="config.actions?.length" class="actions-th">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, ri) in config.rows" :key="ri">
            <td v-for="col in config.columns" :key="col.key">
              <span class="cell-value">{{ formatValue(row[col.key], col.type) }}</span>
            </td>
            <td v-if="config.actions?.length" class="actions-cell">
              <button
                v-for="act in config.actions"
                :key="act.action"
                class="action-btn"
                :class="getActionClass(act.variant)"
                @click="handleAction(act.action, row)"
              >
                {{ act.label }}
              </button>
            </td>
          </tr>
          <tr v-if="config.rows.length === 0">
            <td :colspan="config.columns.length + (config.actions?.length ? 1 : 0)" class="empty-cell">
              暂无数据
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.table-view {
  margin: 8px 0;
}

.table-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text-primary);
}

.table-container {
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.88rem;
}

.data-table th {
  padding: 10px 14px;
  text-align: left;
  font-weight: 600;
  font-size: 0.8rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  white-space: nowrap;
}

.data-table td {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}

.data-table tr:last-child td {
  border-bottom: none;
}

.data-table tr:hover td {
  background: rgba(108, 99, 255, 0.03);
}

.cell-value {
  display: inline-block;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.actions-cell {
  display: flex;
  gap: 6px;
}

.actions-th {
  width: 1%;
  white-space: nowrap;
}

.action-btn {
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.78rem;
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
  color: #fff;
  border: 1px solid transparent;
}
.action-primary:hover {
  background: var(--accent-secondary);
  box-shadow: 0 0 8px rgba(108, 99, 255, 0.4);
}

.action-danger {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--error);
}
.action-danger:hover {
  background: rgba(239, 68, 68, 0.25);
}

.empty-cell {
  text-align: center;
  padding: 32px;
  color: var(--text-muted);
  font-size: 0.85rem;
}
</style>
