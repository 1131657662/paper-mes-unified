import { useQuery } from '@tanstack/react-query'
import type { SettleQuery } from '../../../types/settle'
import { settleKeys } from '../queries/settleKeys'

export function useSettleCollectionSummary(query: SettleQuery, enabled = true) {
  return useQuery({
    ...settleKeys.collectionSummary(query),
    enabled,
  })
}
