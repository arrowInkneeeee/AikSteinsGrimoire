import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from './types/api'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE as string,
  timeout: 15000,
})

// 响应拦截器：解包 ApiResponse
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    // blob 响应不走 JSON 解包
    if (response.config.responseType === 'blob') {
      return response
    }
    if (res.code === 200 && res.success) {
      return res.data as any
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  },
)

/** 普通 JSON 请求 */
export function get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config) as Promise<T>
}

export function post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

/** blob 下载请求（跳过 JSON 解包） */
export function getBlob(url: string, params?: Record<string, unknown>): Promise<AxiosResponse<Blob>> {
  return service.get(url, {
    params,
    responseType: 'blob',
  }) as Promise<AxiosResponse<Blob>>
}

export default service
