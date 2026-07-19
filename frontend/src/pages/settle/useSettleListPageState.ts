import { useSearchParams } from 'react-router-dom'
import { useConfiguredPageSize } from '../../features/systemConfig/hooks/useConfiguredPageSize'
import type { SettleQuery } from '../../types/settle'
import type { SettleListViewMode } from './SettleListModeActions'
import {
  parseSettleListUrl,
  searchFormValues,
  serializeSettleListUrl,
  type QueueFilter,
} from './settleListUrlState'

export function useSettleListPageState() {
  const [searchParams, setSearchParams] = useSearchParams()
  const parsed = parseSettleListUrl(searchParams)
  const [configuredPageSize, setConfiguredPageSize] = useConfiguredPageSize(20)
  const pageSize = parsed.pageSize ?? configuredPageSize

  const update = (patch: Partial<typeof parsed>) => {
    setSearchParams(serializeSettleListUrl({ ...parsed, ...patch }), { replace: true })
  }

  const setFilters = (filters: SettleQuery) => update({ filters, page: 1 })
  const setPage = (page: number) => update({ page })
  const setPageSize = (size: number) => {
    setConfiguredPageSize(size)
    update({ page: 1, pageSize: size })
  }
  const setQueueFilter = (queueFilter: QueueFilter) => update({ page: 1, queueFilter })
  const setCollectionQueue = (collectionQueue: SettleQuery['collectionQueue']) => {
    if (!collectionQueue) return
    update({ collectionQueue, page: 1 })
  }
  const setViewMode = (viewMode: SettleListViewMode) => update({ page: 1, viewMode })

  return {
    ...parsed,
    formInitialValues: searchFormValues(parsed.filters),
    pageSize,
    setCollectionQueue,
    setFilters,
    setPage,
    setPageSize,
    setQueueFilter,
    setViewMode,
  }
}
