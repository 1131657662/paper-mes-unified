import type { Dayjs } from 'dayjs'

export type DeliveryQueueFilter = 'all' | 'pending' | 'done' | 'void'

export interface DeliverySearchFormValues {
  customerUuid?: string
  dateRange?: [Dayjs, Dayjs] | null
  keyword?: string
}

export function deliveryStatus(filter: DeliveryQueueFilter) {
  if (filter === 'pending') return 1
  if (filter === 'done') return 2
  if (filter === 'void') return 3
  return undefined
}

export function tableDensityMode(rowCount: number, pageSize: number, loading: boolean) {
  if (loading) return 'fill'
  if (rowCount === 0) return 'empty'
  return rowCount < pageSize ? 'short' : 'fill'
}
