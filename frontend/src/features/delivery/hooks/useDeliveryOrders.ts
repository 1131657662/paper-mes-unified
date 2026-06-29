import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryQuery } from '../../../types/delivery'

export function useDeliveryOrders(query: DeliveryQuery) {
  return useQuery(queries.delivery.list(query))
}
