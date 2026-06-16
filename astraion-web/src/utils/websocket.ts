import { ref } from 'vue'

export type WsStatus = 'disconnected' | 'connecting' | 'connected'

export interface WsMessage {
  type: 'user' | 'ai' | 'system' | 'error'
  content: string
  metadata?: Record<string, unknown>
  renderData?: Record<string, unknown>
  renderType?: string
}

export interface StreamChunk {
  type: 'delta' | 'complete' | 'error'
  content?: string
  renderData?: Record<string, unknown>
  renderType?: string
  error?: string
}

type MessageCallback = (msg: WsMessage) => void
type StatusCallback = (status: WsStatus) => void

class WebSocketManager {
  private ws: WebSocket | null = null
  private url = ''
  private token = ''
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private maxReconnectAttempts = 5
  private reconnectAttempts = 0

  public status = ref<WsStatus>('disconnected')
  private messageCallbacks: MessageCallback[] = []
  private statusCallbacks: StatusCallback[] = []

  connect(url: string, token: string) {
    if (this.ws?.readyState === WebSocket.OPEN) return

    this.url = url
    this.token = token
    this.status.value = 'connecting'
    this.notifyStatus()

    const wsUrl = `${url}?token=${encodeURIComponent(token)}`
    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      this.status.value = 'connected'
      this.reconnectAttempts = 0
      this.notifyStatus()
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as StreamChunk | WsMessage
        this.handleMessage(data)
      } catch {
        // Plain text message
        this.messageCallbacks.forEach((cb) =>
          cb({ type: 'ai', content: event.data }),
        )
      }
    }

    this.ws.onclose = () => {
      this.status.value = 'disconnected'
      this.notifyStatus()
      this.attemptReconnect()
    }

    this.ws.onerror = () => {
      this.status.value = 'disconnected'
      this.notifyStatus()
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    this.reconnectAttempts = this.maxReconnectAttempts // prevent reconnect
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.status.value = 'disconnected'
    this.notifyStatus()
  }

  send(data: string | Record<string, unknown>) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(typeof data === 'string' ? data : JSON.stringify(data))
      return true
    }
    return false
  }

  private handleMessage(data: StreamChunk | WsMessage) {
    if ('delta' in data && data.type === 'delta') {
      // Streaming delta — accumulate in a temporary message
      this.messageCallbacks.forEach((cb) =>
        cb({
          type: 'ai',
          content: data.content || '',
          metadata: { streaming: true },
          renderData: data.renderData,
          renderType: data.renderType,
        }),
      )
    } else if ('delta' in data && data.type === 'complete') {
      this.messageCallbacks.forEach((cb) =>
        cb({
          type: 'ai',
          content: data.content || '',
          metadata: { streaming: false, complete: true },
          renderData: data.renderData,
          renderType: data.renderType,
        }),
      )
    } else {
      const msg = data as WsMessage
      this.messageCallbacks.forEach((cb) => cb(msg))
    }
  }

  private attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) return
    if (this.reconnectTimer) return

    const delay = Math.min(1000 * Math.pow(2, this.reconnectAttempts), 10000)
    this.reconnectAttempts++

    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      this.connect(this.url, this.token)
    }, delay)
  }

  onMessage(cb: MessageCallback) {
    this.messageCallbacks.push(cb)
    return () => {
      this.messageCallbacks = this.messageCallbacks.filter((c) => c !== cb)
    }
  }

  onStatusChange(cb: StatusCallback) {
    this.statusCallbacks.push(cb)
    return () => {
      this.statusCallbacks = this.statusCallbacks.filter((c) => c !== cb)
    }
  }

  private notifyStatus() {
    this.statusCallbacks.forEach((cb) => cb(this.status.value))
  }
}

export const wsManager = new WebSocketManager()
