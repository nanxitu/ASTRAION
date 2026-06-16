<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

const config = computed(() => ({
  title: (props.data.title as string) || '审批请求',
  requestId: (props.data.requestId as string) || '',
  applicant: (props.data.applicant as string) || '',
  department: (props.data.department as string) || '',
  type: (props.data.type as string) || '',
  reason: (props.data.reason as string) || '',
  details: (props.data.details as Record<string, unknown>) || {},
  submitterNote: (props.data.submitterNote as string) || '',
  approveAction: (props.data.approveAction as string) || 'approve',
  rejectAction: (props.data.rejectAction as string) || 'reject',
}))

const detailEntries = computed(() => Object.entries(config.value.details))

function handleApprove() {
  const event = new CustomEvent('approval-action', {
    detail: {
      action: config.value.approveAction,
      requestId: config.value.requestId,
      decision: 'approved',
    },
    bubbles: true,
  })
  document.dispatchEvent(event)
}

function handleReject() {
  const event = new CustomEvent('approval-action', {
    detail: {
      action: config.value.rejectAction,
      requestId: config.value.requestId,
      decision: 'rejected',
    },
    bubbles: true,
  })
  document.dispatchEvent(event)
}
</script>

<template>
  <div class="approval-card">
    <div class="approval-header">
      <div class="approval-badge">审批</div>
      <div class="approval-meta">
        <h4 class="approval-title">{{ config.title }}</h4>
        <span class="approval-id">#{{ config.requestId }}</span>
      </div>
    </div>

    <div class="approval-body">
      <div class="approval-info">
        <div class="info-row">
          <span class="info-label">申请人</span>
          <span class="info-value">{{ config.applicant }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">部门</span>
          <span class="info-value">{{ config.department }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">类型</span>
          <span class="info-value info-type">{{ config.type }}</span>
        </div>
      </div>

      <div v-if="config.reason" class="approval-reason">
        <div class="reason-label">申请理由</div>
        <p class="reason-text">{{ config.reason }}</p>
      </div>

      <div v-if="config.submitterNote" class="approval-note">
        <div class="note-label">备注</div>
        <p class="note-text">{{ config.submitterNote }}</p>
      </div>

      <div v-if="detailEntries.length" class="approval-details">
        <div v-for="[key, val] in detailEntries" :key="key" class="detail-row">
          <span class="detail-label">{{ key }}</span>
          <span class="detail-value">{{ val }}</span>
        </div>
      </div>
    </div>

    <div class="approval-actions">
      <button class="approval-btn btn-approve" @click="handleApprove">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="20 6 9 17 4 12"/>
        </svg>
        通过
      </button>
      <button class="approval-btn btn-reject" @click="handleReject">
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
        </svg>
        拒绝
      </button>
    </div>
  </div>
</template>

<style scoped>
.approval-card {
  margin: 8px 0;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  overflow: hidden;
  max-width: 480px;
}

.approval-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-color);
}

.approval-badge {
  padding: 4px 10px;
  background: linear-gradient(135deg, var(--accent-primary), var(--accent-blue));
  border-radius: var(--radius-sm);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  white-space: nowrap;
}

.approval-meta {
  flex: 1;
  min-width: 0;
}

.approval-title {
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.approval-id {
  font-size: 0.75rem;
  color: var(--text-muted);
}

.approval-body {
  padding: 16px 20px;
}

.approval-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-label {
  min-width: 60px;
  font-size: 0.8rem;
  color: var(--text-muted);
}

.info-value {
  font-size: 0.88rem;
  color: var(--text-primary);
}

.info-type {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(108, 99, 255, 0.12);
  border-radius: 4px;
  font-size: 0.82rem;
}

.approval-reason,
.approval-note {
  margin-bottom: 12px;
}

.reason-label,
.note-label {
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.reason-text,
.note-text {
  font-size: 0.88rem;
  color: var(--text-primary);
  line-height: 1.5;
}

.approval-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.detail-label {
  font-size: 0.8rem;
  color: var(--text-muted);
}

.detail-value {
  font-size: 0.85rem;
  color: var(--text-primary);
  text-align: right;
}

.approval-actions {
  display: flex;
  gap: 0;
  border-top: 1px solid var(--border-color);
}

.approval-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  font-size: 0.88rem;
  font-weight: 600;
  transition: all var(--transition-fast);
  border: none;
  cursor: pointer;
}

.btn-approve {
  background: rgba(16, 185, 129, 0.08);
  color: var(--success);
  border-right: 1px solid var(--border-color);
}
.btn-approve:hover {
  background: rgba(16, 185, 129, 0.18);
}

.btn-reject {
  background: rgba(239, 68, 68, 0.08);
  color: var(--error);
}
.btn-reject:hover {
  background: rgba(239, 68, 68, 0.18);
}
</style>
