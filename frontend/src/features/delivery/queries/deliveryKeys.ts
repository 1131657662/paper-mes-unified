import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { AvailableFinishQuery, DeliveryQuery } from '../../../types/delivery'
import type {
  DeliveryInventoryCustomerQuery,
  DeliveryInventoryFilter,
  DeliveryInventoryFinishQuery,
  DeliveryInventoryUnassignedQuery,
} from '../../../types/deliveryInventory'
import { deliveryInventoryService } from '../services/deliveryInventoryService'
import { deliveryService } from '../services/deliveryService'

export const deliveryKeys = createQueryKeys('delivery', {
  availableFinishPage: (query: AvailableFinishQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryService.availableFinishPage(query),
  }),
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => deliveryService.detail(uuid),
  }),
  inventoryCustomers: (query: DeliveryInventoryCustomerQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryInventoryService.customers(query),
  }),
  inventoryFinishes: (query: DeliveryInventoryFinishQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryInventoryService.finishes(query),
  }),
  inventoryOrderGroups: (query: DeliveryInventoryFinishQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryInventoryService.orderGroups(query),
  }),
  inventorySummary: (filter: DeliveryInventoryFilter) => ({
    queryKey: [filter],
    queryFn: () => deliveryInventoryService.summary(filter),
  }),
  inventoryUnassigned: (query: DeliveryInventoryUnassignedQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryInventoryService.unassigned(query),
  }),
  list: (query: DeliveryQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryService.list(query),
  }),
  summary: (query: DeliveryQuery) => ({
    queryKey: [query],
    queryFn: () => deliveryService.summary(query),
  }),
})
