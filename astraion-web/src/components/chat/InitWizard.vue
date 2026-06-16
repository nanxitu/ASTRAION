<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { api } from '@/utils/api'

const emit = defineEmits<{ complete: [] }>()

const steps = ['数据库配置', 'AI 模型配置', '管理员设置']
const currentStep = ref(0)
const loading = ref(false)
const errorMsg = ref('')

// ── Step 1: Database ──
const dbConfig = ref({
  dbType: 'postgresql',
  host: 'localhost',
  port: 5432,
  databaseName: 'astraion',
  username: 'postgres',
  password: '',
})
const dbTestResult = ref<{ success: boolean; message: string } | null>(null)
const dbTestLoading = ref(false)

// ── Step 2: AI ──
interface AiProviderInfo {
  code: string; displayName: string; defaultBaseUrl: string
  models: string[]; defaultModel: string; defaultTemperature: number; defaultMaxTokens: number
}
const providers = ref<AiProviderInfo[]>([])
const activeProvider = computed(() => providers.value.find(p => p.code === aiConfig.value.provider) || null)

const aiConfig = ref({
  provider: 'deepseek', model: 'deepseek-v4-flash',
  baseUrl: 'https://api.deepseek.com', apiKey: '',
  temperature: 1.0, maxTokens: 8192,
})
const aiTestResult = ref<{ success: boolean; message: string } | null>(null)
const aiTestLoading = ref(false)

// ── Step 3: Admin ──
const adminConfig = ref({ username: 'admin', password: '', confirmPassword: '', displayName: '' })

// Load DB config on mount
onMounted(async () => {
  try {
    const res = await api.get<any>('/api/v1/system/db-config')
    if (res.data?.data) {
      Object.assign(dbConfig.value, res.data.data)
    }
  } catch { /* use defaults */ }

  try {
    const res = await api.get<any>('/api/v1/system/ai-providers')
    providers.value = (res.data?.data || []) as AiProviderInfo[]
    if (providers.value.length > 0) onProviderChange(aiConfig.value.provider)
  } catch {
    providers.value = [
      { code: 'deepseek', displayName: 'DeepSeek', defaultBaseUrl: 'https://api.deepseek.com', models: ['deepseek-v4-flash','deepseek-v4-pro','deepseek-chat','deepseek-reasoner'], defaultModel: 'deepseek-v4-flash', defaultTemperature: 1.0, defaultMaxTokens: 8192 },
      { code: 'openai', displayName: 'OpenAI', defaultBaseUrl: 'https://api.openai.com', models: ['gpt-4o','gpt-4o-mini','gpt-4-turbo'], defaultModel: 'gpt-4o-mini', defaultTemperature: 0.7, defaultMaxTokens: 4096 },
    ]
  }
})

function onProviderChange(code: string) {
  const p = providers.value.find(p => p.code === code)
  if (p) {
    aiConfig.value.baseUrl = p.defaultBaseUrl; aiConfig.value.model = p.defaultModel
    aiConfig.value.temperature = p.defaultTemperature; aiConfig.value.maxTokens = p.defaultMaxTokens
  }
}

const canProceed = computed(() => {
  switch (currentStep.value) {
    case 0: return !!(dbConfig.value.host && dbConfig.value.databaseName && dbConfig.value.username)
    case 1: return !!(aiConfig.value.baseUrl && aiConfig.value.apiKey && aiConfig.value.model)
    case 2: return adminConfig.value.username && adminConfig.value.password.length >= 4
      && adminConfig.value.password === adminConfig.value.confirmPassword
    default: return false
  }
})

async function testDbConnection() {
  dbTestLoading.value = true; dbTestResult.value = null
  try {
    const res = await api.post<any>('/api/v1/system/test-db', {
      dbType: dbConfig.value.dbType, host: dbConfig.value.host, port: dbConfig.value.port,
      databaseName: dbConfig.value.databaseName, username: dbConfig.value.username, password: dbConfig.value.password,
    })
    dbTestResult.value = res.data?.data
  } catch (err: any) {
    dbTestResult.value = { success: false, message: err?.message || '连接失败' }
  } finally { dbTestLoading.value = false }
}

