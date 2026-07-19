import { useCallback, useState } from 'react'
import { useSettleCandidates } from '../../features/settle/hooks/useSettleCandidates'
import type { SettleCandidateQuery, SettleCandidateVO } from '../../types/settle'
import { mergeCandidateSelection } from './settleCandidateSelectionModel'

const DEFAULT_PAGE_SIZE = 20

export function useSettleCandidateSelection(enabled: boolean) {
  const [query, setQuery] = useState<SettleCandidateQuery>({ current: 1, size: DEFAULT_PAGE_SIZE })
  const [selectedByUuid, setSelectedByUuid] = useState<Record<string, SettleCandidateVO>>({})
  const candidatesQuery = useSettleCandidates(query, enabled)
  const { refetch } = candidatesQuery
  const candidates = enabled ? candidatesQuery.data?.records ?? [] : []
  const selectedCandidates = Object.values(selectedByUuid)
  const lockedCustomerUuid = selectedCandidates[0]?.customerUuid

  const setScope = (scope: Pick<SettleCandidateQuery, 'customerUuid' | 'periodStart' | 'periodEnd'>) => {
    setQuery((current) => ({ ...current, ...scope, current: 1 }))
    setSelectedByUuid({})
  }

  const setKeyword = (keyword: string) => {
    setQuery((current) => ({ ...current, keyword: keyword.trim() || undefined, current: 1 }))
  }

  const setPage = (current: number, size: number) => {
    setQuery((value) => ({ ...value, current, size }))
  }

  const updateSelection = (keys: React.Key[]) => {
    const selectedKeys = new Set(keys.map(String))
    setSelectedByUuid((current) => mergeCandidateSelection(current, candidates, selectedKeys))
  }

  const clearSelection = useCallback(() => setSelectedByUuid({}), [])
  const refreshCandidates = useCallback(
    () => refetch(),
    [refetch],
  )

  return {
    candidates,
    candidatesQuery,
    clearSelection,
    lockedCustomerUuid,
    query,
    selectedCandidates,
    selectedRowKeys: Object.keys(selectedByUuid),
    setKeyword,
    setPage,
    setScope,
    refreshCandidates,
    updateSelection,
  }
}
