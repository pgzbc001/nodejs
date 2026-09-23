import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

// Axios 实例：baseURL /api/v1（dev 经 vite proxy 转发 8080）
const http = axios.create({
  baseURL: '/api/v1',
  timeout: 120000
})

// 请求拦截：注入 X-User-* 演示身份
http.interceptors.request.use((config) => {
  const store = useUserStore()
  config.headers['X-User-Id'] = store.userId
  config.headers['X-User-Name'] = store.userName
  config.headers['X-User-Role'] = store.role
  return config
})

// 响应拦截：统一解包 ApiResponse{code,message,data}，code!==0 拒绝并提示
http.interceptors.response.use(
  async (response) => {
    if (response.config.responseType === 'blob') {
      // blob 请求若实际返回 JSON（错误响应），解析后按错误处理
      const type = response.headers['content-type'] || ''
      if (type.includes('application/json')) {
        const text = await response.data.text()
        let body = null
        try {
          body = JSON.parse(text)
        } catch (e) {
          body = null
        }
        const message = body?.message || '文件处理失败'
        ElMessage.error(message)
        const error = new Error(message)
        error.code = body?.code
        error.detail = body?.data
        return Promise.reject(error)
      }
      return response
    }
    const body = response.data
    if (body && typeof body === 'object' && typeof body.code === 'number') {
      if (body.code === 0) {
        return body.data
      }
      ElMessage.error(body.message || `请求失败（${body.code}）`)
      if (body.code === 40300) {
        ElMessage.warning('当前角色无权执行该操作，请在右上角切换角色后重试')
      }
      const error = new Error(body.message || '请求失败')
      error.code = body.code
      error.detail = body.data // 40000 时为 { 字段名: 提示 } 供表单红字标注
      return Promise.reject(error)
    }
    return body
  },
  (error) => {
    const body = error.response?.data
    const message = body?.message || error.message || '网络异常，请稍后重试'
    ElMessage.error(message)
    if (body?.code === 40300) {
      ElMessage.warning('当前角色无权执行该操作，请在右上角切换角色后重试')
    }
    const wrapped = new Error(message)
    wrapped.code = body?.code
    wrapped.detail = body?.data
    return Promise.reject(wrapped)
  }
)

/**
 * 文件下载：传入 axios 请求（responseType=blob），解析 Content-Disposition 文件名并触发下载。
 */
export async function download(request, fallbackName = 'download.xlsx') {
  const response = await request
  const disposition = response.headers['content-disposition'] || ''
  let filename = fallbackName
  const match = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (match) {
    try {
      filename = decodeURIComponent(match[1])
    } catch (e) {
      filename = fallbackName
    }
  }
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export default http
