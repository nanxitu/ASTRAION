<script setup lang="ts">
import { computed } from 'vue'
import TableView from './TableView.vue'
import FormView from './FormView.vue'
import DetailView from './DetailView.vue'
import ConfirmCard from './ConfirmCard.vue'
import ApprovalCard from './ApprovalCard.vue'

const props = defineProps<{
  renderType: string
  data: Record<string, unknown>
}>()

const component = computed(() => {
  switch (props.renderType) {
    case 'table':
      return TableView
    case 'form':
      return FormView
    case 'detail':
      return DetailView
    case 'confirm':
      return ConfirmCard
    case 'approval':
      return ApprovalCard
    default:
      return null
  }
})
</script>

<template>
  <div class="dynamic-renderer">
    <template v-if="component">
      <component :is="component" :data="data" />
    </template>
    <div v-else class="renderer-error">
      未知渲染类型: {{ renderType }}
    </div>
  </div>
</template>

<style scoped>
.dynamic-renderer {
  margin: 8px 0;
}

.renderer-error {
  padding: 12px 16px;
  background: rgba(239, 68, 68, 0.08);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: var(--radius-md);
  color: var(--error);
  font-size: 0.85rem;
}
</style>
