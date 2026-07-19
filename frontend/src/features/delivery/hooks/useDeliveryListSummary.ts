import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryQuery } from '../../../types/delivery'

export function useDeliveryListSummary(query: DeliveryQuery) {
  return useQuery({
    ...queries.delivery.summary(query),
    staleTime: 15_000,
  })
}
