import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { queries } from '../../../queries'
import type { DeliveryInventoryCustomerQuery } from '../../../types/deliveryInventory'

export function useDeliveryInventoryCustomers(query: DeliveryInventoryCustomerQuery, enabled = true) {
  return useQuery({ ...queries.delivery.inventoryCustomers(query), enabled, placeholderData: keepPreviousData, staleTime: 15_000 })
}
