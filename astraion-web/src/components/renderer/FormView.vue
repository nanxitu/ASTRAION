<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  data: Record<string, unknown>
}>()

interface FormField {
  key: string
  label: string
  type?: string
  placeholder?: string
  required?: boolean
  options?: { label: string; value: string | number }[]
  default?: unknown
}

interface FormConfig {
  title?: string
  fields: FormField[]
  submitLabel?: string
  cancelLabel?: string
  submitAction?: string
  cancelAction?: string
}

const config = computed<FormConfig>(() => ({
  fields: (props.data.fields as FormField[]) || [],
  title: props.data.title as string | undefined,
  submitLabel: (props.data.submitLabel as string) || '提交',
  cancelLabel: (props.data.cancelLabel as string) || '取消',
  submitAction: (props.data.submitAction as string) || 'submit',
  cancelAction: (props.data.cancelAction as string) || 'cancel',
}))

// Initialize form values
const formValues = ref<Record<string, unknown>>({})

function initValues() {
  const vals: Record<string, unknown> = {}
  for (const field of config.value.fields) {
    vals[field.key] = field.default !== undefined ? field.default : ''
  }
  formValues.value = vals
}
initValues()

function getInputType(type?: string): string {
  switch (type) {
    case 'password':
      return 'password'
    case 'number':
      return 'number'
    case 'email':
      return 'email'
    case 'textarea':
      return 'text'
    default:
      return 'text'
  }
}

function handleSubmit() {
  const event = new CustomEvent('form-submit', {
    detail: { action: config.value.submitAction, values: { ...formValues.value } },
    bubbles: true,
  })
  document.dispatchEvent(event)
}

function handleCancel() {
  const event = new CustomEvent('form-cancel', {
    detail: { action: config.value.cancelAction },
    bubbles: true,
  })
  document.dispatchEvent(event)
}
</script>

<template>
  <div class="form-view">
    <h3 v-if="config.title" class="form-title">{{ config.title }}</h3>
    <form class="dynamic-form" @submit.prevent="handleSubmit">
      <div
        v-for="field in config.fields"
        :key="field.key"
        class="form-field"
      >
        <label :for="'f-' + field.key" class="field-label">
          {{ field.label }}
          <span v-if="field.required" class="required">*</span>
        </label>

        <!-- Select -->
        <select
          v-if="field.type === 'select' && field.options"
          :id="'f-' + field.key"
          v-model="formValues[field.key]"
          class="input-base field-input"
        >
          <option value="" disabled>{{ field.placeholder || '请选择' }}</option>
          <option
            v-for="opt in field.options"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </option>
        </select>

        <!-- Textarea -->
        <textarea
          v-else-if="field.type === 'textarea'"
          :id="'f-' + field.key"
          v-model="formValues[field.key] as string"
          class="input-base field-input field-textarea"
          :placeholder="field.placeholder"
          rows="4"
        ></textarea>

        <!-- Default input -->
        <input
          v-else
          :id="'f-' + field.key"
          v-model="formValues[field.key] as string"
          class="input-base field-input"
          :type="getInputType(field.type)"
          :placeholder="field.placeholder"
        />
      </div>

      <div class="form-actions">
        <button type="button" class="btn-secondary" @click="handleCancel">
          {{ config.cancelLabel }}
        </button>
        <button type="submit" class="btn-primary">
          {{ config.submitLabel }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.form-view {
  margin: 8px 0;
  padding: 20px;
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
}

.form-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text-primary);
}

.dynamic-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.required {
  color: var(--error);
  margin-left: 2px;
}

.field-input {
  padding: 10px 14px;
}

.field-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}
</style>
