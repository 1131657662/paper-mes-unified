import request, { rawRequest } from './request'
import type { PageResult } from '../types/common'
import type {
  DeliveryOrder,
  DeliveryAppendItemsDTO,
  DeliveryQuery,
  AvailableFinishVO,
  DeliveryCreateDTO,
  DeliveryDetailVO,
  DeliveryConfirmDTO,
  DeliveryRollbackDTO,
} from '../types/delivery'

export function getDeliveryOrderList(query: DeliveryQuery) {
  return request<PageResult<DeliveryOrder>>({
    url: '/api/delivery-orders',
    method: 'get',
    params: query,
  })
}

export function getAvailableFinishes(customerUuid: string) {
  return request<AvailableFinishVO[]>({
    url: '/api/delivery-orders/available',
    method: 'get',
    params: { customerUuid },
  })
}

export function createDeliveryOrder(data: DeliveryCreateDTO) {
  return request<string>({
    url: '/api/delivery-orders',
    method: 'post',
    data,
  })
}

export function getDeliveryOrderDetail(uuid: string) {
  return request<DeliveryDetailVO>({
    url: `/api/delivery-orders/${uuid}`,
    method: 'get',
  })
}

export function confirmDeliveryOrder(uuid: string, data?: DeliveryConfirmDTO) {
  return request<void>({
    url: `/api/delivery-orders/${uuid}/confirm`,
    method: 'post',
    data: data || {},
  })
}

export function rollbackDeliveryOrder(uuid: string, data: DeliveryRollbackDTO) {
  return request<void>({
    url: `/api/delivery-orders/${uuid}/rollback`,
    method: 'post',
    data,
  })
}

export function appendDeliveryDetails(uuid: string, data: DeliveryAppendItemsDTO) {
  return request<void>({
    url: `/api/delivery-orders/${uuid}/details`,
    method: 'post',
    data,
  })
}

export function removeDeliveryDetail(uuid: string, detailUuid: string) {
  return request<void>({
    url: `/api/delivery-orders/${uuid}/details/${detailUuid}`,
    method: 'delete',
  })
}

export async function exportDeliveryOrder(uuid: string) {
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/delivery-orders/${uuid}/export`,
    method: 'get',
    responseType: 'blob',
  })
  downloadBlob(response.data, filenameFromDisposition(response.headers['content-disposition']))
}

function downloadBlob(blob: Blob, filename?: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename || `出库单_${Date.now()}.xlsx`
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
