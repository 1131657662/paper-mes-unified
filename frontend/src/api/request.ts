import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { message } from 'antd'
import type { R } from '../types/common'
import { getAuthSnapshot } from '../stores/authStore'
import { formatRequestErrorText } from './requestErrorPresentation'

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
  deferUncertainErrorNotification?: boolean
  silentBusinessErrorCodes?: readonly string[]
  silentError?: boolean
}

/** 业务错误：携带后端 code / errorCode，便于调用方按需分支处理（如 E005 弹放行框）。 */
export class BizError extends Error {
  code: number
  errorCode?: string
  httpStatus?: number
  requestId?: string
  notified = false

  constructor(msg: string, code: number, errorCode?: string, httpStatus?: number, requestId?: string) {
    super(msg)
    this.name = 'BizError'
    this.code = code
    this.errorCode = errorCode
    this.httpStatus = httpStatus
    this.requestId = requestId
  }
}

export function isNotFoundError(error: unknown): boolean {
  if (error instanceof BizError) return error.httpStatus === 404 || error.code === 404
  return axios.isAxiosError(error) && error.response?.status === 404
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

instance.interceptors.request.use((config) => {
  if (!config.headers.has('X-Request-Id')) {
    config.headers.set('X-Request-Id', crypto.randomUUID())
  }
  return config
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
    if (axios.isCancel(error)) {
      markErrorNotified(error)
      return Promise.reject(error)
    }
    const bizError = businessErrorFromResponse(error?.response?.data, error?.response?.status)
    if (bizError) {
      if (shouldDeferUncertainNotification(bizError, error?.config)) return Promise.reject(bizError)
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
    if (shouldDeferUncertainNotification(error, error?.config)) return Promise.reject(error)
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

export function businessErrorFromResponse(value: unknown, httpStatus?: number): BizError | null {
  if (!isBusinessErrorBody(value)) return null
  const text = value.message || (value.errorCode && ERROR_CODE_TEXT[value.errorCode]) || '请求失败'
  return new BizError(text, value.code, value.errorCode, httpStatus, value.requestId)
}

function rejectBusinessError(body: unknown, config?: MesRequestConfig) {
  const bizError = businessErrorFromResponse(body) ?? new BizError('请求失败', -1)
  if (shouldDeferUncertainNotification(bizError, config)) return Promise.reject(bizError)
  notifyAndHandleUnauthorized(bizError, config?.url, shouldNotifyBusinessError(bizError, config))
  return Promise.reject(bizError)
}

export function isUncertainRequestError(error: unknown): boolean {
  if (error instanceof BizError) {
    return (error.httpStatus ?? error.code) >= 500
  }
  if (!axios.isAxiosError(error) || axios.isCancel(error)) return false
  return !error.response || Number(error.response.status) >= 500
}

function shouldDeferUncertainNotification(error: unknown, config?: MesRequestConfig): boolean {
  return config?.deferUncertainErrorNotification === true && isUncertainRequestError(error)
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
  let text = error.message
  if (error.code === 403 && url?.includes('/process-orders/steps/') && url.endsWith('/pricing')) {
    text = '当前计价优惠超过免审额度，请由财务或管理员账号处理'
  }
  return formatRequestErrorText(text, error)
}

function isBusinessErrorBody(value: unknown): value is Pick<R<unknown>, 'code' | 'message' | 'errorCode' | 'requestId'> {
  if (typeof value !== 'object' || value === null) return false
  const candidate = value as Partial<R<unknown>>
  return typeof candidate.code === 'number' && candidate.code !== 200
}

function configUrlEndsWith(url: string | undefined, suffix: string) {
  return typeof url === 'string' && url.endsWith(suffix)
}

function errorText(error: unknown, fallbackText: string) {
  if (error instanceof BizError && error.message) return formatRequestErrorText(error.message, error)
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
