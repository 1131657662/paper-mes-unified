import { useState } from 'react'
import { readTablePreferences, updateTablePreferences } from './tablePreferences'

export function useTableSortState<TSort>(
  storageKey: string,
  isValid: (value: unknown) => value is TSort,
  scope = 'physical',
) {
  const [sortChain, setSortChain] = useState<TSort[]>(() => {
    const saved = readTablePreferences<unknown>(storageKey)
    const savedChain = scope === 'physical' ? saved.sortChain : saved.sortChains[scope] ?? []
    return savedChain.filter(isValid)
  })

  const updateSortChain = (next: TSort[]) => {
    const valid = next.filter(isValid)
    setSortChain(valid)
    updateTablePreferences(storageKey, (current) => ({
      ...current,
      sortChain: scope === 'physical' ? valid : current.sortChain,
      sortChains: { ...current.sortChains, [scope]: valid },
    }))
  }

  return { sortChain, updateSortChain }
}
