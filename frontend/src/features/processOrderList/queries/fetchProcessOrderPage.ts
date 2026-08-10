import type { QueryClient } from '@tanstack/react-query'
import type { PageResult } from '../../../types/common'
import type { ProcessOrder, ProcessOrderQuery } from '../../../types/processOrder'
import { processOrderListKeys } from './processOrderListKeys'

export function fetchProcessOrderPage(
  queryClient: QueryClient,
  query: ProcessOrderQuery,
): Promise<PageResult<ProcessOrder>> {
  return queryClient.fetchQuery({
    ...processOrderListKeys.page(query),
    staleTime: 0,
  })
}
