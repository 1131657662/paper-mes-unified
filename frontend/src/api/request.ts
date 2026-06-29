import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { message } from 'antd'
import type { R } from '../types/common'
import { getAuthSnapshot } from '../stores/authStore'

/** 业务错误码默认文案（后端未带 message 时兜底）。 */
const ERROR_CODE_TEXT: Record<string, string> = {
  E001: '当前状态不允许该操作',
  E002: '数据不存在，请刷新后重试',
  E003: '业务规则冲突',
  E004: '数据已锁定，不可修改',
  E005: '重量偏差超差，需授权放行',
  E006: '数据已被他人修改，请刷新后重试',
}

/** 业务错误：携带后端 code / errorCode，便于调用方按需分支处理（如 E005 弹放行框）。 */
export class BizError extends Error {
  code: number
  errorCode?: string
  constructor(msg: string, code: number, errorCode?: string) {
    super(msg)
    this.name = 'BizError'
    this.code = code
    this.errorCode = errorCode
  }
}

const instance = axios.create({
  baseURL: '/',
  timeout: 15000,
})

instance.interceptors.request.use((config) => {
  const token = getAuthSnapshot().user?.accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 后端恒回 HTTP 200，真实结果在 body.code；这里统一解包 R<T>，成功直出 data。
instance.interceptors.response.use(
  (resp) => {
    const body = resp.data as R<unknown>
    if (body && body.code === 200) {
      return body.data as unknown as AxiosResponse
    }
    const text =
      body?.message || (body?.errorCode && ERROR_CODE_TEXT[body.errorCode]) || '请求失败'
    message.error(text)
    if (body?.code === 401) {
      getAuthSnapshot().actions.signOut()
      if (window.location.pathname !== '/login' && !configUrlEndsWith(resp.config.url, '/api/auth/me')) {
        window.location.href = `/login?from=${encodeURIComponent(window.location.pathname + window.location.search)}`
      }
    }
    return Promise.reject(new BizError(text, body?.code ?? -1, body?.errorCode))
  },
  (error) => {
    // HTTP 层异常（网络断、超时、非 200 的传输错误）。
    const text = error?.message?.includes('timeout')
      ? '请求超时，请重试'
      : '网络异常，请检查连接'
    message.error(text)
    return Promise.reject(error)
  },
)

/** 发起请求，返回解包后的业务数据 T。 */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return instance.request<unknown, T>(config)
}

export default request

function configUrlEndsWith(url: string | undefined, suffix: string) {
  return typeof url === 'string' && url.endsWith(suffix)
}
