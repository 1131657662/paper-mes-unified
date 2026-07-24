import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

const localReadModelKeys = (orderUuid: string) => [
  queries.processOrderDetail.detail(orderUuid).queryKey,
  queries.processOrderDetail.printView(orderUuid, 'ISSUED').queryKey,
  queries.processOrderDetail.printView(orderUuid, 'FINISHED').queryKey,
  queries.processOrderDetail.snapshotDiff(orderUuid).queryKey,
  queries.finishCustomerSpec.current(orderUuid).queryKey,
]

const businessDependentKeys = () => [
  queries.settle.candidates._def,
  queries.settle.quoteByOrders._def,
  queries.settle.quoteByMonth._def,
  queries.delivery.availableFinishPage._def,
  queries.delivery.inventoryCustomers._def,
  queries.delivery.inventoryFinishes._def,
  queries.delivery.inventoryOrderGroups._def,
  queries.delivery.inventorySummary._def,
  queries.delivery.inventoryUnassigned._def,
]

function invalidateKeys(queryClient: QueryClient, queryKeys: ReadonlyArray<readonly unknown[]>): Promise<void> {
  return Promise.all(queryKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })))
    .then(() => undefined)
}

export function invalidateProcessOrderLocalReadModels(
  queryClient: QueryClient,
  orderUuid: string,
): Promise<void> {
  return invalidateKeys(queryClient, localReadModelKeys(orderUuid))
}

export function invalidateProcessOrderBusinessDependents(
  queryClient: QueryClient,
): Promise<void> {
  return invalidateKeys(queryClient, businessDependentKeys())
}

export function invalidateProcessOrderReadModels(
  queryClient: QueryClient,
  orderUuid: string,
): Promise<void> {
  return Promise.all([
    invalidateProcessOrderLocalReadModels(queryClient, orderUuid),
    invalidateProcessOrderBusinessDependents(queryClient),
  ]).then(() => undefined)
}
