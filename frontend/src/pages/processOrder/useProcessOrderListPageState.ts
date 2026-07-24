import { useSearchParams } from 'react-router-dom'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import {
  parseProcessOrderListUrl,
  serializeProcessOrderListUrl,
  type ProcessOrderListUrlState,
  type ProcessOrderSearchFilters,
} from './processOrderListUrlState'
import type { QueueStatus } from './ProcessOrderQueueBar'

export function useProcessOrderListPageState() {
  const [searchParams, setSearchParams] = useSearchParams()
  const parsed = parseProcessOrderListUrl(searchParams)
  const [configuredPageSize, setConfiguredPageSize] = useConfiguredPageSize(20)
  const pageSize = parsed.pageSize ?? configuredPageSize

  const update = (patch: Partial<ProcessOrderListUrlState>) => {
    setSearchParams(serializeProcessOrderListUrl({ ...parsed, ...patch }), { replace: true })
  }
  const setFilters = (filters: ProcessOrderSearchFilters) => update({ filters, page: 1 })
  const setQuickStatus = (quickStatus: QueueStatus) => update({ page: 1, quickStatus })
  const changePage = (page: number, nextPageSize: number) => {
    if (nextPageSize !== pageSize) setConfiguredPageSize(nextPageSize)
    update({ page, pageSize: nextPageSize })
  }

  return { ...parsed, pageSize, changePage, setFilters, setQuickStatus }
}
