import axios from 'axios'

const api = axios.create({
  baseURL: '', // proxied through Vite
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor — attach JWT token
api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem('astraion_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Response interceptor — handle 401
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      sessionStorage.removeItem('astraion_token')
      sessionStorage.removeItem('astraion_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  },
)

export { api }
