import request from './request'
import type { PageResult } from '../types/common'
import type {
  DeliveryOrder,
  DeliveryQuery,
  AvailableFinishVO,
  DeliveryCreateDTO,
  DeliveryDetailVO,
  DeliveryConfirmDTO,
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