async function testAiConnection() {
  aiTestLoading.value = true; aiTestResult.value = null
  try {
    const res = await api.post<any>('/api/v1/system/test-ai', {
      baseUrl: aiConfig.value.baseUrl, apiKey: aiConfig.value.apiKey, model: aiConfig.value.model,
    })
    aiTestResult.value = res.data?.data
  } catch (err: any) {
    aiTestResult.value = { success: false, message: err?.message || '测试失败' }
  } finally { aiTestLoading.value = false }
}

async function nextStep() {
  if (!canProceed.value) return; errorMsg.value = ''

  if (currentStep.value === 0) {
    // Step 1 → Save DB config
    loading.value = true
    try {
      await api.post<any>('/api/v1/system/init/database', {
        dbType: dbConfig.value.dbType, host: dbConfig.value.host, port: dbConfig.value.port,
        databaseName: dbConfig.value.databaseName, username: dbConfig.value.username, password: dbConfig.value.password,
      })
      currentStep.value++
    } catch (err: any) {
      errorMsg.value = err?.response?.data?.message || '保存失败'
    } finally { loading.value = false }
  } else if (currentStep.value === 1) {
    // Step 2 → Save AI config
    loading.value = true
    try {
      await api.post<any>('/api/v1/system/init/ai-model', {
        provider: aiConfig.value.provider, model: aiConfig.value.model,
        baseUrl: aiConfig.value.baseUrl, apiKey: aiConfig.value.apiKey,
        temperature: aiConfig.value.temperature, maxTokens: aiConfig.value.maxTokens,
      })
      currentStep.value++
    } catch (err: any) {
      errorMsg.value = err?.response?.data?.message || 'AI 模型配置失败'
    } finally { loading.value = false }
  } else {
    // Step 3 → Create admin + Complete
    await submitInit()
  }
}

function prevStep() { errorMsg.value = ''; if (currentStep.value > 0) currentStep.value-- }

async function submitInit() {
  loading.value = true; errorMsg.value = ''
  try {
    await api.post<any>('/api/v1/system/init/admin', {
      username: adminConfig.value.username, password: adminConfig.value.password,
      displayName: adminConfig.value.displayName || adminConfig.value.username,
    })
    await api.post<any>('/api/v1/system/init/complete')
    emit('complete')
  } catch (err: any) {
    errorMsg.value = err?.response?.data?.message || '初始化失败，请重试'
  } finally { loading.value = false }
}
</script>

