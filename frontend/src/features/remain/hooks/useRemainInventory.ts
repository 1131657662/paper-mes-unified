import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { RemainInventoryQuery } from '../../../types/remain'

export function useRemainInventory(query: RemainInventoryQuery) {
  return useQuery({ ...queries.remain.inventory(query), staleTime: 15_000 })
}
