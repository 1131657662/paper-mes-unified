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
  SettleQuoteVO,
  SettleListSummary,
  SettleQuoteByOrdersDTO,
  SettleQuoteByMonthDTO,
  SettleDiscountApproval,
  SettleDiscountApprovalRequestDTO,
} from '../types/settle'
import { downloadFileFromResponse } from '../utils/downloadFile'
import {
  normalizeDocumentExportInput,
  readableExportFilename,
  type DocumentExportInput,
} from '../utils/documentExport'

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
  })
}

export function quoteSettleByMonth(data: SettleQuoteByMonthDTO) {
  return request<SettleQuoteVO>({
    url: '/api/settle-orders/quote/by-month',
    method: 'post',
    data,
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

export async function exportSettleOrder(input: DocumentExportInput) {
  const { documentNo, uuid } = normalizeDocumentExportInput(input)
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: `/api/settle-orders/${uuid}/export`,
    method: 'get',
    responseType: 'blob',
  })
  await downloadFileFromResponse(response, readableExportFilename('结算单', documentNo))
}
