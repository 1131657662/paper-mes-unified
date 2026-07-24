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
  E007: '重量偏差较大，需填写原因',
  E009: '当前计价优惠超过免审额度，请由财务或管理员账号处理',
  E010: '现结加工单存在未结清款项，需要授权放行',
}

const ERROR_NOTIFIED_KEY = '__paperMesErrorNotified'

export interface MesRequestConfig extends AxiosRequestConfig {
  silentBusinessErrorCodes?: readonly string[]
  silentError?: boolean
}

/** 业务错误：携带后端 code / errorCode，便于调用方按需分支处理（如 E005 弹放行框）。 */
export class BizError extends Error {
  code: number
  errorCode?: string
  notified = false

  constructor(msg: string, code: number, errorCode?: string) {
    super(msg)
    this.name = 'BizError'
    this.code = code
    this.errorCode = errorCode
  }
}

/** 避免请求拦截器、React Query、页面 catch 对同一个错误重复弹提示。 */
export function notifyErrorOnce(error: unknown, fallbackText = '请求失败'): void {
  if (isErrorNotified(error)) return
  message.error(errorText(error, fallbackText))
  markErrorNotified(error)
}

const instance = axios.create({
  baseURL: '/',
  timeout: 15000,
  headers: { 'X-Requested-With': 'XMLHttpRequest' },
})

// 成功和普通业务错误仍按 R<T> 解包；认证/授权错误使用真实 HTTP 401/403。
instance.interceptors.response.use(
  (resp) => {
    if (resp.config.responseType === 'blob') {
      return resp as unknown as AxiosResponse
    }
    const body = resp.data as R<unknown>
    if (body && body.code === 200) {
      return body.data as unknown as AxiosResponse
    }
    return rejectBusinessError(body, resp.config)
  },
  (error) => {
    const bizError = businessErrorFromResponse(error?.response?.data)
    if (bizError) {
      notifyAndHandleUnauthorized(
        bizError,
        error?.config?.url,
        shouldNotifyBusinessError(bizError, error?.config),
      )
      return Promise.reject(bizError)
    }
    // HTTP 层异常（网络断、超时、非 200 的传输错误）。
    const text = error?.message?.includes('timeout')
      ? '请求超时，请重试'
      : '网络异常，请检查连接'
    if (error?.config?.silentError) markErrorNotified(error)
    else notifyErrorOnce(error, text)
    return Promise.reject(error)
  },
)

/** 发起请求，返回解包后的业务数据 T。 */
export function request<T = unknown>(config: MesRequestConfig): Promise<T> {
  return instance.request<unknown, T>(config)
}

export const rawRequest = instance

export default request

export function businessErrorFromResponse(value: unknown): BizError | null {
  if (!isBusinessErrorBody(value)) return null
  const text = value.message || (value.errorCode && ERROR_CODE_TEXT[value.errorCode]) || '请求失败'
  return new BizError(text, value.code, value.errorCode)
}

function rejectBusinessError(body: unknown, config?: MesRequestConfig) {
  const bizError = businessErrorFromResponse(body) ?? new BizError('请求失败', -1)
  notifyAndHandleUnauthorized(bizError, config?.url, shouldNotifyBusinessError(bizError, config))
  return Promise.reject(bizError)
}

function notifyAndHandleUnauthorized(error: BizError, url?: string, shouldNotify = true) {
  if (shouldNotify) notifyErrorOnce(error, contextualErrorText(error, url))
  else markErrorNotified(error)
  if (error.code !== 401) return
  getAuthSnapshot().actions.signOut()
  if (window.location.pathname !== '/login' && !configUrlEndsWith(url, '/api/auth/me')) {
    window.location.href = `/login?from=${encodeURIComponent(window.location.pathname + window.location.search)}`
  }
}

export function shouldNotifyBusinessError(error: BizError, config?: MesRequestConfig): boolean {
  if (config?.silentError) return false
  if (!error.errorCode) return true
  const silentCodes = config?.silentBusinessErrorCodes
  return !silentCodes?.includes(error.errorCode)
}

function contextualErrorText(error: BizError, url?: string) {
  if (error.code === 403 && url?.includes('/process-orders/steps/') && url.endsWith('/pricing')) {
    return '当前计价优惠超过免审额度，请由财务或管理员账号处理'
  }
  return error.message
}

function isBusinessErrorBody(value: unknown): value is Pick<R<unknown>, 'code' | 'message' | 'errorCode'> {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Partial<R<unknown>>
  return typeof candidate.code === 'number' && candidate.code !== 200
}

function configUrlEndsWith(url: string | undefined, suffix: string) {
  return typeof url === 'string' && url.endsWith(suffix)
}

function errorText(error: unknown, fallbackText: string) {
  if (error instanceof BizError && error.message) return error.message
  return fallbackText
}

function isErrorNotified(error: unknown) {
  if (error instanceof BizError) return error.notified
  return isNotifiableError(error) && error[ERROR_NOTIFIED_KEY] === true
}

function markErrorNotified(error: unknown) {
  if (error instanceof BizError) {
    error.notified = true
    return
  }
  if (isNotifiableError(error)) {
    error[ERROR_NOTIFIED_KEY] = true
  }
}

function isNotifiableError(error: unknown): error is Record<typeof ERROR_NOTIFIED_KEY, boolean | undefined> {
  return typeof error === 'object' && error !== null
}
