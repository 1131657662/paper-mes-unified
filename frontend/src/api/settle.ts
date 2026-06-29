import request from './request'
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