<template>
  <div class="wizard-card">
    <div class="wizard-header">
      <div class="wizard-icon">✦</div>
      <h2 class="wizard-title">初始化 ASTRAION</h2>
      <p class="wizard-desc">三步完成系统初始化，AI Key 仅存数据库不会泄露到配置文件</p>
    </div>

    <div class="steps-bar">
      <div v-for="(step, i) in steps" :key="i" class="step-item" :class="{ active: i === currentStep, done: i < currentStep }">
        <div class="step-number">{{ i < currentStep ? '✓' : i + 1 }}</div>
        <span class="step-label">{{ step }}</span>
      </div>
    </div>

    <div class="wizard-body">
      <!-- ═══ Step 1: Database ═══ -->
      <div v-if="currentStep === 0" class="step-content">
        <h3 class="step-title">数据库配置</h3>
        <p class="step-hint">配置 PostgreSQL 连接参数。此信息将保存到 application.yml。</p>
        <div class="form-row">
          <div class="form-group" style="width:120px">
            <label>类型</label>
            <select v-model="dbConfig.dbType" class="input-base">
              <option value="postgresql">PostgreSQL</option>
            </select>
          </div>
          <div class="form-group flex-1">
            <label>主机</label>
            <input v-model="dbConfig.host" class="input-base" placeholder="localhost" />
          </div>
          <div class="form-group" style="width:80px">
            <label>端口</label>
            <input v-model.number="dbConfig.port" class="input-base" type="number" />
          </div>
        </div>
        <div class="form-group">
          <label>数据库名</label>
          <input v-model="dbConfig.databaseName" class="input-base" placeholder="astraion" />
        </div>
        <div class="form-row">
          <div class="form-group flex-1">
            <label>用户名</label>
            <input v-model="dbConfig.username" class="input-base" placeholder="postgres" />
          </div>
          <div class="form-group flex-1">
            <label>密码</label>
            <input v-model="dbConfig.password" class="input-base" type="password" placeholder="输入密码" />
          </div>
        </div>

        <div class="test-area">
          <button class="btn-secondary test-btn" :disabled="dbTestLoading || !dbConfig.host" @click="testDbConnection">
            <span v-if="dbTestLoading" class="spinner-sm"></span>
            <span v-else>测试连接</span>
          </button>
          <div v-if="dbTestResult" class="test-result" :class="{ success: dbTestResult.success }">
            <span class="test-icon">{{ dbTestResult.success ? '✓' : '✗' }}</span>
            {{ dbTestResult.message }}
          </div>
        </div>
      </div>

      <!-- ═══ Step 2: AI Model ═══ -->
      <div v-if="currentStep === 1" class="step-content">
        <h3 class="step-title">AI 模型配置</h3>
        <p class="step-hint">API Key 仅存数据库，绝不会出现在项目配置文件中</p>
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
          <input v-else v-model="aiConfig.model" class="input-base" placeholder="如 deepseek-v4-flash" />
        </div>
        <div class="form-row">
          <div class="form-group flex-1"><label>API 地址</label><input v-model="aiConfig.baseUrl" class="input-base" /></div>
          <div class="form-group flex-1"><label>API Key</label><input v-model="aiConfig.apiKey" class="input-base" type="password" placeholder="sk-..." /></div>
        </div>
        <div class="form-row">
          <div class="form-group flex-1"><label>Temperature</label><input v-model.number="aiConfig.temperature" class="input-base" type="number" step="0.1" /></div>
          <div class="form-group flex-1"><label>Max Tokens</label><input v-model.number="aiConfig.maxTokens" class="input-base" type="number" /></div>
        </div>
        <div class="test-area">
          <button class="btn-secondary test-btn" :disabled="aiTestLoading || !aiConfig.apiKey" @click="testAiConnection">
            <span v-if="aiTestLoading" class="spinner-sm"></span><span v-else>测试连接</span>
          </button>
          <div v-if="aiTestResult" class="test-result" :class="{ success: aiTestResult.success }">
            <span class="test-icon">{{ aiTestResult.success ? '✓' : '✗' }}</span>{{ aiTestResult.message }}
          </div>
        </div>
      </div>

      <!-- ═══ Step 3: Admin ═══ -->
      <div v-if="currentStep === 2" class="step-content">
        <h3 class="step-title">创建管理员</h3>
        <p class="step-hint">设置系统管理员账号（初始化后 root 账号封存）</p>
        <div class="form-group"><label>用户名</label><input v-model="adminConfig.username" class="input-base" /></div>
        <div class="form-group"><label>显示名称</label><input v-model="adminConfig.displayName" class="input-base" placeholder="可选" /></div>
        <div class="form-group"><label>密码</label><input v-model="adminConfig.password" class="input-base" type="password" placeholder="至少4位" /></div>
        <div class="form-group"><label>确认密码</label><input v-model="adminConfig.confirmPassword" class="input-base" type="password" /></div>
      </div>

      <div v-if="errorMsg" class="wizard-error"><span class="wizard-error-icon">⚠</span>{{ errorMsg }}</div>
    </div>

    <div class="wizard-footer">
      <button v-if="currentStep > 0" class="btn-secondary" @click="prevStep" :disabled="loading">上一步</button>
      <div style="flex:1"></div>
      <span class="step-counter">{{ currentStep + 1 }} / {{ steps.length }}</span>
      <button class="btn-primary" :disabled="!canProceed || loading" @click="nextStep">
        <span v-if="loading" class="spinner"></span>
        <span v-else-if="currentStep === steps.length - 1">完成初始化</span>
        <span v-else>下一步</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.wizard-card { width: 100%; max-width: 540px; background: var(--bg-card); border: 1px solid var(--border-color); border-radius: var(--radius-xl); box-shadow: var(--shadow-lg); overflow: hidden; animation: wizardIn 0.4s ease-out; }
