import { mesPageSizeOptions } from '../../components/biz/mesPaginationUtils'
import type { QueueStatus } from './ProcessOrderQueueBar'

export interface ProcessOrderSearchFilters {
  keyword?: string
  customerUuid?: string
  dateFrom?: string
  dateTo?: string
}

export interface ProcessOrderListUrlState {
  filters: ProcessOrderSearchFilters
  page: number
  pageSize?: number
  quickStatus: QueueStatus
}

export function parseProcessOrderListUrl(params: URLSearchParams): ProcessOrderListUrlState {
  return {
    filters: filtersFromSearch(params),
    page: positiveInt(params.get('page'), 1),
    pageSize: pageSizeValue(params.get('size')),
    quickStatus: queueStatusValue(params.get('orderStatus')),
  }
}

export function serializeProcessOrderListUrl(state: ProcessOrderListUrlState): URLSearchParams {
  const params = new URLSearchParams()
  if (state.quickStatus !== 'all') params.set('orderStatus', state.quickStatus)
  if (state.page > 1) params.set('page', String(state.page))
  if (state.pageSize) params.set('size', String(state.pageSize))
  Object.entries(state.filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, value)
  })
  return params
}

function filtersFromSearch(params: URLSearchParams): ProcessOrderSearchFilters {
  return {
    keyword: textValue(params.get('keyword')),
    customerUuid: textValue(params.get('customerUuid')),
    dateFrom: dateValue(params.get('dateFrom')),
    dateTo: dateValue(params.get('dateTo')),
  }
}

function queueStatusValue(value: string | null): QueueStatus {
  return value != null && isQueueStatus(value) ? value : 'all'
}

function isQueueStatus(value: string): value is Exclude<QueueStatus, 'all'> {
  return value === '0' || value === '1' || value === '2' || value === '3'
    || value === '4' || value === '5' || value === '6'
}

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function pageSizeValue(value: string | null) {
  const parsed = Number(value)
  return mesPageSizeOptions.includes(parsed) ? parsed : undefined
}

function textValue(value: string | null) {
  const trimmed = value?.trim()
  return trimmed || undefined
}

function dateValue(value: string | null) {
  return value && /^\d{4}-\d{2}-\d{2}$/.test(value) ? value : undefined
}
