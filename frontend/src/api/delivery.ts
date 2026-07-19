import request from './request'
import type { PageResult } from '../types/common'
import type {
  DeliveryOrder,
  DeliveryAppendItemsDTO,
  DeliveryCancelDTO,
  DeliveryQuery,
  AvailableFinishVO,
  AvailableFinishPageVO,
  AvailableFinishQuery,
  DeliveryCreateDTO,
  DeliveryDetailVO,
  DeliveryConfirmDTO,
  DeliveryBatchConfirmDTO,
  DeliveryListSummary,
  DeliveryRollbackDTO,
} from '../types/delivery'

export function getDeliveryOrderList(query: DeliveryQuery) {
  return request<PageResult<DeliveryOrder>>({
    url: '/api/delivery-orders',
    method: 'get',
    params: query,
  })
}

export function getDeliveryOrderSummary(query: DeliveryQuery) {
  return request<DeliveryListSummary>({
    url: '/api/delivery-orders/summary',
    method: 'get',
    params: query,
  })
}

export function getAvailableFinishes(customerUuid: string, warehouseUuid?: string) {
  return request<AvailableFinishVO[]>({
    url: '/api/delivery-orders/available',
    method: 'get',
    params: { customerUuid, warehouseUuid },
  })
}

export function getAvailableFinishPage(query: AvailableFinishQuery) {
  return request<AvailableFinishPageVO>({
    url: '/api/delivery-orders/inventory/available-finishes',
    method: 'get',
    params: query,
    paramsSerializer: { indexes: null },
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

export function confirmDeliveryOrders(data: DeliveryBatchConfirmDTO) {
  return request<void>({
    url: '/api/delivery-orders/batch-confirm',
    method: 'post',
    data,
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
