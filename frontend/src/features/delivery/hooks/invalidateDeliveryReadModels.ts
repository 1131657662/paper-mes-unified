import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

const deliveryWriteDependentKeys = () => [
  queries.delivery._def,
  queries.deliveryCustomerSpec._def,
  queries.processOrderDetail._def,
  queries.settle.candidates._def,
  queries.settle.quoteByOrders._def,
  queries.settle.quoteByMonth._def,
]

export function invalidateDeliveryReadModels(queryClient: QueryClient): Promise<void> {
  return Promise.all(deliveryWriteDependentKeys().map((queryKey) => (
    queryClient.invalidateQueries({ queryKey })
  ))).then(() => undefined)
}
