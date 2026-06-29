import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { SettleQuery } from '../../../types/settle'

export function useSettleOrders(query: SettleQuery) {
  return useQuery(queries.settle.list(query))
}
