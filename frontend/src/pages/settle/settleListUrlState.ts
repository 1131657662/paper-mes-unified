import dayjs from 'dayjs'
import type { SettleCollectionQueue, SettleQuery } from '../../types/settle'
import type { SettleSearchFormValues } from './SettleSearchBar'
import type { SettleListViewMode } from './SettleListModeActions'

export type QueueFilter = 'all' | 'pending' | 'partial' | 'paid' | 'void'

export interface SettleListUrlState {
  collectionQueue: SettleCollectionQueue
  filters: SettleQuery
  page: number
  pageSize?: number
  queueFilter: QueueFilter
  viewMode: SettleListViewMode
}

export function parseSettleListUrl(params: URLSearchParams): SettleListUrlState {
  return {
    collectionQueue: collectionQueueValue(params.get('collectionQueue')),
    filters: filtersFromSearch(params),
    page: positiveInt(params.get('page'), 1),
    pageSize: positiveIntOrUndefined(params.get('size')),
    queueFilter: queueFilterValue(params.get('queue')),
    viewMode: params.get('view') === 'collection' ? 'collection' : 'documents',
  }
}

export function searchFormValues(filters: SettleQuery): SettleSearchFormValues {
  const dateFrom = filters.dateFrom ? dayjs(filters.dateFrom) : undefined
  const dateTo = filters.dateTo ? dayjs(filters.dateTo) : undefined
  return {
    customerUuid: filters.customerUuid,
    dateRange: dateFrom && dateTo ? [dateFrom, dateTo] : null,
    keyword: filters.keyword,
    settleType: filters.settleType,
  }
}

export function serializeSettleListUrl(state: SettleListUrlState): URLSearchParams {
  const params = new URLSearchParams()
  if (state.viewMode === 'collection') params.set('view', 'collection')
  if (state.queueFilter !== 'all') params.set('queue', state.queueFilter)
  if (state.viewMode === 'collection') params.set('collectionQueue', state.collectionQueue)
  if (state.page > 1) params.set('page', String(state.page))
  if (state.pageSize) params.set('size', String(state.pageSize))
  Object.entries(state.filters).forEach(([key, value]) => {
    if (value !== undefined && value !== '') params.set(key, String(value))
  })
  return params
}

function filtersFromSearch(params: URLSearchParams): SettleQuery {
  const settleType = Number(params.get('settleType'))
  return {
    customerUuid: params.get('customerUuid') || undefined,
    dateFrom: params.get('dateFrom') || undefined,
    dateTo: params.get('dateTo') || undefined,
    keyword: params.get('keyword') || undefined,
    settleType: settleType === 1 || settleType === 2 ? settleType : undefined,
  }
}

function queueFilterValue(value: string | null): QueueFilter {
  return value === 'pending' || value === 'partial' || value === 'paid' || value === 'void' ? value : 'all'
}

function collectionQueueValue(value: string | null): SettleCollectionQueue {
  return value === 'today' || value === 'upcoming' || value === 'reminded' || value === 'overdue' ? value : 'overdue'
}

function positiveInt(value: string | null, fallback: number) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback
}

function positiveIntOrUndefined(value: string | null) {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed > 0 ? parsed : undefined
}
