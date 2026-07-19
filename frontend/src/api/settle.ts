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
  SettleDetail,
  ReceiveRecord,
  SettlePrintLine,
  ReceiveDTO,
  SettleActionReasonDTO,
  SettleQuoteVO,
  SettleListSummary,
  SettleQuoteByOrdersDTO,
  SettleQuoteByMonthDTO,
  SettleDiscountApproval,
  SettleDiscountApprovalRequestDTO,
  SettleCollectionReminder,
  SettleCollectionReminderRequestDTO,
  SettleCollectionSummary,
} from '../types/settle'
import type { OperationLog } from '../types/operationLog'

export function getSettleOrderList(query: SettleQuery) {
  return request<PageResult<SettleOrder>>({
    url: '/api/settle-orders',
    method: 'get',
    params: query,
  })
}

export function getSettleOrderSummary(query: SettleQuery) {
  return request<SettleListSummary>({
    url: '/api/settle-orders/summary',
    method: 'get',
    params: query,
  })
}

export function getSettleCollectionSummary(query: SettleQuery) {
  return request<SettleCollectionSummary>({
    url: '/api/settle-orders/collection-summary',
    method: 'get',
    params: query,
  })
}

export function getSettleCandidates(query: SettleCandidateQuery) {
  return request<PageResult<SettleCandidateVO>>({
    url: '/api/settle-orders/candidates',
    method: 'get',
    params: query,
  })
}

export function quoteSettleByOrders(data: SettleQuoteByOrdersDTO) {
  return request<SettleQuoteVO>({
    url: '/api/settle-orders/quote/by-orders',
    method: 'post',
    data,
    silentBusinessErrorCodes: ['E001', 'E002', 'E004'],
  })
}

export function quoteSettleByMonth(data: SettleQuoteByMonthDTO) {
  return request<SettleQuoteVO>({
    url: '/api/settle-orders/quote/by-month',
    method: 'post',
    data,
    silentBusinessErrorCodes: ['E001', 'E002', 'E004'],
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

export function getSettleOrderHeader(uuid: string) {
  return request<SettleOrder>({ url: `/api/settle-orders/${uuid}/header`, method: 'get' })
}

export function getSettleDetails(uuid: string) {
  return request<SettleDetail[]>({ url: `/api/settle-orders/${uuid}/details`, method: 'get' })
}

export function getSettleReceives(uuid: string) {
  return request<ReceiveRecord[]>({ url: `/api/settle-orders/${uuid}/receives`, method: 'get' })
}

export function getSettleCollectionReminders(uuid: string) {
  return request<SettleCollectionReminder[]>({
    url: `/api/settle-orders/${uuid}/collection-reminders`,
    method: 'get',
  })
}

export function recordSettleCollectionReminder(uuid: string, data: SettleCollectionReminderRequestDTO) {
  return request<string>({
    url: `/api/settle-orders/${uuid}/collection-reminders`,
    method: 'post',
    data,
  })
}

export function getSettlePrintLines(uuid: string) {
  return request<SettlePrintLine[]>({ url: `/api/settle-orders/${uuid}/print-lines`, method: 'get' })
}

export function getSettleOperationLogs(uuid: string) {
  return request<OperationLog[]>({ url: `/api/settle-orders/${uuid}/operation-logs`, method: 'get' })
}

export function receivePayment(uuid: string, data: ReceiveDTO) {
  return request<void>({
    url: `/api/settle-orders/${uuid}/receive`,
    method: 'post',
    data,
  })
}

export function getSettleDiscountApprovals(uuid: string) {
  return request<SettleDiscountApproval[]>({
    url: `/api/settle-orders/${uuid}/discount-approvals`,
    method: 'get',
  })
}

export function requestSettleDiscountApproval(uuid: string, data: SettleDiscountApprovalRequestDTO) {
  return request<string>({
    url: `/api/settle-orders/${uuid}/discount-approvals`,
    method: 'post',
    data,
  })
}

export function approveSettleDiscount(uuid: string, approvalUuid: string) {
  return request<void>({
    url: `/api/settle-orders/${uuid}/discount-approvals/${approvalUuid}/approve`,
    method: 'post',
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
