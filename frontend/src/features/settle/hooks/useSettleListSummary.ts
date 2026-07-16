import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleQuery } from '../../../types/settle'

export function useSettleListSummary(query: SettleQuery) {
  return useQuery(queries.settle.summary(query))
}
