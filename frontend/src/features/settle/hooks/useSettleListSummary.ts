import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleQuery } from '../../../types/settle'

export function useSettleListSummary(query: SettleQuery, enabled = true) {
  return useQuery({
    ...queries.settle.summary(query),
    enabled,
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
    staleTime: 0,
  })
}
