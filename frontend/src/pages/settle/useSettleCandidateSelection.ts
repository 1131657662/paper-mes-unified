import { useCallback, useState } from 'react'
import { useSettleCandidates } from '../../features/settle/hooks/useSettleCandidates'
import type { SettleCandidateQuery, SettleCandidateVO } from '../../types/settle'
import { mergeCandidateSelection } from './settleCandidateSelectionModel'

const DEFAULT_PAGE_SIZE = 20

export function useSettleCandidateSelection(enabled: boolean, initialOrderUuids: string[] = []) {
  const [query, setQuery] = useState<SettleCandidateQuery>({
    current: 1,
    orderUuids: initialOrderUuids.length ? initialOrderUuids : undefined,
    size: initialOrderUuids.length ? 100 : DEFAULT_PAGE_SIZE,
  })
  const [selectedByUuid, setSelectedByUuid] = useState<Record<string, SettleCandidateVO>>({})
  const [initialSelection, setInitialSelection] = useState(initialOrderUuids)
  const candidatesQuery = useSettleCandidates(query, enabled)
  const { refetch } = candidatesQuery
  const candidates = enabled ? candidatesQuery.data?.records ?? [] : []
  const initialKeys = new Set(initialSelection)
  const selectedCandidateMap = mergeCandidateSelection(selectedByUuid, candidates, initialKeys)
  const selectedCandidates = Object.values(selectedCandidateMap)
  const lockedCustomerUuid = selectedCandidates[0]?.customerUuid

  const setScope = (scope: Pick<SettleCandidateQuery, 'customerUuid' | 'periodStart' | 'periodEnd'>) => {
    setQuery((current) => ({ ...current, ...scope, current: 1, orderUuids: undefined }))
    setSelectedByUuid({})
    setInitialSelection([])
  }

  const setKeyword = (keyword: string) => {
    setQuery((current) => ({ ...current, keyword: keyword.trim() || undefined, current: 1 }))
  }

  const setPage = (current: number, size: number) => {
    setQuery((value) => ({ ...value, current, size }))
  }

  const updateSelection = (keys: React.Key[]) => {
    setInitialSelection([])
    const selectedKeys = new Set(keys.map(String))
    setSelectedByUuid((current) => mergeCandidateSelection(current, candidates, selectedKeys))
  }

  const clearSelection = useCallback(() => {
    setSelectedByUuid({})
    setInitialSelection([])
  }, [])
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
    selectedRowKeys: Object.keys(selectedCandidateMap),
    setKeyword,
    setPage,
    setScope,
    refreshCandidates,
    updateSelection,
  }
}
