import request, { rawRequest } from './request'
import type { PageResult } from '../types/common'
import type {
  SettleOrder,
  SettleQuery,
  SettleCandidateQuery,
  SettleCandidateVO,
  SettleByOrderDTO,
  SettleByOrdersDTO,
  SettleByMonthDTO,
  SettleDetailVO,
  ReceiveDTO,
  SettleActionReasonDTO,
} from '../types/settle'

export function getSettleOrderList(query: SettleQuery) {
  return request<PageResult<SettleOrder>>({
    url: '/api/settle-orders',
    method: 'get',
    params: query,
  })
}

export function getSettleCandidates(query: SettleCandidateQuery) {
  return request<SettleCandidateVO[]>({
    url: '/api/settle-orders/candidates',
    method: 'get',
    params: query,
  })
}

export function createSettleByOrder(data: SettleByOrderDTO) {
  return request<string>({
    url: '/api/settle-orders/by-order',
    method: 'post',
    data,
  })
}

export function createSettleByOrders(data: SettleByOrdersDTO) {
  return request<string>({
    url: '/api/settle-orders/by-orders',
    method: 'post',
    data,
  })
}

export function createSettleByMonth(data: SettleByMonthDTO) {
  return request<string>({
    url: '/api/settle-orders/by-month',
    method: 'post',
    data,
  })
}

export function getSettleOrderDetail(uuid: string) {
  return request<SettleDetailVO>({
    url: `/api/settle-orders/${uuid}`,
    method: 'get',
  })
}

export function receivePayment(uuid: string, data: ReceiveDTO) {
  return request<void>({
    url: `/api/settle-orders/${uuid}/receive`,
    method: 'post',
    data,
  })
}

export function cancelReceivePayment(uuid: string, receiveUuid: string, data: SettleActionReasonDTO) {
  return request<void>({
    url: `/api/settle-orders/${uuid}/receives/${receiveUuid}/cancel`,
    method: 'post',
    data,
  })
}

export function voidSettleOrder(uuid: string, data: SettleActionReasonDTO) {
  return request<void>({
    url: `/api/settle-orders/${uuid}/void`,
    method: 'post',
    data,
  })
}

export async function exportSettleOrder(uuid: string) {
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/settle-orders/${uuid}/export`,
    method: 'get',
    responseType: 'blob',
  })
  downloadBlob(response.data, filenameFromDisposition(response.headers['content-disposition']))
}

function downloadBlob(blob: Blob, filename?: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || `结算单_${Date.now()}.xlsx`
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function filenameFromDisposition(disposition?: string) {
  if (!disposition) return undefined
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  return match?.[1] ? decodeURIComponent(match[1]) : undefined
}
