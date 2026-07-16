import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { DeliveryQuery } from '../../../types/delivery'
import { deliveryService } from '../services/deliveryService'

export const deliveryKeys = createQueryKeys('delivery', {
  availableFinishes: (customerUuid: string) => ({
    queryKey: [customerUuid],
    queryFn: () => deliveryService.availableFinishes(customerUuid),
  }),
  detail: (uuid: string) => ({
    queryKey: [uuid],
    queryFn: () => deliveryService.detail(uuid),
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
