import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryInventoryFinishQuery } from '../../../types/deliveryInventory'

export function useDeliveryInventoryOrderGroups(query: DeliveryInventoryFinishQuery, enabled = true) {
  return useQuery({ ...queries.delivery.inventoryOrderGroups(query), enabled, placeholderData: keepPreviousData, staleTime: 15_000 })
}
