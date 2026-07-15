import request, { rawRequest } from './request'
import type { PageResult } from '../types/common'
import type {
  DeliveryOrder,
  DeliveryAppendItemsDTO,
  DeliveryCancelDTO,
  DeliveryQuery,
  AvailableFinishVO,
  DeliveryCreateDTO,
  DeliveryDetailVO,
  DeliveryConfirmDTO,
  DeliveryRollbackDTO,
} from '../types/delivery'
import { downloadFileFromResponse } from '../utils/downloadFile'
import {
  normalizeDocumentExportInput,
  readableExportFilename,
  type DocumentExportInput,
} from '../utils/documentExport'

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

export function cancelPendingDeliveryOrder(uuid: string, data: DeliveryCancelDTO) {
  return request<void>({
    url: `/api/delivery-orders/${uuid}/cancel`,
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

export async function exportDeliveryOrder(input: DocumentExportInput) {
  const { documentNo, uuid } = normalizeDocumentExportInput(input)
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/delivery-orders/${uuid}/export`,
    method: 'get',
    responseType: 'blob',
  })
  await downloadFileFromResponse(response, readableExportFilename('出库单', documentNo))
}
