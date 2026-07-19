import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryInventoryUnassignedQuery } from '../../../types/deliveryInventory'

export function useDeliveryInventoryUnassigned(query: DeliveryInventoryUnassignedQuery, enabled = true) {
  return useQuery({
    ...queries.delivery.inventoryUnassigned(query),
    enabled,
    placeholderData: keepPreviousData,
    staleTime: 15_000,
  })
}
