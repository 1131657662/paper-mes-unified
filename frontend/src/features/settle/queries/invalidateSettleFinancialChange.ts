import type { QueryClient } from '@tanstack/react-query'
import { queries } from '../../../queries'

export function invalidateSettleFinancialChange(
  queryClient: QueryClient,
  settleUuid: string,
): Promise<void> {
  const queryKeys = [
    queries.dashboard._def,
    queries.report._def,
      queries.settle.detail(settleUuid).queryKey,
      queries.settle.detailHeader(settleUuid).queryKey,
      queries.settle.details(settleUuid).queryKey,
      queries.settle.receives(settleUuid).queryKey,
      queries.settle.printLines(settleUuid).queryKey,
    queries.settle.list._def,
    queries.settle.summary._def,
    queries.settle.collectionSummary._def,
  ]
  return Promise.all(queryKeys.map((queryKey) => queryClient.invalidateQueries({ queryKey })))
    .then(() => undefined)
}
