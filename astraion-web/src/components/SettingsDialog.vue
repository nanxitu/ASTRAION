<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { api } from '@/utils/api'

const emit = defineEmits<{ close: [] }>()

interface AiProviderInfo {
  code: string
  displayName: string
  defaultBaseUrl: string
  models: string[]
  defaultModel: string
  defaultTemperature: number
  defaultMaxTokens: number
}

const providers = ref<AiProviderInfo[]>([])
const activeProvider = computed(() => providers.value.find(p => p.code === aiConfig.value.provider) || null)

const aiConfig = ref({
  provider: '',
  model: '',
  baseUrl: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 4096,
})

const saving = ref(false)
const testing = ref(false)
const testResult = ref<{ success: boolean; message: string } | null>(null)
const saveMessage = ref('')

onMounted(async () => {
  try {
    const [provRes, cfgRes] = await Promise.all([
      api.get<any>('/api/v1/system/ai-providers'),
      api.get<any>('/api/v1/system/ai-config'),
    ])
    providers.value = (provRes.data?.data || []) as AiProviderInfo[]
    const cfg = cfgRes.data?.data || {}
    aiConfig.value.provider = cfg.provider || ''
    aiConfig.value.model = cfg.model || ''
    aiConfig.value.baseUrl = cfg.baseUrl || ''
    aiConfig.value.apiKey = cfg.apiKey || ''  // masked
    aiConfig.value.temperature = parseFloat(cfg.temperature) || 0.7
    aiConfig.value.maxTokens = parseInt(cfg.maxTokens) || 4096
  } catch {
    // fallback
    providers.value = [
      { code: 'deepseek', displayName: 'DeepSeek', defaultBaseUrl: 'https://api.deepseek.com', models: ['deepseek-v4-flash', 'deepseek-v4-pro'], defaultModel: 'deepseek-v4-flash', defaultTemperature: 1.0, defaultMaxTokens: 8192 },
      { code: 'openai', displayName: 'OpenAI', defaultBaseUrl: 'https://api.openai.com', models: ['gpt-4o', 'gpt-4o-mini'], defaultModel: 'gpt-4o-mini', defaultTemperature: 0.7, defaultMaxTokens: 4096 },
    ]
  }
})

function onProviderChange(code: string) {
  const p = providers.value.find(p => p.code === code)
  if (p) {
    aiConfig.value.baseUrl = p.defaultBaseUrl
    aiConfig.value.model = p.defaultModel
    aiConfig.value.temperature = p.defaultTemperature
    aiConfig.value.maxTokens = p.defaultMaxTokens
  }
}

async function testConnection() {
  testing.value = true
  testResult.value = null
  try {
    const res = await api.post<any>('/api/v1/system/test-ai', {
      baseUrl: aiConfig.value.baseUrl,
      apiKey: aiConfig.value.apiKey,
      model: aiConfig.value.model,
    })
    testResult.value = res.data?.data
  } catch (err: any) {
    testResult.value = { success: false, message: err?.message || '测试失败' }
  } finally {
    testing.value = false
  }
}

async function save() {
  saving.value = true
  saveMessage.value = ''
  try {
    await api.put<any>('/api/v1/system/ai-config', {
      provider: aiConfig.value.provider,
      model: aiConfig.value.model,
      baseUrl: aiConfig.value.baseUrl,
      apiKey: aiConfig.value.apiKey,
      temperature: aiConfig.value.temperature,
      maxTokens: aiConfig.value.maxTokens,
    })
    saveMessage.value = '✅ 配置已保存'
    setTimeout(() => emit('close'), 1200)
  } catch (err: any) {
    saveMessage.value = '❌ ' + (err?.response?.data?.message || err?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="dialog-overlay" @click.self="emit('close')">
    <div class="dialog-card">
      <div class="dialog-header">
        <h2>系统设置</h2>
        <button class="close-btn" @click="emit('close')">&times;</button>
      </div>

      <div class="dialog-body">
        <h3 class="section-title">AI 模型配置</h3>

        <div class="form-group">
          <label>提供商</label>
          <select v-model="aiConfig.provider" class="input-base" @change="onProviderChange(aiConfig.provider)">
            <option v-for="p in providers" :key="p.code" :value="p.code">{{ p.displayName }}</option>
            <option value="custom">自定义</option>
          </select>
        </div>

        <div class="form-group">
          <label>模型</label>
          <select v-if="activeProvider && activeProvider.models.length > 1" v-model="aiConfig.model" class="input-base">
            <option v-for="m in activeProvider.models" :key="m" :value="m">{{ m }}</option>
          </select>
          <input v-else v-model="aiConfig.model" class="input-base" placeholder="模型名" />
        </div>

        <div class="form-group">
          <label>API 地址</label>
          <input v-model="aiConfig.baseUrl" class="input-base" placeholder="https://api.deepseek.com" />
        </div>

        <div class="form-group">
          <label>API Key</label>
          <input v-model="aiConfig.apiKey" class="input-base" type="password" placeholder="sk-..." />
        </div>

        <div class="form-row">
          <div class="form-group flex-1">
            <label>Temperature</label>
            <input v-model.number="aiConfig.temperature" class="input-base" type="number" step="0.1" min="0" max="2" />
          </div>
          <div class="form-group flex-1">
            <label>Max Tokens</label>
            <input v-model.number="aiConfig.maxTokens" class="input-base" type="number" min="1" />
          </div>
        </div>

        <div class="action-row">
          <button class="btn-secondary" :disabled="testing" @click="testConnection">
            {{ testing ? '测试中...' : '测试连接' }}
          </button>
          <div v-if="testResult" class="test-result" :class="{ success: testResult.success }">
            {{ testResult.success ? '✓' : '✗' }} {{ testResult.message }}
          </div>
        </div>

        <div v-if="saveMessage" class="save-msg" :class="{ success: saveMessage.startsWith('✅') }">
          {{ saveMessage }}
        </div>
      </div>

      <div class="dialog-footer">
        <button class="btn-secondary" @click="emit('close')">取消</button>
        <button class="btn-primary" :disabled="saving" @click="save">
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(4px);
}

.dialog-card {
  width: 90%;
  max-width: 520px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: dialogIn 0.25s ease-out;
}

@keyframes dialogIn {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
}

.dialog-header h2 {
  font-size: 1.1rem;
  font-weight: 700;
}

.close-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: 50%;
  font-size: 1.2rem;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}

.close-btn:hover {
  background: var(--bg-hover);
  color: var(--text-primary);
}

.dialog-body {
  padding: 20px 24px;
}

.section-title {
  font-size: 0.9rem;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--accent-primary);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 12px;
}

.form-group label {
  font-size: 0.8rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 { flex: 1; }

.action-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.test-result {
  padding: 6px 12px;
  border-radius: var(--radius-md);
  font-size: 0.82rem;
  background: rgba(239, 68, 68, 0.08);
  color: var(--error);
}

.test-result.success {
  background: rgba(16, 185, 129, 0.08);
  color: var(--success);
}

.save-msg {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  font-size: 0.85rem;
  background: rgba(239, 68, 68, 0.08);
  color: var(--error);
  text-align: center;
}

.save-msg.success {
  background: rgba(16, 185, 129, 0.08);
  color: var(--success);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
}
</style>
