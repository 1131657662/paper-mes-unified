import { useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryInventoryFilter } from '../../../types/deliveryInventory'

export function useDeliveryInventorySummary(filter: DeliveryInventoryFilter) {
  return useQuery({ ...queries.delivery.inventorySummary(filter), staleTime: 15_000 })
}