@keyframes wizardIn { from { opacity: 0; transform: scale(0.95) translateY(10px); } to { opacity: 1; transform: scale(1) translateY(0); } }
.wizard-header { text-align: center; padding: 28px 32px 18px; }
.wizard-icon { font-size: 2rem; margin-bottom: 10px; color: var(--accent-primary); }
.wizard-title { font-size: 1.35rem; font-weight: 700; margin-bottom: 4px; }
.wizard-desc { font-size: 0.82rem; color: var(--text-muted); }
.steps-bar { display: flex; justify-content: center; padding: 0 24px 20px; }
.step-item { display: flex; flex-direction: column; align-items: center; gap: 6px; flex: 1; position: relative; }
.step-item::after { content: ''; position: absolute; top: 12px; left: 50%; width: 100%; height: 2px; background: var(--border-color); z-index: 0; }
.step-item:last-child::after { display: none; }
.step-item.done::after { background: var(--accent-primary); }
.step-number { width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 0.72rem; font-weight: 700; background: var(--bg-tertiary); color: var(--text-muted); border: 2px solid var(--border-color); z-index: 1; transition: all 0.2s; }
.step-item.active .step-number { background: var(--accent-primary); border-color: var(--accent-primary); color: #fff; box-shadow: 0 0 8px rgba(108,99,255,0.5); }
.step-item.done .step-number { background: var(--success); border-color: var(--success); color: #fff; }
.step-label { font-size: 0.65rem; color: var(--text-muted); white-space: nowrap; }
.step-item.active .step-label { color: var(--accent-primary); font-weight: 600; }
.wizard-body { padding: 0 28px 20px; }
.step-content { animation: fadeIn 0.3s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateX(8px); } to { opacity: 1; transform: translateX(0); } }
.step-title { font-size: 1.05rem; font-weight: 600; margin-bottom: 4px; }
.step-hint { font-size: 0.8rem; color: var(--text-muted); margin-bottom: 14px; }
.form-row { display: flex; gap: 10px; }
.flex-1 { flex: 1; }
.form-group { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.form-group label { font-size: 0.78rem; font-weight: 500; color: var(--text-secondary); }
.test-area { margin-top: 6px; display: flex; flex-direction: column; gap: 8px; }
.test-btn { padding: 8px 18px; font-size: 0.82rem; }
.test-result { display: flex; align-items: center; gap: 8px; padding: 8px 12px; border-radius: var(--radius-md); font-size: 0.8rem; background: rgba(239,68,68,0.08); color: var(--error); width: 100%; }
.test-result.success { background: rgba(16,185,129,0.08); color: var(--success); }
.test-icon { font-weight: 700; }
.wizard-error { display: flex; align-items: center; gap: 8px; padding: 10px 14px; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: var(--radius-md); color: var(--error); font-size: 0.82rem; margin-top: 10px; }
.wizard-footer { display: flex; align-items: center; padding: 14px 28px; border-top: 1px solid var(--border-color); background: var(--bg-secondary); }
.step-counter { font-size: 0.8rem; color: var(--text-muted); margin-right: 10px; }
.spinner, .spinner-sm { display: inline-block; border-radius: 50%; animation: spin 0.6s linear infinite; }
.spinner { width: 18px; height: 18px; border: 2px solid rgba(255,255,255,0.3); border-top-color: #fff; }
.spinner-sm { width: 14px; height: 14px; border: 2px solid var(--text-muted); border-top-color: var(--accent-primary); }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
