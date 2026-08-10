import type { AxiosRequestConfig } from 'axios'
import request from './request'

export function orvalRequest<T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> {
  return request<T>({
    ...config,
    ...options,
    headers: { ...config.headers, ...options?.headers },
  })
}
